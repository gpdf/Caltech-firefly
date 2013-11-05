package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/1/13
 * Time: 11:30 AM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Trey Roby
 */
public class MorePullDown {

    private static MorePullDown active= null;
    private Widget controlWidget;
    private Widget content;
    private PopupPanel pulldown= new PopupPanel();
    private HighlightLook highlightLook;
    private int offX= 0;
    private int offY= 0;

    public MorePullDown(Widget controlWidget, Widget content, HighlightLook highlightLook) {
        this.controlWidget= controlWidget;
        this.content= content;
        this.highlightLook= highlightLook;
        init();
    }


    private void init() {
        pulldown.setStyleName("front-pulldown");
        pulldown.setWidget(content);
        content.addStyleName("centerLayoutPulldown");

        pulldown.setAnimationEnabled(false);

        controlWidget.addDomHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                changeState();
            }
        }, ClickEvent.getType());
    }



    private void changeState() {
        if (pulldown.isShowing()) {
            hide();
        }
        else {
            if (active!=null) active.hide();
            show();
            active= this;
        }

    }

    private void hide() {
        pulldown.hide();
        if (highlightLook!=null) highlightLook.disable();
    }

    public void setOffset(int offX, int offY) {
        this.offX= offX;
        this.offY= offY;
    }

    private void show() {
        int y= controlWidget.getAbsoluteTop() + controlWidget.getOffsetHeight();
        pulldown.setPopupPosition(0+offX, y+offY);
        pulldown.show();
        if (highlightLook!=null) highlightLook.enable();
    }

    public static interface HighlightLook {
        public void enable();
        public void disable();
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