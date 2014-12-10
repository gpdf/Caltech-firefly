package edu.caltech.ipac.hydra.ui.planck;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.data.NewTabInfo;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseTableButtonSetter;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorkerCreator;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.*;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;
import com.google.gwt.i18n.client.NumberFormat;


import java.util.*;

/**
 */
public class MiniMapButtonCreator implements EventWorkerCreator {
    public static final String ID = "PlanckMiniMap";
    private final static NumberFormat nf= NumberFormat.getFormat("#.00");

    public EventWorker create(Map<String, String> params) {
        MiniMapButtonSetter worker = new MiniMapButtonSetter();
        worker.setQuerySources(StringUtils.asList(params.get(EventWorker.QUERY_SOURCE), ","));
        if (params.containsKey(EventWorker.ID)) worker.setID(params.get(EventWorker.ID));

        return worker;
    }

    public static class MiniMapButtonSetter extends BaseTableButtonSetter {
        private TableDataView dataset;
        private TablePanel tablePanel;
        private BaseDialog dialog;
        boolean isSelectAll;
        int totalSel;

        public MiniMapButtonSetter() {
            super(ID);
        }

        protected FocusWidget makeButton(final TablePanel table) {
            tablePanel = table;

            final Button button = GwtUtil.makeButton("Make Minimap", "Generate Minimap Image", new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    if (dialog == null) {
                        dialog = new BaseDialog(table, ButtonType.OK_CANCEL, "Minimap generation", true, null) {
                            protected void inputComplete() {
                                dialog.setVisible(false);
                                generateMiniMap();
                            }

                            protected void inputCanceled() {
                                dialog.setVisible(false);
                            }
                        };
                    }
                    final HTML content = new HTML("You are sending a request for Minimap generation<br><br>");
                    //final HTML content = FormBuilder.createPanel();
                    content.setHTML(content.getHTML() + "<br>" + "<b>Selected rows: </b> <br>");

                    //get all the rows.. then find the selected.
                    table.getDataModel().getAdHocData(new BaseCallback<TableDataView>() {
                        public void doSuccess(TableDataView result) {
                            int rowcount = table.getDataset().getTotalRows();
                            totalSel = 0;
                            for (int i : table.getDataset().getSelected()) {
                                TableData.Row row = result.getModel().getRow(i);
                                totalSel += 1;
                                content.setHTML(content.getHTML() + " " + i + ";");
//                                    content.setHTML(content.getHTML() + " " + i + " - " + StringUtils.toString(row.getValues().values()) + ";");
                            }
                            content.setHTML(content.getHTML() + "<br><br>" + "<b>Total rows selected: </b> " + totalSel + " out of " + rowcount + "<br>");
                            if (totalSel==rowcount) {
                                isSelectAll = true;
                            }
                            else {
                                isSelectAll = false;
                            }
                        }
                    }, null);
                    content.setSize("600px", "300px");
                    dialog.setWidget(content);
                    dialog.show();
                }
            });
            dataset = table.getDataset();


            button.setEnabled(checkSelection());
            dataset.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent pce) {
                    button.setEnabled(checkSelection());
                }
            });

            return button;
        }

        private void generateMiniMap() {
            //set condition if minimap or hires
            tablePanel.getDataModel().getAdHocData(new BaseCallback<TableDataView>() {
                 public void doSuccess(TableDataView result) {
                     NewTabInfo newTabInfo = new NewTabInfo("Minimap");
                     MiniPlotWidget mpw = makeImagePlot(result, newTabInfo);
                     newTabInfo.setDisplay(mpw);
                     WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.NEW_TABLE_RETRIEVED, newTabInfo));
                 }
             },null);

        }

        private MiniPlotWidget makeImagePlot(final TableDataView tableData, final NewTabInfo newTabInfo) {
            final MiniPlotWidget mpw = new MiniPlotWidget(newTabInfo.getName());
            GwtUtil.setStyles(mpw, "fontFamily", "tahoma,arial,helvetica,sans-serif",
                    "fontSize", "11px");
            mpw.setRemoveOldPlot(true);
            mpw.setMinSize(200, 200);
            mpw.setAutoTearDown(false);
            mpw.setLockImage(false);
            mpw.setInlineTitleAlwaysOnIfCollapsed(true);
            mpw.addStyleName("standard-border");
            mpw.getOps(new MiniPlotWidget.OpsAsync() {
                public void ops(PlotWidgetOps widgetOps) {
                    ServerRequest sreq = tablePanel.getDataModel().getRequest();
                    String baseUrl = sreq.getSafeParam("toiminimapHost");
                    String Freq = sreq.getSafeParam("planckfreq");
                    String detector = sreq.getParam("detector");
                    String radius = sreq.getSafeParam("radius");
                    String boxsize = sreq.getSafeParam("boxsize");
                    String type = sreq.getSafeParam("type");
                    String ssoflag = sreq.getSafeParam("ssoflag");
                    String ExpandedDesc, desc;
                    String trangeStr = "";
                    String ssoStr = "";

                    WorldPt pt;
                    String pos = null;
                    String gpos = null;
                    String userTargetWorldPt = sreq.getParam(ReqConst.USER_TARGET_WORLD_PT);
                    if (userTargetWorldPt != null) {
                        pt = WorldPt.parse(userTargetWorldPt);
                        if (pt != null) {
                            pt = VisUtil.convertToJ2000(pt);
                            pt = VisUtil.convert(pt, CoordinateSys.GALACTIC);
                            pos = pt.getLon() + "," + pt.getLat();
                            if (nf.format(pt.getLat()).startsWith("-")) {
                                gpos = "G" + nf.format(pt.getLon())  + nf.format(pt.getLat());
                            } else {
                                gpos = "G" + nf.format(pt.getLon()) + "+" + nf.format(pt.getLat());
                            }
                        }
                    }

                    String targetStr = null;
                    String targetName = sreq.getSafeParam("TargetPanel.field.targetName");
                    if (targetName == null) {
                        targetStr = sreq.getSafeParam("UserTargetWorldPt");
                        targetName = targetStr.replace(";", ",");
                    }
                    targetStr = targetName.replace(" ", "");


                    String optBand = Freq;
                    if (!StringUtils.isEmpty(Freq)) {
                        if (Freq.equals("030")) {
                            optBand = "30000";
                        } else if (Freq.equals("044")) {
                            optBand = "44000";
                        } else if (Freq.equals("070")) {
                            optBand = "70000";
                        }
                    }

                    String size = null;
                    if (type.equals("circle")) {
                        size = Double.toString(2.*StringUtils.getDouble(radius));
                    } else if (type.equals("box")){
                        size = Double.toString(StringUtils.getDouble(boxsize));
                    }


                    String timeSelt = "";
                    String timeStr = "";
                    int selectedRowCount = totalSel;

                    for (int i : tablePanel.getDataset().getSelected()) {
                        TableData.Row row = tableData.getModel().getRow(i);
                        timeSelt += row.getValue("mjd") + ",";
                    }
                    String timeStrArr[] = timeSelt.split(",");
                    String tBegin = timeStrArr[0];
                    String tEnd = timeStrArr[timeStrArr.length-1];
                    trangeStr = tBegin +"-" + tEnd;

                    if (isSelectAll){
                        timeStr = "[]";
                    }
                    else {
                        timeStr = "[";
                        for (int j = 0; j < timeStrArr.length; j++) {
                            double t1, t2;
                            double t = Double.parseDouble(timeStrArr[j]);
                            t1 = t - 0.5;
                            t2 = t + 0.5;
                            if (j != timeStrArr.length - 1) {
                                timeStr += "[" + Double.toString(t1) + "," + Double.toString(t2) + "],";
                            } else {
                                timeStr += "[" + Double.toString(t1) + "," + Double.toString(t2) + "]";

                            }
                        }
                        timeStr += "]";
                    }

                    String detectors[] = sreq.getParam(detector).split(",");
                    String detc_constr;
                    String detcStr;

                    if (detectors[0].equals("_all_")) {
                        detc_constr = "[]";
                        detcStr = "all";
                    } else {
                        detc_constr = "['" + detectors[0] + "'";
                        detcStr = detectors[0];
                        for (int j = 1; j < detectors.length; j++) {
                            detc_constr += ",'" + detectors[j] + "'";
                            detcStr += "," + detectors[j];
                        }
                        detc_constr += "]";
                    }

                    String interations = "0";

                    if (ssoflag.equals("false")){
                        ssoStr = "0";
                    } else if (ssoflag.equals("true")){
                        ssoStr = "2";
                    }


                    ServerRequest req = new ServerRequest("planckTOIMinimapRetrieve", sreq);

                    // add all of the params here.. so it can be sent to server.

                    req.setParam("pos", pos);
                    req.setParam("detc_constr", detc_constr);
                    req.setParam("optBand", optBand);
                    req.setParam("baseUrl", baseUrl);
                    req.setParam("timeStr", timeStr);
                    req.setParam("iterations", interations);
                    req.setParam("size", size);
                    req.setParam("targetStr", targetStr);
                    req.setParam("detcStr", detcStr);
                    req.setParam("ssoStr", ssoStr);
                    desc = gpos + "_" + Freq + "GHz_Minimap";
                    ExpandedDesc = "Minimap with " + desc + ", date range " + trangeStr + ", total "+ selectedRowCount
                                                                            + " date(s) selected, Detector(s): " + detcStr;;

                    // add all of the params here.. so it can be sent to server.
                    WebPlotRequest wpr = WebPlotRequest.makeProcessorRequest(req, ExpandedDesc);
                    wpr.setInitialZoomLevel(8);
                    wpr.setInitialColorTable(4);
                    wpr.setExpandedTitle(ExpandedDesc);
                    wpr.setHideTitleDetail(false);
                    wpr.setShowTitleArea(true);
                    wpr.setDownloadFileNameRoot("planck_toi_search_" + desc);
                    wpr.setTitle(ExpandedDesc);

                    //wpr.setWorldPt(pt);
                    //wpr.setSizeInDeg(size);
                    //wpr.setZoomType(ZoomType.TO_WIDTH);
                    //wpr.setZoomToWidth(width);
                    wpr.setHasMaxZoomLevel(false);

                    widgetOps.plot(wpr, false, new BaseCallback<WebPlot>() {
                        public void doSuccess(WebPlot result) {
                            newTabInfo.ready();
                        }
                    });
                }
            });
            return mpw;
        }

        private boolean checkSelection() {
            return dataset != null && dataset.getSelectionInfo().getSelectedCount() > 0;
        }
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
