/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.gen2.table.client.ColumnDefinition;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SortableGrid;
import com.google.gwt.gen2.table.client.TableModel;
import com.google.gwt.gen2.table.client.TableModelHelper;
import com.google.gwt.gen2.table.event.client.PageChangeEvent;
import com.google.gwt.gen2.table.event.client.PageChangeHandler;
import com.google.gwt.gen2.table.event.client.PageCountChangeEvent;
import com.google.gwt.gen2.table.event.client.PageCountChangeHandler;
import com.google.gwt.gen2.table.event.client.PageLoadEvent;
import com.google.gwt.gen2.table.event.client.PageLoadHandler;
import com.google.gwt.gen2.table.event.client.PagingFailureEvent;
import com.google.gwt.gen2.table.event.client.PagingFailureHandler;
import com.google.gwt.gen2.table.event.client.RowSelectionEvent;
import com.google.gwt.gen2.table.event.client.RowSelectionHandler;
import com.google.gwt.gen2.table.event.client.TableEvent;
import com.google.gwt.gen2.table.override.client.HTMLTable;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.BadgeButton;
import edu.caltech.ipac.firefly.ui.Component;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopoutToolbar;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.ui.VisibleListener;
import edu.caltech.ipac.firefly.ui.creator.XYPlotViewCreator;
import edu.caltech.ipac.firefly.ui.panels.BackButton;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


/**
 * Date: Feb 10, 2009
 *
 * @author lo
 * @version $Id: TablePanel.java,v 1.149 2012/12/14 22:15:15 loi Exp $
 */
public class TablePanel extends Component implements StatefulWidget, FilterToggle.FilterToggleSupport, RequiresResize, ProvidesResize {

    private static final String HIGHLIGHTED_ROW_IDX = "TP_HLIdx";
    private static int maxRowLimit = Application.getInstance().getProperties().getIntProperty(
                                     "SelectableTablePanel.max.row.Limit", 100000);
//    private static int maxRowLimit = Constants.MAX_ROWS_SUPPORTED;
    private static final String TOO_LARGE_MSG = "Sorting is disabled on table with more than " +
            NumberFormat.getFormat("#,##0").format(maxRowLimit) + " rows.";
    private static final HTML FEATURE_ONLY_TABLE = new HTML("<i><font color='red'>This feature is only available in Table View</font></i>");
    private static final HTML TOO_LARGE = new HTML("<i><font color='red'>" + TOO_LARGE_MSG + "</font></i>");
    private static final HTML NOT_LOADED = new HTML("<i><font color='red'>This function is not available <br> " +
            "until the table is fully loaded.</font></i>");

    public static final Name ON_DATA_LOAD = new Name("onDataLoad",
            "After new data is loaded.");
    public static final Name ON_PAGE_LOAD = new Name("onPageLoad",
            "After a page is loaded.");
    public static final Name ON_PAGE_CHANGE = new Name("onPageChange",
            "Page change; before a new page is loaded.");
    public static final Name ON_PAGE_ERROR = new Name("onPageError",
            "Page load error.");
    public static final Name ON_PAGECOUNT_CHANGE = new Name("onPageCountChange",
            "The number of pages changed.");
    public static final Name ON_ROWSELECT_CHANGE = new Name("onRowSelectChange",
            "After a row is selected.  Checkbox checked.");
    public static final Name ON_ROWHIGHLIGHT_CHANGE = new Name("onRowHighlightChange",
            "After a row is highlighted with a click.  Row changes color.");
    public static final Name ON_VIEW_CHANGE = new Name("onViewChange",
            "After a view switch");
    public static final Name ON_STATUS_UPDATE = new Name("onStatusUpdate",
            "Called when table's status is updated");
    private static final int TOOLBAR_SIZE = 30;

    private List<View> views = new ArrayList<View>();
    private List<View> activeViews = new ArrayList<View>();

    private int maskDelayMillSec = 200;
    private boolean onlyMaskWhenUncovered = true;

    private String stateId = "TPL";
    private String name;
    private String shortDesc;
    private HTML titleHolder;
    private DeckPanel viewDeck = new DeckPanel();
    private DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.PX);
    private BasicPagingTable table;
    private PagingToolbar pagingBar;
    private HorizontalPanel centerToolbar;
    private HorizontalPanel rightToolbar;
    private HorizontalPanel leftToolbar;
    private HorizontalPanel toolbarWrapper;

    //    private Loader<TableDataView> loader;
    private DataSetTableModel dataModel;
    //    private TableDataView dataset;
    private boolean headerWidthSet = false;
    private boolean tableTooLarge = false;
    private boolean tableNotLoaded = true;
    private PopupPanel notAllowWarning;
    private int cMouseX;
    private int cMouseY;
    //    private CheckBox filters;
    private FilterToggle filters;
    private TableOptions options;
    private BadgeButton optionsButton;
    private SimplePanel mainWrapper;
    private PopoutToolbar popoutButton;
    private boolean expanded = false;
    private Image textView = new Image(TableImages.Creator.getInstance().getTextViewImage());
    private Image tableView = new Image(TableImages.Creator.getInstance().getTableViewImage());
    private BadgeButton viewSelector = new BadgeButton(textView);
    private BadgeButton saveButton;
    private SimplePanel helpButton = new SimplePanel();
    private boolean handleEvent = true;
    private DSModelHandler modelEventHandler = new DSModelHandler();
    private XYPlotViewCreator.XYPlotView xyPlotView = null;


    private DownloadRequest downloadRequest = null;

    public TablePanel(Loader<TableDataView> loader) {
        this("untitled", loader);
    }

    public TablePanel(String name, Loader<TableDataView> loader) {
        setInit(false);
        this.name = name;
        dataModel = new DataSetTableModel(loader);
        dataModel.addHandler(modelEventHandler);

        mainWrapper = new SimplePanel();
        mainWrapper.addStyleName("mainWrapper");
        mainWrapper.setSize("100%", "100%");
        mainPanel.setSize("100%", "100%");
        mainPanel.addStyleName("mainPanel");
        DOM.setStyleAttribute(mainPanel.getElement(), "borderSpacing", "0px");
        toolbarWrapper = new HorizontalPanel();
        toolbarWrapper.setStyleName("firefly-toolbar");
        stateId = name;
        mainWrapper.add(mainPanel);
        initWidget(mainWrapper);
        sinkEvents(Event.ONMOUSEOVER);

        WebEventManager.getAppEvManager().addListener(Name.DOWNLOAD_REQUEST_READY, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (ev.getName().equals(Name.DOWNLOAD_REQUEST_READY)) {
                    if (ev.getSource() instanceof DownloadRequest) {
                        downloadRequest = (DownloadRequest) ev.getSource();
                    }
                }
            }
        });
        helpButton.setVisible(false);
    }

    public DataSetTableModel getDataModel() {
        return dataModel;
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        if (event.getTypeInt() == Event.ONMOUSEOVER) {
            cMouseX = event.getClientX();
            cMouseY = event.getClientY();
        }
    }

    public void setMaskDelayMillSec(int maskDelayMillSec) {
        this.maskDelayMillSec = maskDelayMillSec;
    }

    public void setHelpId(String id) {
        if (!StringUtils.isEmpty(id)) {
            Widget helpIcon = HelpManager.makeHelpIcon(id);
            helpIcon.setSize("24px", "24px");
            helpButton.setWidget(helpIcon);
            helpButton.setVisible(true);
        } else {
            helpButton.setVisible(false);
        }
    }


    public void showOptionsButton(boolean show) {
        if (optionsButton != null) {
            optionsButton.getWidget().setVisible(show);
        }
    }

    public void showOptions(boolean show) {
        if (show) {
            GwtUtil.DockLayout.showWidget(mainPanel, options);
        } else {
            GwtUtil.DockLayout.hideWidget(mainPanel, options);
        }
    }

    public void showFiltersButton(boolean show) {
        if (filters != null) {
            filters.setVisible(show);
        }
    }

    public void showSaveButton(boolean show) {
        if (saveButton != null) {
            saveButton.getWidget().setVisible(show);
        }
    }

    public void showTableView(boolean show) {
        int idx = getViewIdx(TableView.NAME);
        if (idx >= 0) {
            getViews().get(idx).setHidden(!show);
        }
    }

    public void showPopOutButton(boolean show) {
        if (popoutButton != null) {
            popoutButton.setVisible(show);
        }
    }

    public void showColumnHeader(boolean show) {
        if (getTable() != null && getTable().getHeaderTable() != null) {
            getTable().getHeaderTable().setVisible(show);
        }
    }

    public void showTitle(boolean show) {
        if (titleHolder == null) {
            titleHolder = new HTML("<b>" + name + "</b>");
        }
        titleHolder.setVisible(show);
    }

    public void addView(View view) {
        views.add(view);
        activeViews.add(view);
        view.bind(this);
    }

    public void init() {
        init(null);
    }

    public void switchView(Name name) {
        if (name != null) {
            if (name.equals(TableView.NAME)) {
                viewSelector.setIcon(textView);
                viewSelector.getWidget().setVisible(true);
            } else if (name.equals(TextView.NAME)) {
                viewSelector.setIcon(tableView);
                viewSelector.getWidget().setVisible(true);
            } else {
                viewSelector.getWidget().setVisible(false);
            }
        }

        int vidx = getViewIdx(name);
        if (viewDeck.getVisibleWidget() != vidx) {
            viewDeck.showWidget(vidx);
            for (View v : views) {
                v.onViewChange(views.get(vidx));
            }
            if (!isExpanded()) {
                getEventManager().fireEvent(new WebEvent(this, ON_VIEW_CHANGE, views.get(vidx).getName().getName()));
            }
        }
    }

    private int getViewIdx(Name name) {
        if (!StringUtils.isEmpty(name)) {
            for (int i = 0; i < views.size(); i++) {
                View v = views.get(i);
                if (v.getName().equals(name)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private String getServerError(final Throwable caught) {
        String eMsg = caught != null ? caught.getMessage() : "unknown";

        String msgExtra = "<span class=\"faded-text\">" +
                "<br><br>If you still continue to receive this message, contact IRSA for <a href='http://irsa.ipac.caltech.edu/applications/Helpdesk' target='_blank'>help</a>.  " +
                "<span>";
        String msg = "<b> Unable to load table.</b><br>";

        if (caught instanceof StatusCodeException) {
            StatusCodeException scx = (StatusCodeException) caught;
            if (scx.getStatusCode() == 503) {
                msg = "The site is down for scheduled maintenance.";
                msgExtra = "";
            } else if (scx.getStatusCode() == 0) {
                msg = "If you are not connected to the internet, check your internet connection and try again";
            } else {
                msg = "The server encountered an unexpected condition which prevented it from fulfilling the request.<br>" +
                        "Refreshing the page may resolve the problem.";
            }
        } else if (caught instanceof RPCException) {
            RPCException ex = (RPCException) caught;
            if (ex.getEndUserMsg() != null) {
                eMsg = ex.getEndUserMsg();
            }
        }
        return msg + eMsg + msgExtra;
    }

    public void init(final AsyncCallback<Integer> callback) {

        AsyncCallback<TableDataView> cb = new AsyncCallback<TableDataView>() {
            public void onFailure(Throwable caught) {
                // not sure what to do with this.
                // need to set init to true so other code can continue..
                // but, has no way of passing the error.
                try {
                    handleEvent = false;
                    if (callback != null) {
                        callback.onSuccess(0);
                    }
                    PopupUtil.showError("Request Fail", getServerError(caught));
                } finally {
                    TablePanel.this.setInit(true);
                }
            }

            public void onSuccess(TableDataView result) {
                try {
                    table = makeTable(dataModel);
                    layout();
                    addListeners();
                    if (GwtUtil.isOnDisplay(TablePanel.this)) {
                        onShow();
                    }
                    table.gotoFirstPage();
                    TablePanel.this.setInit(true);
                } finally {
                    if (callback != null) {
                        callback.onSuccess(dataModel.getTotalRows());
                    }
                }
            }
        };
        // load up the first page of data.. upon success, creates and initializes the tablepanel.
        dataModel.getData(cb, 0);
    }

    public boolean isTableLoaded() {
        return !tableNotLoaded;
    }

    public int getRowCount() {
        if (table == null || table.getRowValues() == null) {
            GWT.log("TablePanel not ready", null);
            return -1;
        } else {
            return table.getRowValues().size();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public BasicPagingTable getTable() {
        return table;
    }

    public TableDataView getDataset() {
        if (table == null || table.getDataModel() == null) return null;

        return table.getDataModel().getCurrentData();
    }

    public List<View> getViews() {
        return views;
    }

    public List<View> getVisibleViews() {
        List<View> options = new ArrayList<View>();
        for (View v : activeViews) {
            if (!v.isHidden()) {
                options.add(v);
            }
        }
        return options;
    }

    public void showToolBar(final boolean show) {

        if (toolbarWrapper.isAttached()) {
            toolbarWrapper.setVisible(show);
            leftToolbar.setVisible(show);
            rightToolbar.setVisible(show);
            centerToolbar.setVisible(show);
            if (show) {
                GwtUtil.DockLayout.showWidget(mainPanel, toolbarWrapper);
            } else {
                GwtUtil.DockLayout.hideWidget(mainPanel, toolbarWrapper);
            }
        } else {
            this.getEventManager().addListener(ON_SHOW, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    showToolBar(show);
                    TablePanel.this.getEventManager().removeListener(ON_SHOW, this);
                }
            });
        }
    }

    public PagingToolbar getPagingBar() {
        return pagingBar;
    }

    public void showPagingBar(final boolean show) {
        if (pagingBar != null && pagingBar.isAttached()) {
            if (show) {
                toolbarWrapper.setVisible(true);
                centerToolbar.setVisible(true);
                pagingBar.setVisible(true);
                GwtUtil.DockLayout.showWidget(mainPanel, toolbarWrapper);
            } else {
                pagingBar.setVisible(false);
            }
        } else {
            this.getEventManager().addListener(ON_SHOW, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    showPagingBar(show);
                    TablePanel.this.getEventManager().removeListener(ON_SHOW, this);
                }
            });
        }
    }

    public Widget addToolButton(final GeneralCommand cmd) {
        return addToolButton(cmd, true);
    }

    public void clearToolButtons() {
        clearToolButtons(true, true, true);
    }

    public void clearToolButtons(boolean left, boolean center, boolean right) {
        if (rightToolbar != null && right) {
            rightToolbar.clear();
        }
        if (centerToolbar != null && center) {
            centerToolbar.clear();
        }
        if (leftToolbar != null && left) {
            leftToolbar.clear();
        }
    }

    public void removePanels() {
        if (toolbarWrapper != null) {
            mainPanel.remove(toolbarWrapper);
        }
    }

    public void addToolWidget(Widget w, boolean alignRight) {
        if (alignRight && rightToolbar != null) {
            rightToolbar.add(w);
            rightToolbar.setCellVerticalAlignment(w, VerticalPanel.ALIGN_BOTTOM);
            GwtUtil.setStyle(w, "marginLeft", "5px");
        } else if (leftToolbar != null) {
            leftToolbar.add(w);
            leftToolbar.setCellVerticalAlignment(w, VerticalPanel.ALIGN_BOTTOM);
            GwtUtil.setStyle(w, "marginRight", "5px");
        }
    }

    public void addToolButton(FocusWidget btn, boolean alignRight) {
        btn.addStyleName("button");
        addToolWidget(btn, alignRight);
    }

    public Widget addToolButton(final GeneralCommand cmd, boolean alignRight) {
        final FocusWidget btn = new Button(cmd.getLabel());
        updateHighlighted(btn, cmd);
        btn.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) {
                cmd.execute();
            }
        });

        cmd.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pce) {
                if (pce.getPropertyName().equals(GeneralCommand.PROP_TITLE)) {
                    if (btn instanceof HasText) {
                        ((HasText) btn).setText(String.valueOf(pce.getNewValue()));
                    }
                } else if (pce.getPropertyName().equals(GeneralCommand.PROP_HIGHLIGHT)) {
                    updateHighlighted(btn, cmd);
                }
            }
        });
        if (!StringUtils.isEmpty(cmd.getShortDesc())) {
            btn.setTitle(cmd.getShortDesc());
        }
        addToolButton(btn, alignRight);
        return btn;
    }

    public boolean isActiveView(Name vname) {
        int idx = viewDeck.getVisibleWidget();
        return idx >= 0 && vname != null && views.get(idx).getName().equals(vname);
    }

    public Name getActiveView() {
        int idx = viewDeck.getVisibleWidget();
        return idx >= 0 && idx < views.size() ? views.get(idx).getName() : null;
    }

    public boolean hasView(Name view) {
        if (view == null) return false;
        for (View v : views) {
            if (v.getName().equals(view)) {
                return true;
            }
        }
        return false;
    }

    public DownloadRequest getDownloadRequest() {
        return downloadRequest;
    }

    public static void updateHighlighted(FocusWidget b, GeneralCommand cmd) {
        if (cmd.isHighlighted()) {
            b.removeStyleName("button");
            b.removeStyleName("normal-text");
            b.addStyleName("button-highlight");
            b.addStyleName("highlight-text");
        } else {
            b.removeStyleName("button-highlight");
            b.removeStyleName("highlight-text");
            b.addStyleName("button");
            b.addStyleName("normal-text");
        }
    }


    void updateHasAccessRows() {

        HTMLTable.RowFormatter formatter = table.getDataTable().getRowFormatter();
        List<TableData.Row> rows = table.getRowValues();
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (!rows.get(i).hasAccess()) {
                    formatter.addStyleName(i, "caution");
                }
            }
        }
    }

    void showTooLargeWarning() {
        showNotAllowWarning(TOO_LARGE);
    }

    void showNotLoadedWarning() {
        showNotAllowWarning(NOT_LOADED);
    }

    public void showNotAllowWarning(HTML msg) {
        showNotAllowWarning(msg, 4000);
    }

    /**
     *
     * @param msg
     * @param delay  how long should the message stay up in msec.
     */
    public void showNotAllowWarning(HTML msg, int delay) {
        if (notAllowWarning == null) {
            notAllowWarning = new PopupPanel(true);
            notAllowWarning.setAnimationEnabled(true);
            notAllowWarning.addStyleName("onTopDialog");
        }
        notAllowWarning.setWidget(msg);
        notAllowWarning.setPopupPosition(cMouseX - 75, cMouseY - 25);
        notAllowWarning.show();
        new Timer() {
            public void run() {
                notAllowWarning.hide();
            }
        }.schedule(delay);

    }

    public void addDoubleClickListner(DoubleClickHandler dch) {
        table.addDoubleClickListener(dch);
    }

//====================================================================
// override methods
//====================================================================

    @Override
    public void onShow() {
        if (!handleEvent) return;

        setAppStatus(true);

        Name vn = getActiveView();
        if (vn != null) {
            View v = views.get(getViewIdx(vn));
            if (v != null && v instanceof VisibleListener) {
                ((VisibleListener) v).onShow();
            }
        }
        super.onShow();
    }

    @Override
    public void onHide() {
        if (!handleEvent) return;
        setAppStatus(false);

        Name vn = getActiveView();
        if (vn != null) {
            View v = views.get(getViewIdx(vn));
            if (v != null && v instanceof VisibleListener) {
                ((VisibleListener) v).onHide();
            }
        }
        super.onHide();
    }

    private void setAppStatus(boolean onshow) {
        if (onshow && getDataModel().isMaxRowsExceeded()) {
            Application.getInstance().setStatus("Dataset too large: some functions are disabled. Filter the data down to " + Constants.MAX_ROWS_SUPPORTED + " rows.");
            if (xyPlotView != null && activeViews.contains(xyPlotView)) {
                activeViews.remove(xyPlotView);
                if (isActiveView(xyPlotView.getName())) {
                    switchView(TableView.NAME);
                }
                Application.getInstance().getLayoutManager().getLayoutSelector().layout();
            }
        } else {
            Application.getInstance().setStatus("");
            if (xyPlotView != null && !activeViews.contains(xyPlotView)) {
                activeViews.add(xyPlotView);
                Application.getInstance().getLayoutManager().getLayoutSelector().layout();
            }
        }
    }

    @Override
    public void onInit() {
        super.onInit();
        applySortIndicator();
    }

    private void applySortIndicator() {
        SortInfo si = dataModel.getSortInfo();
        if (si != null) {
            TableDataView.Column c = getDataset().findColumn(si.getPrimarySortColumn());
            if (c != null) {
                getTable().setSortIndicator(c.getTitle(), si.getDirection());
            }

        }
    }

//====================================================================
//  private/protected methods
//====================================================================

    protected void layout() {

        if (table == null) return;

//        final FlexTable.FlexCellFormatter formatter = mainPanel.getFlexCellFormatter();
        // Initialize the tables
        // Create the tables
        table.addStyleName("expand-fully");
        table.setFilterChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                doFilters();
            }
        });

        // Override the column sorter
        table.getDataTable().setColumnSorter(new CustomColumnSorter());

        FocusPanel fp = new FocusPanel(table);
        fp.addKeyDownHandler(new HighlightedKeyMove(table.getDataTable()));
        DOM.setStyleAttribute(fp.getElement(), "outline", "yellow dotted thin");
        fp.addStyleName("expand-fully");

        addView(new TableView());
        addView(new TextView());
        TableServerRequest r = dataModel.getRequest();

        if (XYPlotWidget.ENABLE_XY_CHARTS && isCatalogData()) {
            xyPlotView = new XYPlotViewCreator.XYPlotView(new HashMap<String, String>());
            addView(xyPlotView);
        }

        viewDeck.setAnimationEnabled(true);
        viewDeck.addStyleName("expand-fully");

        // sort the views based on its index
        Collections.sort(views, new Comparator<View>() {
            public int compare(View v1, View v2) {
                return v1.getViewIdx() == v2.getViewIdx() ? 0 : v1.getViewIdx() < v2.getViewIdx() ? -1 : 1;
            }
        });

        for (View view : views) {
            viewDeck.add(view.getDisplay());
        }

        options = new TableOptions(this);

        // Create top rightToolbar
        centerToolbar = new HorizontalPanel();
        // Create top rightToolbar
        rightToolbar = new HorizontalPanel();
        // Create top leftToolbar
        leftToolbar = new HorizontalPanel();

        Widget cbar = GwtUtil.centerAlign(centerToolbar);
        Widget rbar = GwtUtil.rightAlign(rightToolbar);

        toolbarWrapper.add(leftToolbar);
        toolbarWrapper.add(cbar);
        toolbarWrapper.add(rbar);
        toolbarWrapper.setCellVerticalAlignment(leftToolbar, VerticalPanel.ALIGN_MIDDLE);
        toolbarWrapper.setCellVerticalAlignment(cbar, VerticalPanel.ALIGN_MIDDLE);
        toolbarWrapper.setCellVerticalAlignment(rbar, VerticalPanel.ALIGN_MIDDLE);


        mainPanel.addNorth(toolbarWrapper, TOOLBAR_SIZE);
        mainPanel.addEast(options, 200);

        // Create the paging bar
        pagingBar = new PagingToolbar(TablePanel.this);
//        mainPanel.addSouth(pagingBar, 32);
        centerToolbar.add(pagingBar);

        // Add the scroll table to the mainPanel
        mainPanel.add(viewDeck);
//        mainPanel.setCellWidth(tableHolder, "100%");
//        mainPanel.setCellHeight(tableHolder, "100%");

        addToolBar();

        if (table != null && table.getDataModel() != null && table.getDataModel().getTotalRows() == 0) {
            showToolBar(false);
        }

        switchView(views.get(0).getName());
        showOptions(false);
        if (FFToolEnv.isAPIMode()) {
            showPopOutButton(false);
        }
    }

    private boolean isCatalogData() {
        DataSet ds = dataModel== null ? null : dataModel.getCurrentData();
        TableMeta meta = ds == null ? null : ds.getMeta();
        return meta != null && meta.contains(MetaConst.CATALOG_OVERLAY_TYPE);
    }

    void updateTableStatus() {
        if (table == null || pagingBar == null) return;

        tableTooLarge = table.getTableModel().getRowCount() > maxRowLimit;
        tableNotLoaded = !getDataset().getMeta().isLoaded();

        pagingBar.setIsLoading(tableNotLoaded);
        pagingBar.updateStatusMsg();
        if (!expanded) {
            getEventManager().fireEvent(new WebEvent<Boolean>(this, ON_STATUS_UPDATE, isTableLoaded()));
        }

        if (GwtUtil.isOnDisplay(this)) {
            setAppStatus(true);
        }
    }

    protected void addListeners() {

        WebEventListener handler = new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (ev.getName().equals(ON_PAGE_LOAD)) {
                    if (isActiveView(TableView.NAME)) {
                        if (GwtUtil.isOnDisplay(table) && getTable().getDataTable().getRowCount() > 0) {
                            if (getTable().getDataTable().isSelectionEnabled()) {
                                getTable().getDataTable().selectRow(0, true);
                                filters.reinit();
                            }
                        }
                    }
                } else if (ev.getName().equals(ON_SHOW)) {
                    if (isActiveView(TableView.NAME)) {
                        table.onShow();
                        filters.reinit();
                        if (table.getRowCount() > 0 &&
                                table.getDataTable().getSelectedRows().size() == 0) {
                            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                                public void execute() {
                                    table.getDataTable().selectRow(0, true);
                                    table.scrollHighlightedIntoView();
                                }
                            });
                        } else {
                            table.scrollHighlightedIntoView();
                        }
                    }
                }
            }
        };

        getEventManager().addListener(ON_PAGE_LOAD, handler);
        getEventManager().addListener(ON_SHOW, handler);
        getEventManager().addListener(ON_HIDE, handler);
        bindDataViewToTable(getDataset());

        // listen to table's events
        table.addPageChangeHandler(new PageChangeHandler() {
            public void onPageChange(PageChangeEvent event) {
                mask("Loading...", maskDelayMillSec);
                if (!expanded) {
                    getEventManager().fireEvent(new WebEvent(TablePanel.this, ON_PAGE_CHANGE));
                }
            }
        });
        table.addPageCountChangeHandler(new PageCountChangeHandler() {
            public void onPageCountChange(PageCountChangeEvent event) {
                updateTableStatus();
                if (!expanded) {
                    getEventManager().fireEvent(new WebEvent(TablePanel.this, ON_PAGECOUNT_CHANGE));
                }
            }
        });
        table.addPageLoadHandler(new PageLoadHandler() {
            public void onPageLoad(PageLoadEvent event) {
                unmask();
                updateHasAccessRows();
                if (!expanded && handleEvent) {
                    getEventManager().fireEvent(new WebEvent(TablePanel.this, ON_PAGE_LOAD));
                }
            }
        });
        table.addPagingFailureHandler(new PagingFailureHandler() {
            public void onPagingFailure(PagingFailureEvent event) {
                unmask();
                if (!expanded) {
                    getEventManager().fireEvent(new WebEvent(TablePanel.this, ON_PAGE_ERROR));
                }
            }
        });

        table.getDataTable().addRowSelectionHandler(new RowSelectionHandler() {
            public void onRowSelection(RowSelectionEvent event) {
                if (!expanded && (GwtUtil.isOnDisplay(TablePanel.this) && handleEvent)) {
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        public void execute() {
                            getEventManager().fireEvent(new WebEvent(TablePanel.this, ON_ROWHIGHLIGHT_CHANGE));
                        }
                    });
                }
            }

        });
    }

    protected void bindDataViewToTable(TableDataView dataset) {
        new TableDataViewToTableAdapter(this, dataset);
    }

    protected BasicPagingTable newTable(DataSetTableModel model, TableDataView dataset) {
        return new BasicPagingTable(name, model, new BasicPagingTable.DataTable(), new DatasetTableDef(dataset));
    }

    protected BasicPagingTable makeTable(DataSetTableModel cachedModel) {

        // Create the scroll table
        final BasicPagingTable table = newTable(cachedModel, cachedModel.getCurrentData());

        table.setPageSize(dataModel.getPageSize());
        table.setEmptyTableWidget(new HTML(
                "There are no data to display"));

        // Setup the formatting
        table.setCellPadding(0);
        table.setCellSpacing(0);
        table.setResizePolicy(ScrollTable.ResizePolicy.UNCONSTRAINED);

        return table;
    }

    protected void addToolBar() {

        final Image saveImg = new Image(TableImages.Creator.getInstance().getSaveImage());
        saveButton = new BadgeButton(saveImg);
        saveButton.setTitle("Save the content as an IPAC table");
        saveButton.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        // for browsers not supporting pointer-event
                        if (!saveButton.isEnabled()) return;

                        if (tableNotLoaded) {
                            showNotLoadedWarning();
                        } else {
                            // determine visible columns
                            boolean hasCollapseCols = false;
                            List<String> cols = new ArrayList<String>();
                            for (int i = 0; i < getDataset().getColumns().size(); i++) {
                                if (getDataset().getColumn(i).isVisible()) {
                                    cols.add(getDataset().getColumn(i).getName());
                                } else {
                                    hasCollapseCols = true;
                                }
                            }

                            // if there are hidden columns, set request to only include visible columns
                            if (hasCollapseCols && cols.size() > 0) {
                                dataModel.getRequest().setParam(TableServerRequest.INCL_COLUMNS, StringUtils.toString(cols, ","));
                                if (dataModel.getTotalRows() > maxRowLimit) {
                                    showNotAllowWarning(new HTML("<i><font color='brown'>Due to the size of this table, it may take a few minutes to process your request." +
                                            "  <br>Please be patient.  Your file will start downloading after this process has completed.</font></i>"), 8000);
                                }
                            }

                            saveButton.setEnabled(false);
                            Image loadingImage = new Image(GwtUtil.LOADING_ICON_URL);
                            saveButton.setIcon(loadingImage);
                            // check every 5 seconds.. up to 120 times.  ==> 10 mins.  button will be re-enable after this.
                            GwtUtil.submitDownloadUrl(dataModel.getLoader().getSourceUrl(), 5000, 60, new Command() {
                                public void execute() {
                                    saveButton.setEnabled(true);
                                    saveButton.setIcon(saveImg);
                                }
                            });
                            dataModel.getRequest().removeParam(TableServerRequest.INCL_COLUMNS);
                        }
                    }
                });

        filters = new FilterToggle(this);

        BackButton close = new BackButton("Close");
        final AbsolutePanel popoutWrapper = new AbsolutePanel();
        popoutWrapper.setSize("100%", "100%");
        popoutWrapper.add(close, 0, 0);

        close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                Application.getInstance().getLayoutManager().getRegion(LayoutManager.POPOUT_REGION).hide();
                if (mainWrapper.getWidget() == null) {
                    mainWrapper.add(mainPanel);
                    mainPanel.setSize("100%", "100%");
                }
                expanded = false;
                if (BrowserUtil.isTouchInput()) popoutButton.showToolbar(true);
                onShow();
            }
        });

        ClickHandler popoutHandler = new ClickHandler() {
            public void onClick(ClickEvent event) {
                        popoutButton.hideToolbar();
                        popoutWrapper.add(mainPanel, 0, 32);
                        Application.getInstance().getLayoutManager().getRegion(LayoutManager.POPOUT_REGION).setDisplay(popoutWrapper);
                        expanded = true;
                        mainPanel.forceLayout();
                    }
                };

        popoutButton = new PopoutToolbar(popoutHandler, true);

        textView.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                switchView(TextView.NAME);
            }
        });
        tableView.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    switchView(TableView.NAME);
                }
            });

        textView.setTitle("Text View");
        tableView.setTitle("Table View");

        optionsButton = new BadgeButton(new Image(VisIconCreator.Creator.getInstance().getSettings()));
        optionsButton.setTitle("Edit Table Options");
        optionsButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (GwtUtil.DockLayout.isHidden(options)) {
                    options.syncOptions();
                    showOptions(true);
                } else {
                    showOptions(false);
                }
            }
        });

        addToolWidget(filters, true);
        addToolWidget(viewSelector.getWidget(), true);
        addToolWidget(saveButton.getWidget(), true);
        addToolWidget(optionsButton.getWidget(), true);
        addToolWidget(helpButton, true);
        addToolWidget(popoutButton, true);


        if (!BrowserUtil.isTouchInput()) {
            this.addDomHandler(new MouseOverHandler() {
                public void onMouseOver(MouseOverEvent event) {
                    if (!expanded) popoutButton.showToolbar(true);
                }
            }, MouseOverEvent.getType());

            this.addDomHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent event) {
                    if (!expanded) popoutButton.showToolbar(false);
                }
            }, MouseOutEvent.getType());
        }
    }


    public void highlightRow(boolean forceEventTrigger, int idx) {
        boolean oldVal = handleEvent;
        this.handleEvent = handleEvent || forceEventTrigger;
        table.highlightRow(idx);
        this.handleEvent = oldVal;
    }

    /**
     * This method clear out cache, then reload the table plus its data.
     *
     * @param page after loaded, goto the given page if possible.  if page is out of range, goto first page or last
     *             page.
     */
    public void reloadTable(int page) {
        reloadTable(page, dataModel.getPageSize(), -1);
    }

    /**
     * This method clear out cache, then reload the table plus its data.
     *
     * @param page     after loaded, goto the given page if possible.  if page is out of range, goto first page or last
     *                 page.
     * @param pageSize use the given pageSize
     * @param hlRowIdx highlight the given row index.  index is the index of the whole table, not relative to a page.
     */
    public void reloadTable(int page, int pageSize, final int hlRowIdx) {
        if (table.getDataModel() == null) return;

        table.getDataModel().clearCache();
        SortInfo sortInfo = dataModel.getSortInfo();
        if (sortInfo != null) {

            int cidx = 0;
            List<ColumnDefinition<TableData.Row, ?>> vcol = table.getTableDefinition().getVisibleColumnDefinitions();
            for (cidx = 0; cidx < vcol.size(); cidx++) {
                ColDef col = (ColDef) vcol.get(cidx);
                if (col.getName() != null && col.getName().equals(sortInfo.getPrimarySortColumn())) {
                    break;
                }
            }
            if (cidx >= 0) {
                TableModelHelper.ColumnSortList sl = new TableModelHelper.ColumnSortList();
                sl.add(new TableModelHelper.ColumnSortInfo(cidx, sortInfo.getDirection() == SortInfo.Direction.ASC));
                getTable().getDataTable().setColumnSortList(sl);
            }
        } else {
            table.getDataTable().getColumnSortList().clear();
            table.clearSortIndicator();
        }

        if (dataModel.getPageSize() != pageSize) {
            dataModel.setPageSize(pageSize);
            table.setPageSize(pageSize);
        }

        page = Math.min(page, getDataset().getTotalRows() / pageSize);
        table.getTableModel().setRowCount(TableModel.UNKNOWN_ROW_COUNT);    // this line is needed to force loader to load when previous results in zero rows.
        table.gotoPage(page, true);
        WebEventListener doHL = new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                int hlidx = hlRowIdx < 0 ? getTable().getAbsoluteFirstRowIndex() : hlRowIdx;
                getTable().highlightRow(hlidx);
                syncTableUI();
                TablePanel.this.getEventManager().removeListener(ON_PAGE_LOAD, this);
            }
        };
        getEventManager().addListener(ON_PAGE_LOAD, doHL);
    }

    /**
     * update the table's UI.  Will not reload the data.
     */
    public void redrawTable() {
        handleEvent = false;
        Set<Integer> selRows = getTable().getDataTable().getSelectedRows();
        int sRow = selRows == null || selRows.size() == 0 ? 0 : selRows.iterator().next();
        table.refresh();
        getTable().getDataTable().selectRow(sRow, true);
        syncTableUI();

        handleEvent = true;
    }

    private void syncTableUI() {
        applySortIndicator();
        table.setFilters(dataModel.getFilters());
        filters.reinit();
        if (isActiveView(TextView.NAME)) {
            TextView tview = (TextView) getViews().get(getViewIdx(TextView.NAME));
            tview.loadTextView();
        }
    }


    public boolean isExpanded() {
        return expanded;
    }

    public void doFilters() {
        List<String> filterList = table.getFilters(true);
        String newFilters = StringUtils.toString(filterList);
        String oldFilters = StringUtils.toString(dataModel.getFilters());

        if (!newFilters.equals(oldFilters)) {
            dataModel.setFilters(filterList);
            reloadTable(0);
            fireStaleEvent();
        }
    }

    private void fireStaleEvent() {
        handleEvent = false;
        dataModel.fireDataStaleEvent();
        handleEvent = true;
    }

    protected void onSorted() {
        if (getDataset().getMeta().isLoaded()) {
            fireStaleEvent();
        } else {
            modelEventHandler.fireStaleEventOnload();
        }
    }

//====================================================================
//  Implementing StatefulWidget
//====================================================================

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String id) {
        stateId = id;
    }

    public void recordCurrentState(Request req) {
        if (getTable() == null) return;

        int ps = getTable().getPageSize();
        int startIdx = getTable().getCurrentPage() * getTable().getPageSize();
        int hlIdx = getTable().getHighlightedRowIdx();

        if (ps > 0) {
            req.setParam(getStateId() + "_" + Request.PAGE_SIZE, String.valueOf(ps));
        }
        if (startIdx > 0) {
            req.setParam(getStateId() + "_" + Request.START_IDX, String.valueOf(startIdx));
        }
        if (!CollectionUtil.isEmpty(dataModel.getFilters())) {
            req.setParam(getStateId() + "_" + Request.FILTERS, Request.toFilterStr(dataModel.getFilters()));
        }
        if (req.getSortInfo() != null) {
            req.setParam(getStateId() + "_" + Request.SORT_INFO, String.valueOf(req.getSortInfo()));
        }
        if (hlIdx >= 0) {
            req.setParam(getStateId() + "_" + HIGHLIGHTED_ROW_IDX, String.valueOf(hlIdx));
        }
    }

    public void moveToRequestState(final Request req, final AsyncCallback callback) {

        if (getTable() == null) return;

        int rps = Math.max(0, req.getIntParam(getStateId() + "_" + Request.PAGE_SIZE));
        int lps = Math.max(0, dataModel.getPageSize());
        int rsIdx = Math.max(0, req.getIntParam(getStateId() + "_" + Request.START_IDX));
        int lsIdx = Math.max(0, getTable().getCurrentPage() * getTable().getPageSize());
        List<String> filters = Request.parseFilters(req.getParam(getStateId() + "_" + Request.FILTERS));
        final SortInfo sortInfo = SortInfo.parse(req.getParam(getStateId() + "_" + Request.SORT_INFO));
        int selIdx = req.getIntParam(getStateId() + "_" + HIGHLIGHTED_ROW_IDX);

        boolean doRefresh = (rps != 0 && rps != lps) || (rsIdx != lsIdx);
        doRefresh = doRefresh || !Request.toFilterStr(filters).equals(Request.toFilterStr(dataModel.getFilters()));
        doRefresh = doRefresh || !String.valueOf(sortInfo).equals(String.valueOf(dataModel.getSortInfo()));

        if (doRefresh) {
            rps = rps == 0 ? dataModel.getPageSize() : rps;
            int page = rsIdx / rps;
            dataModel.setFilters(filters);
            dataModel.setSortInfo(sortInfo);

            reloadTable(page, rps, selIdx);
            getEventManager().addListener(ON_PAGE_LOAD, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    TablePanel.this.getEventManager().removeListener(ON_PAGE_LOAD, this);
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            onMoveToReqStateCompleted(req);
                            callback.onSuccess(null);
                        }
                    });
                }
            });
        } else {
            selIdx = selIdx < 0 ? 0 : selIdx;
            getTable().highlightRow(true, selIdx);
            onMoveToReqStateCompleted(req);
            callback.onSuccess(null);
        }
    }

    protected void onMoveToReqStateCompleted(Request req) {
    }

    public boolean isActive() {
        return true;
    }

    // FilterToggleSupport

    public void toggleFilters() {
        if (getTable() == null || getTable().getDataModel() == null) return;

        if (!isActiveView(TableView.NAME)) {
            getTable().togglePopoutFilters(filters, PopupPane.Align.BOTTOM_LEFT);
        } else {
            if (getTable().isShowFilters()) {
                getTable().showFilters(false);
            } else {
                if (!isTableLoaded()) {
                    showNotLoadedWarning();
                } else {
                    getTable().setFilters(table.getDataModel().getFilters());
                    getTable().showFilters(true);
                }
            }
            //reinit();
        }
    }

    public List<String> getFilters() {
        return getTable().getFilters();
    }

    public void clearFilters() {
        getTable().setFilters(null);
        doFilters();
    }

    public void onResize() {
        for (View view : getViews()) {
            if (view != null && view.getDisplay() instanceof RequiresResize) {
                ((RequiresResize) view.getDisplay()).onResize();
            }
        }
    }


//====================================================================
//  Inner classes
//====================================================================

    private class DSModelHandler implements ModelEventHandler {
        private boolean fireStaleEventOnload = false;

        public void onFailure(Throwable caught) {
            updateTableStatus();
        }

        public void onLoad(TableDataView result) {
            updateTableStatus();
            if (fireStaleEventOnload) {
                fireStaleEventOnload = false;
                fireStaleEvent();
            }
            if (!expanded && handleEvent) {
                getEventManager().fireEvent(new WebEvent(TablePanel.this, ON_DATA_LOAD));
            }
        }

        public void onStatusUpdated(TableDataView result) {
            updateTableStatus();
        }

        public void onDataStale(DataSetTableModel model) {
            if (handleEvent) {
                reloadTable(0);
            }
        }

        public void fireStaleEventOnload() {
            this.fireStaleEventOnload = true;
        }
    }

    private class CustomColumnSorter extends SortableGrid.ColumnSorter {

        public void onSortColumn(SortableGrid grid, TableModelHelper.ColumnSortList sortList,
                                 SortableGrid.ColumnSorterCallback callback) {

            if (tableTooLarge) {
                showTooLargeWarning();
            } else if (tableNotLoaded) {
                showNotLoadedWarning();
            } else {
                // Get the primary column and sort order
                int column = sortList.getPrimaryColumn();
                SortInfo prevSortInfo = dataModel.getSortInfo();
                if (prevSortInfo != null) {
                    String prevCol = prevSortInfo.getPrimarySortColumn();
                    ColDef col = (ColDef) table.getTableDefinition().getVisibleColumnDefinitions().get(column);
                    if (col != null && col.getName().equals(String.valueOf(prevCol))) {
                        if (prevSortInfo.getDirection().equals(SortInfo.Direction.DESC)) {
                            sortList.clear();
                            dataModel.setSortInfo(null);
                            table.clearSortIndicator();
                        }
                    }
                }

                table.gotoPage(0, true);
                if (sortList.size() > 0) {
                    callback.onSortingComplete();
                }
                onSorted();
            }
        }
    }

    public static class HighlightedKeyMove implements KeyDownHandler {
        private FixedWidthGrid table;

        public HighlightedKeyMove(FixedWidthGrid table) {
            this.table = table;
        }

        private void adjustHighligtedIdx(int offset) {

            Set<Integer> hlighted = table.getSelectedRows();
            int hlrow = hlighted == null || hlighted.size() == 0 ? 0 : hlighted.toArray(new Integer[hlighted.size()])[0];
            if (hlrow > -1) {
                int idx = hlrow + offset;
                if (idx >= 0 &&
                        idx < table.getRowCount()) {
                    table.selectRow(idx, true);
                }
            }
        }


        public void onKeyDown(KeyDownEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_DOWN) {
                adjustHighligtedIdx(1);
                event.stopPropagation();
            } else if (event.getNativeKeyCode() == KeyCodes.KEY_UP) {
                adjustHighligtedIdx(-1);
                event.stopPropagation();
            }
        }
    }


    public static class TableDataViewToTableAdapter {

        private TablePanel table;
        private TableDataView dataset;
        boolean sinkEvent = true;

        public TableDataViewToTableAdapter(TablePanel table, TableDataView dataset) {
            this.table = table;
            this.dataset = dataset;
            bind();
        }

        private boolean isEqual(TableData.Row r1, TableData.Row r2, String col) {
            if (r1 == null || r2 == null) return false;
            String cv = String.valueOf(r1.getValue(col));
            String tv = String.valueOf(r2.getValue(col));
            return cv.equals(tv);
        }

        protected void bind() {

            final List<String> relatedCols = table.getDataset().getMeta().getRelatedCols();
            final HTMLTable.RowFormatter rowFormatter = table.getTable().getDataTable().getRowFormatter();

            if (relatedCols.size() > 0) {
                table.getTable().getDataTable().addRowSelectionHandler(new RowSelectionHandler() {
                    public void onRowSelection(RowSelectionEvent event) {

                        if (sinkEvent) {
                            sinkEvent = false;
                            Set<TableEvent.Row> rows = event.getSelectedRows();
                            if (rows != null && rows.size() > 0) {
                                TableData.Row row = table.getTable().getRowValue(rows.iterator().next().getRowIndex());
                                for (int r = 0; r < table.getRowCount(); r++) {
                                    boolean isRelated = true;
                                    for (String c : relatedCols) {
                                        TableData.Row cRow = table.getTable().getRowValue(r);
                                        if (cRow == row || !isEqual(row, cRow, c)) {
                                            isRelated = false;
                                            break;
                                        }
                                    }
                                    if (isRelated) {
                                        rowFormatter.addStyleName(r, "related");
                                    } else {
                                        rowFormatter.removeStyleName(r, "related");
                                    }
                                }
                            }
                            sinkEvent = true;
                        }
                    }
                });

            }

            table.getTable().getDataTable().addRowSelectionHandler(new RowSelectionHandler() {
                public void onRowSelection(RowSelectionEvent event) {

                    if (sinkEvent) {
                        sinkEvent = false;
                        Set<TableEvent.Row> rowIdxs = event.getSelectedRows();
                        if (rowIdxs.size() > 0) {
                            int idx = rowIdxs.iterator().next().getRowIndex() + table.getTable().getAbsoluteFirstRowIndex();
                            dataset.highlight(idx);
                            if (table.getTable().getHighlightedRowIdx() >= 0) {
                                table.getTable().scrollHighlightedIntoView();
                            }
                        }
                        sinkEvent = true;
                    }
                }
            });

            dataset.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent pce) {
                    try {
                        if (sinkEvent) {
                            sinkEvent = false;
                            if (table.getTable().getDataTable().getRowCount() > 0) {
                                if (pce.getPropertyName().equals(DataSet.ROW_HIGHLIGHTED)) {
                                    Integer idx = (Integer) pce.getNewValue();
                                    table.getTable().highlightRow(true, idx);
                                } else if (pce.getPropertyName().equals(DataSet.ROW_CLEARHIGHLIGHTED)) {
                                    table.getTable().clearHighlighted();
                                }
                            }
                            sinkEvent = true;
                        }
                    } catch (Exception e) {
                        GWT.log(e.getMessage(), e);
                    }
                }
            });
        }

    }

//====================================================================
//
//====================================================================

    public interface View {
        int getViewIdx();       //-1 for default order, use other number to override.

        Name getName();

        String getShortDesc();

        Widget getDisplay();

        void onViewChange(View newView);

        TablePanel getTablePanel();

        void onMaximize();

        void onMinimize();

        ImageResource getIcon();

        void bind(TablePanel table);

        void bind(EventHub hub);

        boolean isHidden();

        void setHidden(boolean flg);

    }


}

