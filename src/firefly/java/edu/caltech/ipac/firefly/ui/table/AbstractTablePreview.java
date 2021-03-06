/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.VisibleListener;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;

/**
 * Date: Jun 1, 2009
 *
 * @author loi
 * @version $Id: AbstractTablePreview.java,v 1.9 2011/04/27 19:33:01 roby Exp $
 */
public abstract class AbstractTablePreview extends Composite implements RequiresResize, TablePreview, VisibleListener {

    private String id= null;
    private String name;
    private String shortDesc;
    private EventHub eventHub;

    protected AbstractTablePreview() {
    }

    protected AbstractTablePreview(String name, String shortDesc) {
        this(null, name, shortDesc);
    }

    protected AbstractTablePreview(Widget display, String name, String shortDesc) {
        this.name = name;
        this.shortDesc = shortDesc;
        if (display != null) {
            setDisplay(display);
        }
    }


    protected EventHub getEventHub() {
        return eventHub;
    }

    public void bind(EventHub hub) {
        this.eventHub = hub;
        eventHub.bind(this);
    }

    public void unbind() {
        if (eventHub!=null) eventHub.unbind(this);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public String getName() {
        return name;
    }

    public Widget getDisplay() {
        return this;
    }

    public void setPreviewVisible(boolean v) {
        Widget w= getDisplay();
        if (w!=null && v!=w.isVisible()) {
            w.setVisible(v);
            if (v) onShow();
            else onHide();
        }
    }

    public void setID(String id) {
        this.id= id;
    }

    public String getID() {
        return id;
    }

    protected void setDisplay(Widget display) {
        this.initWidget(display);
    }

    public void onShow() {
        if (getDisplay() != null) {
            final TablePanel table = eventHub.getActiveTable();
            if (table != null) {
                if (table.isInit()) {
                    updateDisplay(table);
                } else {
                    table.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                        public void eventNotify(WebEvent ev) {
                            updateDisplay(table);
                            table.getEventManager().removeListener(TablePanel.ON_INIT, this);
                        }
                    });
                }
            }
        }
    }

        public void onHide() {
    }

    public boolean isInitiallyVisible() { return true; }

    abstract protected void updateDisplay(TablePanel table);


    public int getPrefHeight() { return 0; }

    public int getPrefWidth() { return 0; }

    public void onResize() {
        Widget w= getWidget();
        if (w instanceof RequiresResize) ((RequiresResize)w).onResize();
    }
}
