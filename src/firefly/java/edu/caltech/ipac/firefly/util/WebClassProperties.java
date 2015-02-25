/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.firefly.core.Application;

import java.util.Map;


/**
 * You set the base of the property in the constructor.
 * You should only use this class when you know that the properties you are
 * looking for must exist.  If any of the methods do not find a property 
 * value then they return the property string as the value.  This is very 
 * useful for debugging.  It shows you on the widget the actual string that 
 * you are searching for.
 * @author Trey Roby
 */
public class WebClassProperties {

    protected final String     _root;
    protected final String     _base;
    protected boolean    _loaded= false;
    protected String     _stringFileName= null;
    private final Map<String,String> _db;

//=========================================================================
//------------------------- Constructors -----------------------------------
//=========================================================================

    public WebClassProperties(String base, PropFile textRes, Map<String,String> pdb) {

        _db= pdb;

        if (textRes!=null) {
            Application.getInstance().getProperties().load(pdb, textRes);
        }


        if (base==null) {
            _root= "";
            _base= "";
        }
        else {
            _root= base;
            _base= base + ".";
        }
        load(); // TODO: this is all set up to turn into a lazy load-- need to evaluate
    }

    public WebClassProperties(String base) { this(base,null, null); }
    public WebClassProperties(Class c) { this(getShortName(c),null, null); }
    public WebClassProperties(Class c, PropFile textRes) { this(getShortName(c),textRes, null); }


//========================================================================
//------------------------- Public Methods --------------------------------
//=========================================================================

    private static String getShortName(Class c) {
        String parts[]= c.getName().split("\\.");
        return parts[parts.length-1];
    }


    public String getBase() {
        load();
        return _root;
    }


    public String makeBase(String s) {
        load();
        return (_base+s);
    }

    public String getProperty(String prop, String defValue) {
        load();
        WebAppProperties props= Application.getInstance().getProperties();
        return props.getProperty(_base+prop, defValue, _db);
    }


    public String getProperty(String prop) {
        load();
        return getProperty(prop, _base+prop);
    }

    public String getName() {
        load();
        return WebProp.getName(_root, _db);
    }

    public String getName(String prop) {
        load();
        return WebProp.getName(_base+prop, _db);
    }
    public String getName(String prop, String defValue) {
        load();
        return WebProp.getName(_base+prop, defValue, _db);
    }

    public String[] getNames(String prop[]) {
        load();
        return WebProp.getNames(_root, prop, _db);
    }

    public String getTip(String prop) {
        load();
        return WebProp.getTip(_base+prop, _db);
    }
    public String getTip() {
        load();
        return WebProp.getTip(_root, _db);
    }

    public String getTitle() {
        load();
        return WebProp.getTitle(_root, _db);
    }

    public String getTitle(String prop) {
        load();
        return WebProp.getTitle(_base+prop, _db);
    }

    public String getColumnName(String prop) {
        load();
        return WebProp.getColumnName(_base+prop, _db);
    }

    public boolean getSelected(String prop) {
        load();
        return WebProp.getSelected(_base+prop, _db);
    }

    public float getFloatValue(String prop) {
        load();
        return WebProp.getFloatValue(_base+prop, _db);
    }

    public int getIntValue(String prop) {
        load();
        return WebProp.getIntValue(_base+prop, _db);
    }

    public long getLongValue(String prop) {
        load();
        return WebProp.getLongValue(_base+prop, _db);
    }

    public String getDefault(String prop) {
        load();
        return WebProp.getDefault(_base+prop, _db);
    }

    public float getFloatDefault(String prop) {
        load();
        return WebProp.getFloatDefault(_base+prop, _db);
    }

    public double getDoubleDefault(String prop) {
        load();
        return WebProp.getDoubleDefault(_base+prop, _db);
    }

    public int getIntDefault(String prop) {
        load();
        return WebProp.getIntDefault(_base+prop, _db);
    }

    public long getLongDefault(String prop) {
        load();
        return WebProp.getLongDefault(_base+prop, _db);
    }


    public String getError(String prop) {
        load();
        return WebProp.getError(_base+prop, _db);
    }

    public String getErrorDescription(String prop) {
        load();
        return WebProp.getErrorDescription(_base+prop, _db);
    }

    public String getDataType(String prop) {
        load();
        return WebProp.getDataType(_base+prop, _db);
    }

    public String[] getItems() {
        load();
        return WebProp.getItems(_root, _db);
    }

    public String[] getItems(String prop) {
        load();
        return WebProp.getItems(_base+prop, _db);
    }




    String getBaseNoLoad() { return _root; }

    public void load() {
        if(!_loaded) {
            _loaded=true;
            // TODO: make an rpc call here to load properties

        }
    }


}
