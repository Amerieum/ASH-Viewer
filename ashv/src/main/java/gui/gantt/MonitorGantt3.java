package gui.gantt;

import com.egantt.model.drawing.ContextResources;
import com.egantt.model.drawing.DrawingState;
import com.egantt.swing.component.ComponentResources;
import com.egantt.swing.component.context.BasicComponentContext;
import com.egantt.swing.component.tooltip.ToolTipState;
import com.egantt.swing.table.list.BasicJTableList;
import core.manager.ColorManager;
import core.manager.ConstantManager;
import core.processing.GetFromRemoteAndStore;
import ext.egantt.drawing.painter.context.BasicPainterContext;
import ext.egantt.swing.GanttTable;
import gui.BasicFrame;
import gui.detail.SessionDetail;
import gui.detail.SqlDetail;
import gui.util.ProgressBarUtil;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingx.JXTable;
import org.jfree.chart.util.GanttParam;
import org.jfree.chart.util.IDetailPanel;
import store.StoreManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MonitorGantt3 extends JPanel implements IDetailPanel {
    private BasicFrame jFrame;
    private StoreManager storeManager;
    private GetFromRemoteAndStore getFromRemoteAndStore;
    private ColorManager colorManager;

    private JPanel main;

    private JSplitPane splitPaneMain;
    private JSplitPane splitPaneSqlSession;
    private JPanel jPanelqlSessionDetail;

    private GanttDataById3 ganttDataByIdSqlId;
    private GanttDataById3 ganttDataByIdSessionId;

    private GanttParam ganttParamOut;

    private String[][] columnNamesSqls = {{"Activity %", "SQL ID", "SQL type",  "Sql Text"}};
    private String[][] columnNamesSessions = {{"Activity %", "Session ID", "Session Serial#", "Username", "Program"}};

    @Getter @Setter private GanttParam ganttParam;
    @Getter @Setter private boolean isHistory;

    public MonitorGantt3(GanttParam ganttParamOut, BasicFrame jFrame, StoreManager storeManager,
                         GetFromRemoteAndStore getFromRemoteAndStore, ColorManager colorManager) {
        this.ganttParamOut = ganttParamOut;

        this.jFrame = jFrame;
        this.storeManager = storeManager;
        this.getFromRemoteAndStore = getFromRemoteAndStore;
        this.colorManager = colorManager;

        this.init();
    }

    private void init() {
        this.setLayout(new GridLayout(1, 1, 3, 3));
        this.main = new JPanel();
        this.main.setLayout(new GridLayout(1, 1, 3, 3));

        this.splitPaneMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.splitPaneSqlSession = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.jPanelqlSessionDetail = new JPanel();

        this.add(this.main);
    }

    @Override
    public void LoadDataToDetail(GanttParam ganttParamIn) {
        ProgressBarUtil.loadProgressBar(this, this.main, "Loading, please wait...");
        Thread t = new Thread() {
            @Override
            public void run() {
                setTimeRange();
                init0();
                loadDataToJPanelsPrivate0(ganttParamIn);
            }
        };
        t.start();
    }

    private void init0() {
        this.ganttDataByIdSqlId = new GanttDataById3(storeManager, (byte) 0); // Sql Id
        this.ganttDataByIdSessionId = new GanttDataById3(storeManager, (byte) 1); // Session Id
    }

    private void clearListOfGanttDataById() {
        this.ganttDataByIdSqlId.clear();
        this.ganttDataByIdSessionId.clear();
    }

    private void loadDataToJPanelsPrivate0(GanttParam ganttParamIn) {
        GanttParam ganttParamTo = new GanttParam.Builder(
                ganttParamIn.getBeginTime(), ganttParamIn.getEndTime())
                .sqlId(ganttParamOut.getSqlId())
                .sessionId(ganttParamOut.getSessionId())
                .serial(ganttParamOut.getSerial())
                .currentFileR(ganttParamOut.getCurrentFileR())
                .tsName(ganttParamOut.getTsName())
                .waitClass(ganttParamOut.getWaitClass())
                .dbaFilesIdList(ganttParamOut.getDbaFilesIdList())
                .build();

        this.clearListOfGanttDataById();

        this.ganttDataByIdSqlId.computeGanttDataFromBDB(ganttParamTo); // Sql Id data
        this.ganttDataByIdSessionId.computeGanttDataFromBDB(ganttParamTo); // Session Id

        this.addGanttTablesToJPanel();
        this.clearListOfGanttDataById();
    }

    private GanttTable getGanttDataToViewSqlId() {
        final GanttTable tableGanttSql;
        tableGanttSql = new GanttTable(
                ganttDataByIdSqlId.getDataToGantt(4, getFromRemoteAndStore),
                columnNamesSqls,
                getBasicJTableList(),
                ganttDataByIdSqlId.getListClassAndEvents());
        tableGanttSql.setRowHeightForJtable(23);
        tableGanttSql.getJXTable().setColumnControlVisible(true);

        tableGanttSql.getJXTable().setCellSelectionEnabled(true);

        setDoubleClickForColSqlId(tableGanttSql.getJXTable(), 1); // Open SQL id detail for double click event

        setTooltipAndPercent(tableGanttSql);

        return tableGanttSql;
    }

    private GanttTable getGanttDataToViewSession() {
        final GanttTable tableGanttSessions;
        tableGanttSessions = new GanttTable(
                ganttDataByIdSessionId.getDataToGantt(5, getFromRemoteAndStore),
                columnNamesSessions,
                getBasicJTableList(),
                ganttDataByIdSessionId.getListClassAndEvents());

        tableGanttSessions.setRowHeightForJtable(23);
        tableGanttSessions.getJXTable().setColumnControlVisible(true);
        tableGanttSessions.getJXTable().getColumnExt(2).setVisible(false); //Set invisible for session Serial

        tableGanttSessions.getJXTable().setCellSelectionEnabled(true);

        setDoubleClickForColSessionId(tableGanttSessions.getJXTable(), 1, 2);

        setTooltipAndPercent(tableGanttSessions);

        return tableGanttSessions;
    }


    private void addGanttTablesToJPanel() {
        // Sql
        JScrollPane leftPaneSql = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        leftPaneSql.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        leftPaneSql.setViewportView(this.getGanttDataToViewSqlId().getJXTable());
        leftPaneSql.setVerticalScrollBar(leftPaneSql.getVerticalScrollBar());

        // Session
        JScrollPane rightPaneSession = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightPaneSession.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        rightPaneSession.setViewportView(this.getGanttDataToViewSession().getJXTable());
        rightPaneSession.setVerticalScrollBar(rightPaneSession.getVerticalScrollBar());

        splitPaneSqlSession.setLeftComponent(leftPaneSql);
        splitPaneSqlSession.setRightComponent(rightPaneSession);
        splitPaneSqlSession.setDividerLocation(this.getWidth() / 2);
        splitPaneSqlSession.setOneTouchExpandable(true);

        splitPaneSqlSession.requestFocus();

        this.main.removeAll();
        this.main.add(this.splitPaneSqlSession);
        this.repaint();
        this.validate();
    }

    private void setTooltipAndPercent(GanttTable gantttable) {
        final String textPainter = ConstantManager.TEXT_PAINTER;
        BasicPainterContext graphics = new BasicPainterContext();
        graphics.setPaint(Color.BLACK);
        graphics.put(textPainter, new Font(null, Font.BOLD, 10));
        gantttable.getDrawingContext().put(textPainter,
                ContextResources.GRAPHICS_CONTEXT, graphics);
    }

    private BasicJTableList getBasicJTableList() {
        BasicJTableList tableListSqls = new BasicJTableList();
        {
            BasicComponentContext componentContext = new BasicComponentContext();
            ToolTipState state = (event, cellState) -> {
                DrawingState drawing = cellState.getDrawing();
                Object key = drawing != null ? drawing.getValueAt(event.getPoint()) : null;
                if (key == null)
                    return "";
                return key.toString();
            };
            componentContext.put(ComponentResources.TOOLTIP_STATE, state);
            tableListSqls.setRendererComponentContext(componentContext);
        }
        return tableListSqls;
    }

    public void setDoubleClickForColSqlId(JXTable table, int columnIndex){
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    final Object valueAt = table.getModel().getValueAt(table.getSelectedRow(), columnIndex);
                    new SqlDetail(jFrame, new GanttParam.Builder(ganttParam.getBeginTime(), ganttParam.getEndTime()).build(),
                            (String) valueAt, storeManager, getFromRemoteAndStore, colorManager);
                }
            }
        });
    }

    public void setDoubleClickForColSessionId(JXTable table, int cIndexSess, int cIndexSerial){
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    final Object valueAtSessId = table.getModel().getValueAt(table.getSelectedRow(), cIndexSess);
                    final Object valueAtSerial = table.getModel().getValueAt(table.getSelectedRow(), cIndexSerial);

                    new SessionDetail(jFrame,
                            new GanttParam.Builder(ganttParam.getBeginTime(), ganttParam.getEndTime())
                                    .sessionId((String) valueAtSessId).serial((String) valueAtSerial).build(),
                            storeManager, getFromRemoteAndStore, colorManager);
                }
            }
        });
    }

    private void setTimeRange(){
        if (!isHistory){
            double start = getFromRemoteAndStore.getCurrServerTime() - ConstantManager.CURRENT_WINDOW;
            double end = getFromRemoteAndStore.getCurrServerTime();

            ganttParam = new GanttParam.Builder(start, end).build();
        }
    }

}
