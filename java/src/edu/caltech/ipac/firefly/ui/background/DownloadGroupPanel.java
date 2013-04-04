package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.ProgressBar;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitor;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.PropFile;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.Notifications;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;

/**
 * User: roby
 * Date: Oct 24, 2008
 * Time: 9:57:02 AM
 */


/**
 * @author Trey Roby
 */
public class DownloadGroupPanel extends Composite {

    public enum FileDownloadStatus {NONE, WORKING, DONE}

    interface PFile extends PropFile { @Source("DownloadGroupPanel.prop") TextResource get(); }

    private static WebClassProperties _prop= new WebClassProperties(DownloadGroupPanel.class, (PFile) GWT.create(PFile.class));
    private static final String STATUS_STARTING_TXT= _prop.getName("status.starting");
    private static final String STATUS_STARTING_PART_TXT= _prop.getName("status.starting.part");
    private static final String STATUS_WAITING_PART_TXT= _prop.getName("status.waiting.part");
    private static final String STATUS_CANCELED_TXT= _prop.getName("status.canceled");
    private static final String STATUS_NODETAIL_DETAIL_TXT= _prop.getName("noDetails.detail");

    private static final String STATUS_ABORTED_DETAIL_TXT= _prop.getName("status.aborted.detail");
    private static final String STATUS_FAIL_DETAIL_TXT= _prop.getName("status.fail.detail");
    private static final String STATUS_CANCELED_DETAIL_TXT= _prop.getName("status.canceled.detail");

    private static final String ABORT_TIP= _prop.getTip("abort");
    private static final String DELETE_TIP= _prop.getTip("delete");


    private static final String EMAIL_SENT=     _prop.getName("emailSent");
    private static final String EMAIL_NOT_SENT= _prop.getName("emailNotSent");

    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();



    private final static int TITLE_POS        = 0;
    private final static int SUBTITLE_POS     = 1;
    private final static int STATUS_POS       = 2;
    private final static int STATUS_ALT_POS   = 0;
    private final static int WORKING_ICON_POS = 4;
    private final static int ABORT_BUTTON_POS = 5;
    private final static int WARNINGS_POS     = 2;
    private final static int FOOTNOTE_POS_A   = 0;
    private final static int FOOTNOTE_POS_B   = 2;

    private final FlexTable _content= new FlexTable();
    private final String      _title;
    private final MonitorItem _monItem;
    private final Image       _workingIcon= new Image(GwtUtil.LOADING_ICON_URL);
    private Widget            _warningsButton;
    private BackgroundReport  _oldReport;
    private DetailUIInfo      _detailUI[];
    private FocusPanel        _abortButton;
    private boolean           _aborted= false;
    private boolean           _success= false;
    private boolean           _calledAutoActivation= false;



//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    DownloadGroupPanel(MonitorItem monItem) {
        SimplePanel topPanel= new SimplePanel();
        initWidget(topPanel);
        topPanel.setWidget(_content);
        _monItem= monItem;
        _oldReport= monItem.getReport();
        _title= monItem.getTitle();
        layout();

    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    /**
     * update the package report and the display the user seee.
     * @return true, if the updated report requires the uers attension, usually the
     *         is a download ready. false, otherwise
     */
    boolean updateUI() {
        BackgroundReport report= _monItem.getReport();
        WebAssert.argTst(ComparisonUtil.equals(report.getID(), _oldReport.getID()),
                         "You cannot update the report to one with " +
                         "a different package id.");
        boolean retval= update();
        _oldReport= _monItem.getReport();
        if (_monItem.isDone()) {
            if (_monItem.getState()==BackgroundState.SUCCESS)  {
               if (!_success)  {
                   String name= Application.getInstance().getAppName();
                   Notifications.notify( name + " Task Completed",
                                         _monItem.getReportDesc() +", " +_monItem.getTitle() +" has completed.");
               }
                _success= true;
                if (!_calledAutoActivation &&
                        !_monItem.isRecreated() &&
                        _monItem.getActivateOnCompletion() &&
                        _monItem.getReport().getPartCount()==1) {
                    _calledAutoActivation= true;
                    _monItem.activate(0,true);
                }
            }
        }

        return retval;

    }


    int getPartsLines() {
        int retval;
        int size= getPartCount(_monItem);
        if (isFailure(_monItem.getReport())) {
            retval= 1;
        }
        else if (size==0){
            retval= 1;
        }
        else if (size<3){
            retval= size;
        }
        else {
           retval= size/2 + 1;
        }
        return retval;
    }

    MonitorItem getMonitorItem() { return _monItem; }

    private static int getPartCount(MonitorItem item) {
        return getPartCount(item,item.getReport());
    }
    private static int getPartCount(MonitorItem item, BackgroundReport report) {
        int size= report.getPartCount();
        if (!item.getShowParts() && size>0) {
            size= 1;
        }
        return size;
    }

    private static BackgroundState getPartState(MonitorItem item, int idx) {
        return getPartState(item,item.getReport(),idx);
    }

    private static BackgroundState getPartState(MonitorItem item, BackgroundReport report, int idx) {
        if (item.getShowParts()) {
            return report.getPartState(idx);
        }
        else {
            return report.getState();
        }
    }


    //TODO: work on this until it is correct
    public BackgroundManager.AttnState getAttentionState() {

        BackgroundReport report= _monItem.getReport();
        boolean ready= false;
        boolean working= false;
        boolean fail= false;
        int partCnt= getPartCount(_monItem);
        if (partCnt>0) {
            for(int i=0; (i<partCnt); i++) {
                BackgroundState pstate= getPartState(_monItem,i);
                if (pstate==BackgroundState.SUCCESS && !_monItem.isActivated(i)) {
                    ready= true;
                }
                else if (pstate== BackgroundState.FAIL) {
                    fail= true;
                }
                else if (!report.isDone()) {
                    working= true;
                }
            }
        }
        else {
            if (!report.isDone()) working= true;
        }


        BackgroundManager.AttnState attnState;

        if (fail) {
            attnState= BackgroundManager.AttnState.FAIL;
        }
        else if (ready && working) {
            attnState= BackgroundManager.AttnState.READY_WORKING;
        }
        else if (ready) {
            attnState= BackgroundManager.AttnState.READY;
        }
        else if (working) {
            attnState= BackgroundManager.AttnState.WORKING;
        }
        else {
            attnState= BackgroundManager.AttnState.NONE;
        }

        return attnState;
    }


    public int getUndownloadCnt() {
        int total= 0;
        BackgroundState state= _monItem.getReport().getState();
        if (state!=BackgroundState.FAIL && state!=BackgroundState.CANCELED ) {
            for(int i=0; (i<getPartCount(_monItem)); i++) {
                if (getPartState(_monItem,i)==BackgroundState.SUCCESS &&
                        !_monItem.isActivated(i) ) {
                    total++;
                }
            }
        }
        return total;
    }

    public int getWorkingCnt() {
        int total= 0;
        BackgroundState state= _monItem.getReport().getState();
        if (!_monItem.getReport().isDone()) {
            if (state!=BackgroundState.FAIL && state!=BackgroundState.CANCELED ) {
                for(int i=0; (i<getPartCount(_monItem)); i++) {
                    BackgroundState partState= getPartState(_monItem,i);
                    if (partState==BackgroundState.WAITING ||
                            partState==BackgroundState.WORKING ||
                            partState==BackgroundState.STARTING) {
                        total++;
                    }
                }
            }
        }
        return total;
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void layout() {


        _abortButton= makeAbortButton();
        _abortButton.addStyleName("download-group-abort");
        _content.addStyleName("download-group-content");
        _detailUI=  new DetailUIInfo[getPartCount(_monItem)];
        layoutDetails();
    }


//    private HTML getTitleWidget() {
//        HTML titleHTML= new HTML(_title);
//        titleHTML.addStyleName("group-title");
//        return titleHTML;
//    }

    private void updateItemTitle() {
        HTML titleHTML= new HTML(_title);
        titleHTML.addStyleName("group-title");
        FlexTable.FlexCellFormatter formatter= _content.getFlexCellFormatter();
        _content.setWidget(0, TITLE_POS,titleHTML);
        String width;
        if (_title.length()< 20)  width= "200px";
        else if (_title.length()< 20)  width= "250px";
        else if (_title.length()< 25)  width= "300px";
        else                          width= "350px";

        formatter.setWidth(0, TITLE_POS, width);
    }

    private void layoutDetails() {


        _content.clear();

        _content.addStyleName("download-group-header");

        updateItemTitle();
//        _content.setWidget(0, TITLE_POS,getTitleWidget());
        _content.setWidget(0, SUBTITLE_POS,_workingIcon);
        _content.setWidget(0, ABORT_BUTTON_POS, _abortButton);
        _content.setWidget(0, WORKING_ICON_POS, _workingIcon);
        FlexTable.FlexCellFormatter formatter= _content.getFlexCellFormatter();
        formatter.setWidth(0, ABORT_BUTTON_POS, "35px");
        formatter.setWidth(0, WORKING_ICON_POS, "20px");
//        formatter.setWidth(0, TITLE_POS, "350px");

        BackgroundReport report= _monItem.getReport();
        int partCnt= getPartCount(_monItem);
        if (_monItem.getState()==BackgroundState.STARTING ||
            (partCnt==0 && !report.isDone()) ) {
            Widget w= createWorkingWidget(STATUS_STARTING_TXT);
            _content.setWidget(0,STATUS_POS,w);
        }
        else {
            TablePos tpos;
            for(int i= 0; (i<partCnt); i++) {
                _detailUI[i]= new DetailUIInfo();
                tpos= getStatusPosCol(_monItem,i);
                _content.setWidget(tpos.getRow(), tpos.getCol(), makeDetailStateWidget(i));
                formatter.setWidth(tpos.getRow(), tpos.getCol(), "250px");
                if (tpos.getCol()==STATUS_ALT_POS) {
                    formatter.setHorizontalAlignment(tpos.getRow(), tpos.getCol(), HasHorizontalAlignment.ALIGN_RIGHT);
                }
                formatter.setWidth(i, ABORT_BUTTON_POS, "35px");
            }
        }



        _warningsButton= GwtUtil.makeLinkButton(_prop.makeBase("warnings"),
                                                new ClickHandler() {
                                                    public void onClick(ClickEvent ev) {
                                                        showWarnings((Widget)ev.getSource());
                                                    }
                                                });
        _content.setWidget(partCnt,WARNINGS_POS,_warningsButton);
//        formatter.setColSpan(partCnt,WARNINGS_POS,3 );
        formatter.setHorizontalAlignment(partCnt,WARNINGS_POS, HasHorizontalAlignment.ALIGN_RIGHT);
        _warningsButton.setVisible(false);
    }



    private void showWarnings(Widget p) {
        BackgroundReport report= _monItem.getReport();
        int num= report.getNumMessages();
        StringBuffer txt= new StringBuffer(300);
        if (num>0) {
            txt.append(_prop.getName("warnings.desc"));
            txt.append("<br>");
            txt.append("<ul>");
            for(int i=0; (i<num); i++) {
                txt.append("<li>");
                txt.append(report.getMessage(i));
                txt.append("</li>");
            }
            txt.append("</ul>");
        }
        else {
            txt.append(_prop.getName("nowarnings"));

        }
        PopupUtil.showInfo(p,_prop.getTitle("warnings"), txt.toString());
    }

    private boolean update() {

        boolean retval;
        if (getPartCount(_monItem)==getPartCount(_monItem,_oldReport)) {
            retval= updateDetails();
        }
        else {
            _detailUI=  new DetailUIInfo[getPartCount(_monItem)];
            layoutDetails();
            retval= updateDetails();
        }
        return retval;
    }





    private boolean updateDetails() {
        BackgroundReport newReport= _monItem.getReport();
        int partCount= getPartCount(_monItem);
        boolean open= false;

        if (newReport.isDone())  disableIndicators();

        if (isFailure(newReport)) {
            disableIndicators();
            _content.clear();
            Label label;
            switch (newReport.getState()) {
                case USER_ABORTED:
                    label= new Label(STATUS_ABORTED_DETAIL_TXT);
                    break;
                case CANCELED:
                    label= new Label(STATUS_CANCELED_DETAIL_TXT);
                    break;
                case FAIL:
                    label= new Label(STATUS_FAIL_DETAIL_TXT);
                    break;
                default:
                    label= new Label("");
                    break;

            }
            updateItemTitle();
//            _content.setWidget(0, TITLE_POS,getTitleWidget());
            _content.setWidget(0,SUBTITLE_POS,label);
            _content.setWidget(0, ABORT_BUTTON_POS, _abortButton);
            _content.getCellFormatter().setWidth(0,SUBTITLE_POS, "300px");
            DOM.setStyleAttribute(label.getElement(), "float", "right");
            if (_content.getRowCount()>1) {
                for(int i=1; (i<_content.getRowCount()); i++) {
                    _content.removeRow(i);
                }
            }


        }
        else if (partCount==0 && !newReport.isDone()) {
            String msg= STATUS_NODETAIL_DETAIL_TXT;
            if (newReport.getState()==BackgroundState.WAITING && !StringUtils.isEmpty(_monItem.getWaitingMsg())) {
                msg= _monItem.getWaitingMsg();
            }
            Label label= new Label(msg);
            _content.setWidget(0,STATUS_POS,label);
        }
        else {
            for(int i= 0; (i<partCount); i++) {
                TablePos tpos;
                if (i>=getPartCount(_monItem,_oldReport) || getPartState(_monItem,i) != getPartState(_monItem,_oldReport,i)) {
                    tpos= getStatusPosCol(_monItem,i);
                    _content.setWidget(tpos.getRow(),tpos.getCol(), makeDetailStateWidget(i));
                }
                BackgroundState state= getPartState(_monItem,i);

                if (newReport.isSuccess() && !_monItem.isActivated(i) ) {
                    open= true;
                }
                else if (state==BackgroundState.WORKING) {
                    _detailUI[i].setProgressDetail(newReport,i);
                }
            }
            _warningsButton.setVisible(newReport.getNumMessages()>0);

        }

        if (newReport.isSuccess())  addSuccessFootnote(newReport);


        return open;
    }


    private void addSuccessFootnote(BackgroundReport newReport) {


        if (newReport.isSuccess()) {
            int partCount= getPartCount(_monItem);
            TablePos tpos= getStatusPosCol(_monItem,partCount-1);
            int currentRow= tpos.getRow() + 1 + (partCount % 2);
            String paddingTop= "15px";

            if (newReport.hasAttribute(BackgroundReport.JobAttributes.CanSendEmail)) {
                boolean sent= newReport.hasAttribute(BackgroundReport.JobAttributes.EmailSent);
                if (sent) {
                    Label msgL= new Label(EMAIL_SENT);
                    GwtUtil.setStyle(msgL, "paddingTop", paddingTop);
                    _content.setWidget(currentRow, FOOTNOTE_POS_B, msgL);
                    _content.getFlexCellFormatter().setAlignment(currentRow,FOOTNOTE_POS_A, HasHorizontalAlignment.ALIGN_LEFT,
                                                                 HasVerticalAlignment.ALIGN_MIDDLE);
                }
            }


            if (partCount>1 && newReport.hasAttribute(BackgroundReport.JobAttributes.DownloadScript)) {
                Widget downButton= GwtUtil.makeLinkButton(_prop.makeBase("downloadScript"),new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        showDialogScriptDialog(_monItem.getID(),_monItem.getReport().getDataSource());
                    }
                });
                _content.getFlexCellFormatter().setAlignment(currentRow,FOOTNOTE_POS_B, HasHorizontalAlignment.ALIGN_LEFT,
                                                             HasVerticalAlignment.ALIGN_MIDDLE);
                _content.setWidget(currentRow, FOOTNOTE_POS_A, downButton);
                GwtUtil.setStyle(downButton, "paddingTop", paddingTop);
            }
        }

    }

    private void showDialogScriptDialog(String id, String dataSource) {
        DownloadScriptDialog.show(this, id, dataSource);
    }

    private static TablePos getStatusPosCol(MonitorItem monItem, int idx) {
        TablePos retval;
        int size= getPartCount(monItem);
        if (size<=3) {
            retval= new TablePos(idx,STATUS_POS);
        }
        else {
            int totalRows= size/2;
            if (size % 2 == 1)  totalRows++;

            if (idx < totalRows)  {
                retval= new TablePos(idx+1,STATUS_ALT_POS);
            }
            else {
                retval= new TablePos((idx-totalRows)+1,STATUS_POS);
            }
        }
        return retval;
    }


    private Widget makeDetailStateWidget(int idx) {

        Widget retval;
        String desc;

        BackgroundReport report= _monItem.getReport();
        BackgroundState partState= getPartState(_monItem, idx);
        int partCount= getPartCount(_monItem);
        if (isFailure(report)) partState= BackgroundState.CANCELED;
        switch (partState) {

            case STARTING:
                desc= (partCount==1) ? STATUS_STARTING_TXT : STATUS_STARTING_PART_TXT + (idx+1);
                retval= createWorkingWidget(desc);
                break;
            case WAITING:
                desc= (partCount==1) ? _monItem.getWaitingMsg() : STATUS_WAITING_PART_TXT + (idx+1);
                retval= createWorkingWidget(desc);
                break;
            case WORKING:
                _detailUI[idx].setProgressDetail(report,idx);
                retval= _detailUI[idx].getProgressDetailWidget();
                break;
            case CANCELED:
                retval= new Label(STATUS_CANCELED_TXT);
                retval.addStyleName("downloadElement-warning");
                break;
            case USER_ABORTED:
            case FAIL:
                retval= null;
                WebAssert.argTst(false,"should never happen");
                break;
            case SUCCESS:
                retval= makeSuccessWidget(idx);
                retval.addStyleName("downloadElement-download");
                break;
            default:
                retval= new Label("Unknown: " + getPartState(_monItem,idx).toString());
                retval.addStyleName("downloadElement-unknown");
                break;
        }

        return retval;

    }



    private Widget makeSuccessWidget(int idx) {
        return _monItem.makeActivationUI(idx);
    }

//    private Widget createHeaderWaitingWidget() {
//        return createWorkingWidget(STATUS_WAITING_TXT);
//    }


    private Widget createWorkingWidget(String txt) {
        Label l= new Label(txt);
        l.addStyleName("downloadElement-normal");
        l.addStyleName(_ffCss.markedText());
        return l;
    }



    private void abort() {
        if (_monItem.getReport().isDone()) {
            _content.clear();
            _content.setHeight("3px");
            BackgroundMonitor mon= Application.getInstance().getBackgroundMonitor();
            mon.removeItem(_monItem);
        }
        else if (!_aborted && !_monItem.isDone()) {
            DeferredCommand.addPause();
            DeferredCommand.addCommand(new Command() {
                public void execute() { confirmAbort(); }
            });
        }
    }

    private void confirmAbort() {
        PopupUtil.showConfirmMsg(_abortButton,
                               _prop.getTitle("abort.message")+ " " +_title,
                               _prop.getName("abort.message"),
                               new ClickHandler() {
                                   public void onClick(ClickEvent ev) { doAbort(); }
                               } );
    }


    private void doAbort() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _monItem.cancel();
                _aborted= true;
                disableIndicators();
            }
        });
    }


    private void disableIndicators() {
        _abortButton.clear();
//        Label l= new Label();
//        l.setWidth("15px");
//        l.setHeight("15px");
//        _abortButton.setWidget(l);

        IconCreator ic= IconCreator.Creator.getInstance();
        Image image= new Image(ic.getBlueDelete10x10());
        image.setPixelSize(10,10);

        _abortButton.setSize("15px", "15px");
        _abortButton.setWidget(image);
        _abortButton.setTitle(DELETE_TIP);


        _workingIcon.setVisible(false);
    }




    private FocusPanel makeAbortButton() {
        FocusPanel fp= new FocusPanel();
        Image image= new Image(GWT.getModuleBaseURL()+ "images/stop.gif");
        image.setPixelSize(15,15);
        fp.setWidget(image);
        fp.addClickHandler(new  AbortHandler());
        fp.setTitle(ABORT_TIP);
        return fp;

    }


// =====================================================================
// -------------------- Inner Classes --------------------------------
// =====================================================================



    private class AbortHandler implements ClickHandler {
        public void onClick(ClickEvent sender) { abort(); }
    }



    private static class DetailUIInfo {
        private static final String RETRIEVED_ICON=GWT.getModuleBaseURL()+ "images/blue_check-on_10x10.gif";
        private static final String PROCESSING_ICON= GWT.getModuleBaseURL()+ "images/blue-downloading-15x15.png";
        private final FlexTable _progressDetail= new FlexTable();
        private final Image _icon= new Image(PROCESSING_ICON);
        private ProgressBar _progressBar= null;
        private final FlowPanel _pbarHolder= new FlowPanel();
        private boolean _firstPbarUpdate= true;

        DetailUIInfo() {
            _progressDetail.addStyleName("downloadElement-progress");
            _pbarHolder.setWidth("200px");
            _progressDetail.setWidget(0,1,_pbarHolder);
            _progressDetail.getFlexCellFormatter().setColSpan(0,1,5);
            _icon.setVisible(false);
        }


        public void setShowRetrivedIcon(FileDownloadStatus status) {
            switch (status) {
                case DONE:
                    _icon.setVisible(true);
                    _icon.setUrl(RETRIEVED_ICON);
                    break;
                case NONE:
                    _icon.setVisible(false);
                    break;
                case WORKING:
                    _icon.setVisible(true);
                    _icon.setUrl(PROCESSING_ICON);
                    break;
                default: assert false;
                    break;
            }
        }

        public Widget getDownloadMsgWidget() { return _icon; }


        void setProgressDetail(BackgroundReport report, int idx) {

            if (_pbarHolder.isVisible()  && _pbarHolder.getAbsoluteLeft()>0)  {
                BackgroundReport.Progress progress= report.getPartProgress(idx);
                long tot= progress.getTotalByes();
                boolean progressBySize = (tot > 0);
                float percent= progressBySize ? ((float)progress.getProcessedBytes() / (float)tot) :
                        ((float)progress.getProcessedFiles() / (float)progress.getTotalFiles());
                int pInt= (int)(percent*100);
                if (_firstPbarUpdate) {
                    getProgressBar().setTextFormatter(new PText("Starting background Processing"));
                    getProgressBar().setProgress(percent);
                }
                else {
                    getProgressBar().setTextFormatter(new PText("Zipped " +pInt + "%" +" of "+
                            (progressBySize ? StringUtils.getSizeAsString(tot,true) : (progress.getTotalFiles()+" files"))));
                    getProgressBar().setProgress(percent);
                }
            }
            _firstPbarUpdate= false;
        }

        public Widget getProgressDetailWidget() { return _progressDetail; }

        public ProgressBar getProgressBar() {
            if (_progressBar==null) {
                _progressBar= new ProgressBar(0F,1F);
                _progressBar.setWidth("100%");
                _pbarHolder.add(_progressBar);

            }
            return _progressBar;
        }
    }

    private static class TablePos {
        final int _row;
        final int _col;
        public TablePos(int row, int col) {
            _row= row;
            _col= col;
        }
        public int getRow() { return _row; }
        public int getCol() { return _col; }
    }


    private static boolean isFailure(BackgroundReport report) {
        BackgroundState state= report.getState();
        return (state== BackgroundState.FAIL ||
                state== BackgroundState.USER_ABORTED ||
                state== BackgroundState.CANCELED);
    }

    private static class PText extends ProgressBar.TextFormatter {
        private final String _s;
        public PText(String s)  { _s= s; }
        protected String getText(ProgressBar bar, double curProgress) {
            return _s;
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
