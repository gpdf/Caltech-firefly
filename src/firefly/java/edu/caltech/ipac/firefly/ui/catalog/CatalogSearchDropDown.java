/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.IrsaCatalogDropDownCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.core.SearchAdmin;
import edu.caltech.ipac.firefly.core.background.ActivationFactory;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitor;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.NewTableResults;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.rpc.SearchServicesAsync;
import edu.caltech.ipac.firefly.ui.DefaultWorkingWidget;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.searchui.LoadCatalogFromVOSearchUI;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.List;


/**
 * @author Trey Roby
 */
public class CatalogSearchDropDown {
    private static final WebClassProperties _prop= new WebClassProperties(CatalogSearchDropDown.class);


    private boolean _showing= false;
    private SimplePanel _mainPanel= new SimplePanel();
    private CatalogPanel _catalogPanel= null;
    private LoadCatalogFromVOSearchUI voSearchUI = null;
    private TabPane<Widget> _tabs = new TabPane<Widget>();
    private SubmitKeyPressHandler keyPressHandler= new SubmitKeyPressHandler();
    private final boolean useSearchAdmin;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public CatalogSearchDropDown(String projectId, boolean useSearchAdmin) {

        this.useSearchAdmin= useSearchAdmin;
        _tabs.addTab(createSearchCatalogsContent(projectId),"Search Catalogs");
        _tabs.addTab(createLoadCatalogsContent(),"Load Catalog");
        _tabs.addTab(createLoadCatalogFromVOContent(), "VO Catalog");
        _mainPanel.setWidget(_tabs);
        _mainPanel.addStyleName("content-panel");
        _mainPanel.setSize("95%", "550px");
        _tabs.setSize("99%", "99%");
        _tabs.selectTab(0);

//        _mainPanel.add(title);
//        _mainPanel.setCellHeight(title, "1px");
//        _mainPanel.setCellHorizontalAlignment(title, HasHorizontalAlignment.ALIGN_CENTER);


    }


    private Widget createSearchCatalogsContent(String projectId) {
        _catalogPanel = new CatalogPanel(null, projectId);
        _catalogPanel.setSize("95%", "95%");
        _catalogPanel.addKeyPressOnCreation(keyPressHandler);

//        HorizontalPanel buttons= new HorizontalPanel();
//        buttons.addStyleName("base-dialog-buttons");
//        GwtUtil.setStyle(buttons, "cssFloat", "none");
//        buttons.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);

        FlowPanel buttons= new FlowPanel();
        buttons.setWidth("100%");

        Button ok= new Button("Search");
        ok.addStyleName("highlight-text");
        ok.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) { doOK(); }
        });

        buttons.add(ok);
        Widget help= HelpManager.makeHelpIcon("basics.catalog");
        buttons.add(help);

        GwtUtil.setStyles(ok, "cssFloat", "left",
                                "margin", "0 0 0 37px",
                                "color", "black",
                                "fontWeight", "bold");

        GwtUtil.setStyles(help, "cssFloat", "right",
                          "", "0 20px 0 0");


        VerticalPanel vp= new VerticalPanel();
        vp.add(_catalogPanel);
        vp.add(buttons);

        vp.setCellHorizontalAlignment(_catalogPanel, VerticalPanel.ALIGN_CENTER);
        vp.setSize("95%", "450px");
        return vp;
    }

    private void doOK() {
        try {
            if (validateInput()) {
                inputCompleteAsync();
            }
        } catch (ValidationException e) {
            PopupUtil.showError("Error", e.getMessage());
        }
    }



    private Widget createLoadCatalogsContent() {
        SimpleInputField field = SimpleInputField.createByProp(_prop.makeBase("upload"));
        final FileUploadField _uploadField= (FileUploadField)field.getField();

        ButtonBase ok= GwtUtil.makeFormButton("Upload", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (_uploadField.validate()) {
                    _uploadField.submit(new AsyncCallback<String>() {

                        public void onFailure(Throwable caught) {
                            if (caught != null) PopupUtil.showSevereError(caught);
                        }

                        public void onSuccess(final String filepath) {
                            // filepath is returned
                            final TableServerRequest req = new TableServerRequest(CommonParams.USER_CATALOG_FROM_FILE);
                            req.setParam("filePath", filepath);
                            req.setStartIndex(0);
                            req.setPageSize(50);
                            if (useSearchAdmin) {
                                SearchAdmin.getInstance().submitSearch(req, "Table Upload");
                                hide();
                            }
                            else {
                                SearchServices.App.getInstance().getRawDataSet(req, new AsyncCallback<RawDataSet>() {

                                    public void onFailure(Throwable caught) {
                                        if (caught != null) PopupUtil.showSevereError(caught);
                                    }

                                    public void onSuccess(RawDataSet result) {
                                        String basename;
                                        String fullPath = _uploadField.getUploadFilename();
                                        int idx = fullPath.lastIndexOf('/');
                                        if (idx<0) idx = fullPath.lastIndexOf('\\');
                                        if (idx > 1) {
                                            basename = fullPath.substring(idx+1);
                                        } else {
                                            basename = fullPath;
                                        }
                                        newRawDataSet(basename, result, req);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        ButtonBase cancel = GwtUtil.makeFormButton("Cancel", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                _tabs.selectTab(0);
            }
        });

        Form.ButtonBar buttonBar = new Form.ButtonBar();
        buttonBar.setVisible(false);
        buttonBar.addStyleName("button-bar");
        buttonBar.addRight(ok);
        buttonBar.addRight(cancel);
        buttonBar.getHelpIcon().setHelpId("basics.loadcatalog");

        HTML text = GwtUtil.makeFaddedHelp("Custom catalog in IPAC table format&nbsp;&nbsp;");

        Label pad = new Label(" ");
        pad.setHeight("400px");

        FlexTable grid = new FlexTable();
        grid.setCellSpacing(5);
        DOM.setStyleAttribute(grid.getElement(), "padding", "5px");
        grid.setWidget(0, 0, field);
        grid.setWidget(1, 0, text);
        grid.setWidget(2, 0, buttonBar);
        grid.setWidget(3, 0, pad);

        return grid;
    }

    private Widget createLoadCatalogFromVOContent() {
        voSearchUI = new LoadCatalogFromVOSearchUI();
        ButtonBase ok= GwtUtil.makeFormButton("Search", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (!voSearchUI.validate()) {
                    return;
                }
                voSearchUI.makeServerRequest(new AsyncCallback<ServerRequest>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        if (caught != null) PopupUtil.showSevereError(caught);
                    }

                    @Override
                    public void onSuccess(ServerRequest request) {
                        final TableServerRequest treq = (TableServerRequest) request;

                        final DefaultWorkingWidget working= new DefaultWorkingWidget((ClickHandler)null);
                        working.setText("Retrieving VO Catalog...");
                        final MaskPane maskPane = new MaskPane(_mainPanel, working);
                        maskPane.show();

                        SearchServicesAsync serv= SearchServices.App.getInstance();
                        serv.submitBackgroundSearch(treq, null, 3000, new AsyncCallback<BackgroundStatus>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                if (caught != null) PopupUtil.showSevereError(caught);
                                maskPane.hide();
                            }

                            @Override
                            public void onSuccess(BackgroundStatus bgStat) {
                                WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.CATALOG_SEARCH_IN_PROCESS));
                                MonitorItem monItem= new MonitorItem(treq, voSearchUI.getSearchTitle(), BackgroundUIHint.CATALOG, false);
                                monItem.setStatus(bgStat);
                                monItem.setActivateOnCompletion(true);
                                if (bgStat.isSuccess()) {
                                    ActivationFactory.getInstance().activate(monItem);
                                    maskPane.hide();
                                    hide();
                                }
                                else {
                                    handleBackgrounding(monItem);
                                }
                            }

                            private void handleBackgrounding(final MonitorItem monItem) {
                                working.setText("Backgrounding...");
                                Timer t= new Timer() {
                                    @Override
                                    public void run() {
                                        BackgroundMonitor monitor= Application.getInstance().getBackgroundMonitor();
                                        Application.getInstance().getBackgroundManager().animateToManager(0, 0, 1300);
                                        monItem.setWatchable(true);
                                        monitor.addItem(monItem);
                                        maskPane.hide();
                                        hide();
                                    }
                                };
                                t.schedule(1000);
                            }
                        });
                    }
                });
            }
        });
        ButtonBase cancel = GwtUtil.makeFormButton("Cancel", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                _tabs.selectTab(0);
            }
        });
        Form.ButtonBar buttonBar = new Form.ButtonBar();
        buttonBar.setVisible(false);
        buttonBar.addStyleName("button-bar");
        buttonBar.addRight(ok);
        buttonBar.addRight(cancel);
        buttonBar.getHelpIcon().setHelpId("basics.loadcatalog");

        VerticalPanel vp = new VerticalPanel();
        vp.add(voSearchUI.createLoadCatalogsContent());
        vp.add(buttonBar);
        DOM.setStyleAttribute(vp.getElement(), "padding", "5px");
        SimplePanel sp = new SimplePanel();
        sp.add(vp);

        return sp;
    }

    private void newRawDataSet(String title, RawDataSet rawDataSet, TableServerRequest req) {
        DataSet ds= DataSetParser.parse(rawDataSet);
        if (ds.getTotalRows()>0) {
            NewTableResults data= new NewTableResults(req, WidgetFactory.TABLE, title);
            WebEvent<NewTableResults> ev= new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED,
                    data);
            WebEventManager.getAppEvManager().fireEvent(ev);
            hide();
        }
        else {
            PopupUtil.showError("No Rows returned", "The search did not find any data");
        }
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public void hide() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                _showing= false;
                hideOnSearch();
                Application.getInstance().getToolBar().getDropdown().close();
            }
        });
    }

    protected void hideOnSearch() { }

    public void show() {
        _showing= true;
        if (_catalogPanel!=null) _catalogPanel.showPanel();
        if (voSearchUI != null) voSearchUI.updateActiveTarget();
        Application.getInstance().getToolBar().getDropdown().setTitle(_prop.getTitle());
        Application.getInstance().getToolBar().getDropdown().setContent(_mainPanel, true, null,
                                                          IrsaCatalogDropDownCmd.COMMAND_NAME);
    }


    public void inputCompleteAsync() {
        if (_catalogPanel.isAsyncCallRequired()) {
            _catalogPanel.getFieldValuesAsync(new AsyncCallback<List<Param>>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(List<Param> params) {
                    performSearch(params);

                }
            });
        }
        else {
            performSearch(_catalogPanel.getFieldValues());
        }
    }

    private void performSearch(List<Param> params) {
        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
        req.setParams(params);
        req.setUse(CatalogRequest.Use.CATALOG_OVERLAY);
        if (useSearchAdmin) {
            hide();
            SearchAdmin.getInstance().submitSearch(req, req.getQueryCatName());
        }
        else {
            Widget w= _mainPanel.getParent();
            int cX= w.getAbsoluteLeft()+ w.getOffsetWidth()/2;
            int cY= w.getAbsoluteTop()+ w.getOffsetHeight()/2;
            IrsaCatalogTask.getCatalog(_mainPanel,req,new CatalogSearchResponse(){
                public void showNoRowsReturned() {
                    PopupUtil.showError(_prop.getTitle("noRowsReturned"),
                                        _prop.getError("noRowsReturned"));
                }

                public void status(RequestStatus requestStatus) {
                    hide();
                }
            },cX,cY, _catalogPanel.getTitle());
        }
    }


    protected boolean validateInput() throws ValidationException {
        return _catalogPanel.validatePanel();
    }

    public class SubmitKeyPressHandler implements KeyPressHandler {
        public void onKeyPress(KeyPressEvent ev) {
            final int keyCode = ev.getNativeEvent().getKeyCode();
            char charCode = ev.getCharCode();
            if ((keyCode == KeyCodes.KEY_ENTER || charCode == KeyCodes.KEY_ENTER) && ev.getRelativeElement() != null) {
                DeferredCommand.addCommand(new Command() {
                    public void execute() {
                        doOK();
                    }
                });
            }
        }
    }

}
