package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.target.TargetUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.TableConnectionList;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.VisConstants;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.NewPlotNotificationEvent;
import edu.caltech.ipac.visualize.plot.NewPlotNotificationListener;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.PlotPaintComplexListener;
import edu.caltech.ipac.visualize.plot.PlotPaintEvent;
import edu.caltech.ipac.visualize.plot.PlotViewStatusEvent;
import edu.caltech.ipac.visualize.plot.PlotViewStatusListener;
import edu.caltech.ipac.visualize.plot.WorldPt;

import javax.swing.ListSelectionModel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * This class is the data class for any set of objects that we show on
 * plots.  <i>This class need more documentation.</i>
 * 
 * @see FixedObject
 *
 * @author Trey Roby
 * @version $Id: FixedObjectGroup.java,v 1.25 2010/09/01 18:27:43 roby Exp $
 *
 */
public class FixedObjectGroup implements TableConnectionList,
                                         PropertyChangeListener,
                                         PlotViewStatusListener,
                                         PlotPaintComplexListener,
                                         Serializable,
                                         Iterable<FixedObject> {




   private final static ClassProperties _prop= new ClassProperties(
                                                    FixedObjectGroup.class);

//===================================================================
//---------- private Constants for the table column name ------------
//===================================================================
    private enum Direction {LON,LAT}

   private final String ENABLED_COL     = _prop.getColumnName("on");
   private final String HILIGHT_COL     = _prop.getColumnName("hilight");
   private final String SHAPE_COL       = _prop.getColumnName("shape");
   private final String SHOW_NAME_COL   = _prop.getColumnName("showName");
   private final String TNAME_COL       = _prop.getColumnName("targetName");
   protected final String USER_RA_COL     = _prop.getColumnName("userRa");
   protected final String USER_DEC_COL    = _prop.getColumnName("userDec");
   private final String USER_LON_COL    = _prop.getColumnName("userLon");
   private final String USER_LAT_COL    = _prop.getColumnName("userLat");
//   private final String CAT_COL         = _prop.getColumnName("cat");
//   private final String OBSERVER_COL    = _prop.getColumnName("observer");
  private static final String CAT_NAME = "CatName";
  private final static String FIXLEN = "fixlen";
  private final static String FIXLEN_VALUE = "T";
  private final static String ROWS_RETRIEVED = "RowsRetreived";

//====================================================================
//---------- public Constants for the table column index ------------
//====================================================================

    public enum ParseInstruction {NONE,SEARCH_HMS_J2000,SEARCH_HMS_B1950}

   public static final int ENABLED_IDX     = 0;
   public static final int HILIGHT_IDX     = 1;
   public static final int SHAPE_IDX       = 2;
   public static final int TNAME_IDX       = 3;
   public static final int SHOW_NAME_IDX   = 4;
   public static final int USER_RA_IDX     = 5;
   public static final int USER_DEC_IDX    = 6;

   public static final int BASE_NUM_COLUMNS= 7;

//======================================================================
//---------- public Constants for property change events ---------------
//======================================================================

    static public final String BULK_UPDATE    = "bulkUpdate";
    static public final String SELECTED_COUNT = "selectedCount";
    static public final String ADD            = "add";
    static public final String REMOVE         = "remove";
//====================================================================
//---------- constants for the type of color we can set --------------
//====================================================================
   public static final int COLOR_TYPE_HIGHLIGHT= 45;
   public static final int COLOR_TYPE_STANDARD = 47;
   public static final int COLOR_TYPE_SELECTED = 48;


//====================================================================
//---------- defaults for imports - package accesss ------------------
//====================================================================

    protected final static String DEFAULT_TNAME_OPTIONS[]= {
                   ".*name.*",         // generic
                   ".*pscname.*",      // IRAS
                   ".*target.*",       // our own table output
                   ".*designation.*",  // 2MASS, WISE
                   ".*objid.*",        // SPITZER
                   ".*starid.*"        // PCRS
    };
    protected final static String DEFAULT_RA_NAME_OPTIONS[]= {".*ra.*"};
    protected final static String DEFAULT_DEC_NAME_OPTIONS[]= {".*dec.*"};

//======================================================================
//----------------------- Private / Protected variables ----------------
//======================================================================

    private static NumberFormat   _nf= NumberFormat.getInstance();// OK for i18n
    private boolean      _usesWorldCoordSys;
   private int            _numColumns;
   private String         _colNames[];
   private String         _title;
   private FixedObject    _current;
   private ArrayList<FixedObject> _objects= new ArrayList<FixedObject>(200);
   private int            _selectedCount;
   private boolean        _doingBulkUpdates  = false;
   private boolean        _targetNameEditable= false;
   private DataGroup      _extraData;
   private transient List<PlotInfo> _plots= null;
   private transient SkyShape _allShape;
   private transient PropertyChangeSupport _propChange;
   private transient List<FixedObjectGroupDataListener> _dataListeners;
   private boolean        _showPosInDecimal= AppProperties.getBooleanPreference(
                                     VisConstants.COORD_DEC_PROP, false);
   private String         _csysDesc= AppProperties.getPreference(
                                           VisConstants.COORD_SYS_PROP,
                                           CoordinateSys.EQ_J2000_STR);
   private Color        _allHighLightColor= Color.blue;
   private Color        _allSelectedColor = Color.orange;
   private Color        _allStandardColor = Color.red;

   private int         _extraDataColumnRemap[]= null;

    static {
        _nf.setMaximumFractionDigits(5);
        _nf.setMinimumFractionDigits(5);
    }

   public FixedObjectGroup() {
       this(true,null,null);
   }

    public FixedObjectGroup(boolean usesWorldCoordSys) {
        this(usesWorldCoordSys,null,null);
    }

    public FixedObjectGroup(String title, DataGroup extraData) {
        this(true,title,extraData);
    }

   public FixedObjectGroup(boolean usesWorldCoordSys, String title, DataGroup extraData) {
       init(title,usesWorldCoordSys,extraData,null);
   }

    public FixedObjectGroup(DataGroup dataGroup) throws ColumnException {
        this(dataGroup,null,null,null);
    }


    public FixedObjectGroup (DataGroup dataGroup,
                             int  tnameIdx,
                             int  raIdx,
                             int  decIdx) throws NumberFormatException,
                                                 IllegalArgumentException {
        constructHelper(dataGroup, tnameIdx, raIdx, decIdx);
    }

    public FixedObjectGroup (DataGroup dataGroup,
                             String  targetNameOptions[],
                             String  raNameOptions[],
                             String  decNameOptions[])
                                  throws NumberFormatException,
                                         IllegalArgumentException,
                                         ColumnException {
        this(dataGroup,targetNameOptions,makeParseGroupList(raNameOptions,decNameOptions));
    }

    private static List<ParseGroup> makeParseGroupList(String  raNameOptions[],
                                                       String  decNameOptions[]) {
        List<ParseGroup> pgList= new ArrayList<ParseGroup>(1);
        if (raNameOptions==null) {
            raNameOptions= DEFAULT_RA_NAME_OPTIONS;
        }
        if (decNameOptions==null) {
            decNameOptions= DEFAULT_DEC_NAME_OPTIONS;
        }
        pgList.add(new ParseGroup(raNameOptions, decNameOptions,
                                      ParseInstruction.NONE));
        return pgList;
    }


    public FixedObjectGroup (DataGroup dataGroup,
                             String  targetNameOptions[],
                             List<ParseGroup> passedPGList )
                                          throws NumberFormatException,
                                                 IllegalArgumentException,
                                                  ColumnException {

        List<ParseGroup> pgList;

        if (passedPGList!=null) {
            pgList= passedPGList;
        }
        else {
            pgList= new ArrayList<ParseGroup>(1);
            pgList.add(new ParseGroup(DEFAULT_RA_NAME_OPTIONS,
                                      DEFAULT_DEC_NAME_OPTIONS,
                                      ParseInstruction.NONE));
        }

        if (targetNameOptions==null) {
            targetNameOptions= DEFAULT_TNAME_OPTIONS;
        }

        int tnameIdx= -1;
        int raIdx= -1;
        int decIdx= -1;
        DataType[] originalDataDef= dataGroup.getDataDefinitions();


        for(int i=0; i<originalDataDef.length; i++) {
            Class classType= originalDataDef[i].getDataType();
            String key= originalDataDef[i].getKeyName();
            String lKey= null;
            if (key!=null) lKey= key.toLowerCase();

            if (tnameIdx == -1 &&
                (classType==String.class) &&
                matchesList(lKey,targetNameOptions)) {
                tnameIdx= i;
            }
            else if (raIdx == -1 && (classType==Double.class || classType==Float.class)) {
                ParseInstruction pi= matchesRAList(lKey,pgList);
                if (pi!=null) raIdx= i;
            }
            else if (decIdx == -1 && (classType==Double.class || classType==Float.class)) {
                ParseInstruction pi= matchesDecList(lKey,pgList);
                if (pi!=null) decIdx= i;
            }
        }

        if (raIdx==-1 && decIdx==-1 && passedPGList!=null) {
            addHMSColumnsIfPosible(pgList, dataGroup);
            originalDataDef= dataGroup.getDataDefinitions();
            for(int i=0; i<originalDataDef.length; i++) {
                String key= originalDataDef[i].getKeyName();

                if (key.equals("_ra")) raIdx= i;
                else  if (key.equals("_dec")) decIdx= i;


            }

        }

        if (raIdx == -1) {
            throw new ColumnException(
                            "Could not convert to FixedObjectGroup.  "+
                            "There is not a field that can be converted to a RA",
                            ColumnException.ColumnType.RA,
                            tnameIdx, raIdx, decIdx);
        }
        if (decIdx == -1) {
            throw new ColumnException(
                         "Could not convert to FixedObjectGroup.  "+
                         "There is not a field that can be converted to a Dec",
                         ColumnException.ColumnType.DEC,
                         tnameIdx, raIdx, decIdx);
        }

        constructHelper(dataGroup, tnameIdx, raIdx, decIdx);
    }


    void addHMSColumnsIfPosible(List<ParseGroup> pgList,
                                DataGroup dataGroup)  throws  ColumnException {
        DataType[] originalDataDef= dataGroup.getDataDefinitions();
        int raIdx= -1;
        int decIdx= -1;
        int len= dataGroup.size();
        DataType raDataType= new DataType("_ra","RA", Double.class);
        DataType decDataType= new DataType("_dec","Dec", Double.class);
        ParseInstruction parseInstruction= ParseInstruction.SEARCH_HMS_J2000;

        CoordinateSys convertTarget= CoordinateSys.EQ_J2000;
        for(int i=0; i<originalDataDef.length; i++) {
            Class classType= originalDataDef[i].getDataType();
            String key= originalDataDef[i].getKeyName();

            if (raIdx == -1 && (classType==String.class)) {
                ParseInstruction pi= matchesRAList(key,pgList);
                if (pi!=null) {
                    try {
                        TargetUtil.convertStringToLon((String)dataGroup.get(0).getDataElement(originalDataDef[i]),
                                                      convertTarget);
                        dataGroup.addDataDefinition(raDataType);
                        double lon;
                        for(int j= 0; (j<len); j++) {
                            lon= TargetUtil.convertStringToLon((String)dataGroup.get(j).getDataElement(originalDataDef[i]),
                                                               convertTarget);
                            dataGroup.get(j).setDataElement(raDataType,lon);
                        }
                        raIdx= i;
                        parseInstruction= pi;
                    } catch (CoordException e) {
                        // not nothing - not found
                    }
                }
            }
            else if (decIdx == -1 && (classType==String.class)) {
                ParseInstruction pi= matchesDecList(key,pgList);
                if (pi!=null) {

                    try {
                        TargetUtil.convertStringToLat((String)dataGroup.get(0).getDataElement(originalDataDef[i]),
                                                      CoordinateSys.EQ_J2000);
                        dataGroup.addDataDefinition(decDataType);
                        double lat;
                        for(int j= 0; (j<len); j++) {
                            lat= TargetUtil.convertStringToLat((String)dataGroup.get(j).getDataElement(originalDataDef[i]),
                                                               CoordinateSys.EQ_J2000);
                            dataGroup.get(j).setDataElement(decDataType,lat);
                        }

                        decIdx= i;
                        parseInstruction= pi;
                    } catch (CoordException e) {
                        // not nothing - not found
                    }
                }
            }
        }
        if (raIdx == -1) {
            throw new ColumnException( "Could not convert to FixedObjectGroup.  "+
                                       "There is not a field that can be converted to a RA",
                                       ColumnException.ColumnType.RA,
                                       -1, raIdx, decIdx);
        }
        if (decIdx == -1) {
            throw new ColumnException( "Could not convert to FixedObjectGroup.  "+
                                       "There is not a field that can be converted to a Dec",
                                       ColumnException.ColumnType.DEC,
                                       -1, raIdx, decIdx);
        }



        if (parseInstruction==ParseInstruction.SEARCH_HMS_B1950) {
            DataObject data;
            for(int j= 0; (j<len); j++) {
                data= dataGroup.get(j);
                double ra= (Double)data.getDataElement(raDataType);
                double dec= (Double)data.getDataElement(decDataType);
                WorldPt wp= new WorldPt(ra,dec,CoordinateSys.EQ_B1950);
//                Position p= new Position(ra,dec,
//                                edu.caltech.ipac.astro.target.CoordinateSys.EQ_B1950);
                wp= Plot.convert(wp,convertTarget);
                data.setDataElement(raDataType, wp.getLon());
                data.setDataElement(decDataType, wp.getLat());
            }
        }

    }


    private void constructHelper(DataGroup dataGroup,
                                 int tnameIdx,
                                 int raIdx,
                                 int decIdx) throws NumberFormatException {

        DataType[] originalDataDef= dataGroup.getDataDefinitions();

        int outIdx=0;
        int extraDataRemapAry[]= new int[originalDataDef.length];
        Arrays.fill(extraDataRemapAry,-1);

        for(int i=0; i<originalDataDef.length; i++) {
            if (i == tnameIdx || i== raIdx || i==decIdx) {
                originalDataDef[i].setImportance(DataType.Importance.IGNORE);
            }
            else {
                extraDataRemapAry[outIdx++]= i;
            }
        }

        init (dataGroup.getTitle(), true, dataGroup, extraDataRemapAry);

        Iterator i= dataGroup.iterator();
        DataObject element;
        FixedObject fixedObj;
        beginBulkUpdate();
        _objects.ensureCapacity(dataGroup.size());
        while(i.hasNext()) {
            element= (DataObject)i.next();
            fixedObj= makeFixedObject(element, tnameIdx, raIdx, decIdx);
            add(fixedObj);
        }
        endBulkUpdate();
    }


    DataType getExtraDataElement(int i) {
        int remap= i;
        if (_extraDataColumnRemap!=null)  {
            remap= _extraDataColumnRemap[i];
        }
        DataType  retval= null;
        if (remap>-1) retval= _extraData.getDataDefinitions()[remap];
        return retval;
    }

    public int getExtraDataLength() {
        int length= 0;
        DataType defs[]= _extraData.getDataDefinitions();
        if (defs!=null) {
            length= getExtraUsedLength();
        }
        return length;
    }

    public String getTitle() { return _title;  }
    public void setTitle(String title) { _title= title;  }
    public void setColumnName(int columnIdx, String name) {
       Assert.tst(columnIdx < _numColumns);
       _colNames[columnIdx]= name;
    }

    public void setTargetNameEditable(boolean editable) {
       _targetNameEditable= editable;
    }
    public boolean isTargetNameEditable() {return _targetNameEditable; }

    public FixedObject findObjectWithTargetName(String name) {
         FixedObject retval= null;
         for(FixedObject  fixedObj: _objects) {
             if (name.equals(fixedObj.getTargetName())) {
                  retval= fixedObj;
                  break;
             }
         }
         return retval;
    }

    /**
     * Return an iterator for all the objects in this group
     * @return Iterator  the iterator
     */
    public Iterator<FixedObject> iterator() {
        return _objects.iterator();
    }

    public boolean isWorldCoordSys() { return _usesWorldCoordSys; }

    public void beginBulkUpdate() { _doingBulkUpdates = true; }

    public void endBulkUpdate()   {
        if (_doingBulkUpdates) {
            getPropChange().firePropertyChange ( BULK_UPDATE, null, this);
            doRepair();
        }
        _doingBulkUpdates= false;
    }

    /**
     * Set the current object
     * @param current  the new current object
     */
    public void setCurrent(FixedObject current) {
        _current= current;
    }

    /**
     * Return the current object
     * @return FixedObject  the current object
     */
    public FixedObject getCurrent() { return _current; }

    /**
     * Return the extra data types defined for this group.
     * @return DataType[]  the extra data types.
     */
    public DataType[] getExtraDataDefs() {
        DataType[] retval= null;
        if (_extraData!=null) {
            int usedLength= getExtraUsedLength();
            if (usedLength>0) {
                if (_extraDataColumnRemap!=null) {
                    retval= new DataType[usedLength];
                    for(int i=0; i<retval.length; i++) {
                        retval[i]= getExtraDataElement(i);
                    }
                }
                else {
                    retval= _extraData.getDataDefinitions();
                }
            }
            else {
               retval= null;
            }

        }
        return  retval;
    }

    public DataGroup getExtraData() { return _extraData; }

    public void setAllEnabled(boolean enable) {
        beginBulkUpdate();
        for (FixedObject  fixedObj: _objects) {
             fixedObj.setEnabled(enable);
        }
        endBulkUpdate();
    }

    public void setAllNamesEnabled(boolean enable) {
         beginBulkUpdate();
         for (FixedObject  fixedObj: _objects) {
             fixedObj.setShowName(enable);
         }
         endBulkUpdate();
    }

    public void setAllShapes(SkyShape shape) {
         _allShape= shape;
         if (size() > 0) {
            beginBulkUpdate();
            for (FixedObject  fixedObj: _objects) {
                fixedObj.getDrawer().setSkyShape(shape);
            }
            endBulkUpdate();
         }
    }

    public SkyShape getAllShapes() {
        if (_allShape==null) {
            _allShape= SkyShapeFactory.getInstance().getSkyShape("x");
        }
         return _allShape;
    }

    /**
     * Set the color for all the objects.
     * The are three type of colors highlight color, standard color,
     * and selected color.
     * @param colorType the color type.  Must the on of the constands:
     *    <code>COLOR_TYPE_HIGHLIGHT</code>,
     *    <code>COLOR_TYPE_STANDARD</code>,
     *    <code>COLOR_TYPE_SELECTED</code>
     * @param c the color to set
     */
    public void setAllColor(int colorType, Color c) {
          Assert.tst(colorType == COLOR_TYPE_HIGHLIGHT ||
                     colorType == COLOR_TYPE_STANDARD   ||
                     colorType == COLOR_TYPE_SELECTED);
         beginBulkUpdate();
         switch (colorType) {
               case COLOR_TYPE_HIGHLIGHT : _allHighLightColor= c; break;
               case COLOR_TYPE_STANDARD :  _allStandardColor = c; break;
               case COLOR_TYPE_SELECTED :  _allSelectedColor = c; break;
               default :                   Assert.tst(false);     break;
         } // end switch
         for (FixedObject  fixedObj: _objects) {
             switch (colorType) {
                  case COLOR_TYPE_HIGHLIGHT :
                                fixedObj.getDrawer().setHighLightColor(c);
                                break;
                  case COLOR_TYPE_STANDARD :
                                fixedObj.getDrawer().setStandardColor(c);
                                break;
                  case COLOR_TYPE_SELECTED :
                                fixedObj.getDrawer().setSelectedColor(c);
                                break;
                  default :
                                Assert.tst(false);
                                break;
             } // end switch
         } // end loop
         endBulkUpdate();
    }

    public Color getAllColor(int colorType) {
       Assert.tst(colorType == COLOR_TYPE_HIGHLIGHT ||
                  colorType == COLOR_TYPE_STANDARD   ||
                  colorType == COLOR_TYPE_SELECTED);
       Color retval= Color.red;
       switch (colorType) {
          case COLOR_TYPE_HIGHLIGHT: retval= _allHighLightColor; break;
          case COLOR_TYPE_STANDARD : retval= _allStandardColor;  break;
          case COLOR_TYPE_SELECTED : retval= _allSelectedColor;  break;
          default                  : Assert.tst(false);          break;
       } // end switch
       return retval;
    }

    public void add(FixedObject s) {
       _objects.add(s);
       s.addPropertyChangeListener(this);
       CoordinateSys csys= CoordinateSys.parse(_csysDesc);
       s.setCoordinateSys(csys);
       computeAllTransformsForObject(s);
       if (!_doingBulkUpdates) {
           getPropChange().firePropertyChange ( ADD, null, this);
       }
    }

    public void remove(FixedObject s) {
        Assert.tst(_objects.contains(s));
        s.removePropertyChangeListener(this);

        FixedObject newFo= null;
        int line= indexOf(s);
        if ( (line+1) < size()) newFo= get(line+1);

        _objects.remove(s);
        if (!_doingBulkUpdates) {
            getPropChange().firePropertyChange ( REMOVE, s, newFo);
        }
    }

    public void clear() {
       FixedObject s;
       for(Iterator<FixedObject> i= _objects.iterator(); (i.hasNext()); ) {
          s= i.next();
          i.remove();
          s.removePropertyChangeListener(this);
       }
       getPropChange().firePropertyChange ( ALL_ENTRIES_UPDATED, null, this);
    }



    public double minRa() {
        FixedObject fo= Collections.min(_objects, raComparator());
        return fo.getX();
    }

    public double minDec() {
        FixedObject fo= Collections.min(_objects, decComparator());
        return fo.getY();
    }

    public double maxRa() {
        FixedObject fo= Collections.max(_objects, raComparator());
        return fo.getX();
    }

    public double maxDec() {
        FixedObject fo= Collections.max(_objects, decComparator());
        return fo.getY();
    }

    public void setShowing( Plot plot, boolean show) {
       PlotInfo pInfo= findPlotInfo(plot);
       boolean oldShow= pInfo._show;
       pInfo._show= show;
       if (oldShow != show) plot.repair();
    }

    public void doRepair() {
        Plot p;
        for(PlotInfo plotInfo: getPlots()) {
             p= plotInfo._p;
             p.repair();
        }
    }

    public void doRepair(FixedObject fixedObj) {
        Rectangle r;
        Plot p;
        for(PlotInfo plotInfo: getPlots()) {
             p= plotInfo._p;
             r= fixedObj.getDrawer().computeRepair( p.getTransform(),
                                     getPlots().indexOf(plotInfo) );
             if (r != null) p.repair(r);
        }
    }

    public void drawOnPlot(Plot p, Graphics2D g2) {
       int idx= findPlot(p);
       PlotInfo pInfo= getPlots().get(idx);
       if (pInfo._show) {
           //System.out.println("drawOnPlot");
           for(FixedObject fixedObj: _objects) {
               if (fixedObj.isEnabled())
                      fixedObj.getDrawer().drawOnPlot(idx, g2);
           }
       }
    }



    public ClosestResults findClosestPoint(ImagePt pt, Plot p) {
          int idx= findPlot(p);
          double dx, dy;
          double dist;
          double minDist= Double.MAX_VALUE;
          ClosestResults retval= new ClosestResults();


          ImagePt testPt= null;
          FixedObject foundPt= null;
          for(FixedObject fixedObj: _objects) {
              testPt= fixedObj.getDrawer().getImagePt(idx);
              if (testPt != null) {
                 dx= pt.getX() - testPt.getX();
                 dy= pt.getY() - testPt.getY();
                 dist= Math.sqrt(dx*dx + dy*dy);
                 //System.out.println("comparing: " + dist +" & " + minDist);
                 if (dist < minDist) {
                        minDist= dist;
                        foundPt= fixedObj;
                 }
              }
          }
          retval._pt= foundPt;
          retval._dist= minDist;
          retval._group= this;
          return retval;
    }

    public void addPlotView(PlotContainer container) {
       for(Plot p: container) addPlot(p);
       container.addPlotViewStatusListener( this);
       container.addPlotPaintListener(this);
    }

    public void removePlotView(PlotContainer container) {
       for(Plot p: container) removePlot(p);
       container.removePlotViewStatusListener( this);
       container.removePlotPaintListener(this);
    }



    public int getSelectedCount() {
          return _selectedCount;
    }

    public FixedObject get(int i) { return (FixedObject)_objects.get(i); }

    public void copySelectionFromModel(ListSelectionModel model) {
       int         len= size();
       FixedObject o;
       Integer     oldCount= new Integer(_selectedCount);
       _selectedCount= 0;
       for(int i=0; (i<len); i++) {
           o= get(i);
           o.setSelectedWithNoEvent( model.isSelectedIndex(i) );
           if (model.isSelectedIndex(i)) _selectedCount++;
       } // end loop
       getPropChange().firePropertyChange (SELECTED_COUNT, oldCount,
                                       new Integer(_selectedCount) );
       doRepair();
    }


    public DataGroup convertToDataGroupForSaving() {
        int length= (_extraData!=null) ? getExtraUsedLength()+3 : 3;
        DataType newDataDef[]= new DataType[length];
        newDataDef[0]= new DataType("target",TNAME_COL,String.class,
                                    DataType.Importance.HIGH,"",true);
        newDataDef[1]= new DataType("ra",USER_RA_COL,Double.class,
                                    DataType.Importance.HIGH,"degrees",true);
        newDataDef[2]= new DataType("dec",USER_DEC_COL,Double.class,
                                    DataType.Importance.HIGH,"degrees",true);

        for(int i=3; i<newDataDef.length; i++) {
            newDataDef[i]= getExtraDataElement(i-3).copyWithNoColumnIdx(i);
        }

        DataGroup   retval= new DataGroup(getTitle(), newDataDef);
        DataObject  dataObject;
        for(FixedObject fixedObj: _objects) {
            WorldPt pt = fixedObj.getEqJ2000Position();
            dataObject= new DataObject(retval);
            dataObject.setDataElement(newDataDef[0], fixedObj.getTargetName());
            dataObject.setDataElement(newDataDef[1], new Double(pt.getLon()));
            dataObject.setDataElement(newDataDef[2], new Double(pt.getLat()));

            for(int j=3; j<newDataDef.length; j++) {
                dataObject.setDataElement(newDataDef[j],
                                          fixedObj.getExtraData(getExtraDataElement(j-3)));
            }
            retval.add(dataObject);
        }

        // adding metadata for this data group
        retval.addAttributes( new DataGroup.Attribute(CAT_NAME, getTitle()) );
        retval.addAttributes( new DataGroup.Attribute(FIXLEN, FIXLEN_VALUE) );
        retval.addAttributes( new DataGroup.Attribute(
                        ROWS_RETRIEVED, String.valueOf(retval.size())) );
        return retval;
    }


//=====================================================================
//----------- Add / Remove Listener Methods ---------------------------
//=====================================================================

    /**
     * Add a property changed listener.
     * @param p  the listener
     */
    public void addPropertyChangeListener (PropertyChangeListener p) {
       getPropChange().addPropertyChangeListener (p);
    }

    /**
     * Remove a property changed listener.
     * @param p  the listener
     */
    public void removePropertyChangeListener (PropertyChangeListener p) {
       getPropChange().removePropertyChangeListener (p);
    }

  /**
   * Add a FixedObjectGroupDataListener.
   * @param l the listener
   */
   public void addFixedObjectGroupDataListener(FixedObjectGroupDataListener l) {
      getDataListeners().add(l);
   }
  /**
   * Remove a FixedObjectGroupDataListener.
   * @param l the listener
   */
   public void removeFixedObjectGroupDataListener(
                                  FixedObjectGroupDataListener l) {
      getDataListeners().remove(l);
   }

//======================================================================
//------------- Methods from PropertyChangeListener Interface ----------
//======================================================================

    public void propertyChange(PropertyChangeEvent ev) {
       String propName= ev.getPropertyName();
       if (!_doingBulkUpdates) {
           if (propName.equals(FixedObject.SELECTED)) {
               updateSelectedCount(
                              ((Boolean)ev.getNewValue()).booleanValue());
               FixedObject fixedObj= (FixedObject)ev.getSource();
               if (!_doingBulkUpdates) doRepair(fixedObj);
           }
           else if (propName.equals(FixedObject.POSITION)) {
               FixedObject fixedObj= (FixedObject)ev.getSource();
               computeAllTransformsForObject(fixedObj);
               int idx= _objects.indexOf(fixedObj);
               Assert.tst(idx >= 0);
               fireFixedChange(fixedObj, idx );
               if (!_doingBulkUpdates) doRepair(fixedObj);
           }
           else if (propName.equals(FixedObject.SHOW_NAME)) {
               FixedObject fixedObj= (FixedObject)ev.getSource();
               if (!_doingBulkUpdates && fixedObj.isEnabled()) {
                   doRepair(fixedObj);
                   getPropChange().firePropertyChange ( ENTRY_UPDATED, null,
                                                    fixedObj);
               }
           }
           else if (propName.equals(FixedObject.ENABLED)) {
               FixedObject fixedObj= (FixedObject)ev.getSource();
               doRepair(fixedObj);
               getPropChange().firePropertyChange ( ENTRY_UPDATED, null,
                                                fixedObj);
           }
       }
                              //=========================

       if (propName.equals(VisConstants.COORD_DEC_PROP)) {
            _showPosInDecimal= AppProperties.getBooleanPreference(
                               VisConstants.COORD_DEC_PROP,false);

           getPropChange().firePropertyChange ( ALL_ENTRIES_UPDATED, null, this);
       }
       else if (propName.equals(VisConstants.COORD_SYS_PROP)) {
            _csysDesc = AppProperties.getPreference(
                                  VisConstants.COORD_SYS_PROP,
                                  CoordinateSys.EQ_J2000_STR);
            updateCoordinateSystem();
           getPropChange().firePropertyChange ( ALL_ENTRIES_UPDATED, null, this);
       }
    }

    public int indexOf(FixedObject fixedObj) {
        return indexOf((Object)fixedObj);
    }

    public int indexOf(Object object) {
        return _objects.indexOf((FixedObject)object);
    }

    public int size() { return _objects.size(); }
    public int getColumnCount() { return _numColumns; } //TODO: remove

    public String getColumnName(int idx) { return _colNames[idx]; } //TODO: remove


    public List<FixedObject> createSortedView(final String extraDataKey) {
        ArrayList<FixedObject> _sortedView= new ArrayList<FixedObject>(
                                                          _objects);
        Collections.sort(_sortedView, new Comparator<FixedObject>() {
            public int compare(FixedObject o1, FixedObject o2) {
                return ComparisonUtil.doCompare(
                                 (Number)o1.getExtraData(extraDataKey),
                                 (Number)o2.getExtraData(extraDataKey));
            }
        });
        return Collections.unmodifiableList(_sortedView);
    }
   // ===================================================================
   // --------  Methods from PlotViewStatusListener Interface-----------
   // ===================================================================
    public void plotAdded(PlotViewStatusEvent ev) {
         addPlot(ev.getPlot());
    }
    public void plotRemoved(PlotViewStatusEvent ev) {
         removePlot(ev.getPlot());
    }

   // ===================================================================
   // ---------  Methods from PlotPaintListener Interface---------------
   // ===================================================================

    public void paint(PlotPaintEvent ev) {
         drawOnPlot( ev.getPlot(), ev.getGraphics() );
    }
//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void addPlot(Plot p) {
        NewPlotNotificationListener psl=
              new NewPlotNotificationListener() {
                  public void newPlot(NewPlotNotificationEvent e) {
                        computeImageTransform(e.getPlot());
                  }
              };
        p.addPlotStatusListener(psl);
        getPlots().add(new PlotInfo(p, psl) ); // needs to add to empty slot
        if (p.isPlotted()) computeImageTransform(p);
    }

    private void removePlot(Plot p) {
       Iterator<PlotInfo> i= getPlots().iterator();
       boolean found= false;
       PlotInfo plotInfo;
       for(; (i.hasNext() && !found); ) {
           plotInfo= i.next();
           if (p==plotInfo._p) {
               found= true;
               p.removePlotStatusListener(plotInfo._psl);
               i.remove();
           }
       }
    }



    private void updateSelectedCount(boolean newChange) {
         // this method also needs to work with bulk updates
         Integer oldCount= new Integer(_selectedCount);
         _selectedCount= newChange ? (_selectedCount+1) : (_selectedCount-1);
         getPropChange().firePropertyChange (SELECTED_COUNT, oldCount,
                                         new Integer(_selectedCount) );
    }

    private void updateCoordinateSystem() {
        CoordinateSys csys= CoordinateSys.parse(_csysDesc);
        Assert.tst(csys);
        for(FixedObject fixedObj: _objects) {
            fixedObj.setCoordinateSys(csys);
        }
        updateTitles();
    }

    private void updateTitles() {
       if (_csysDesc.equals(CoordinateSys.GALACTIC_STR) ||
           _csysDesc.equals(CoordinateSys.SUPERGALACTIC_STR) ) {
              _colNames[USER_RA_IDX]   = USER_LON_COL;
              _colNames[USER_DEC_IDX]  = USER_LAT_COL;
       }
       else {
              _colNames[USER_RA_IDX]   = USER_RA_COL;
              _colNames[USER_DEC_IDX]  = USER_DEC_COL;
       }
    }

    String getFormatedLon(WorldPt pt) {
        return formatPos(pt.getLon(), Direction.LON, pt.getCoordSys().isEquatorial());
    }

    String getFormatedLat(WorldPt pt) {
        return formatPos(pt.getLat(), Direction.LAT, pt.getCoordSys().isEquatorial());
    }

    private String formatPos(double x, Direction dir, boolean isEquatorial) {
       String retval= null;
       if (_showPosInDecimal) {
           retval= _nf.format(x);
       }
       else {
          try {
               if (dir==Direction.LAT) {
                   retval= TargetUtil.convertLatToString(x, isEquatorial);
               }
               else if (dir==Direction.LON) {
                   retval= TargetUtil.convertLonToString(x, isEquatorial);
               }
              else {
                   Assert.tst(false);
               }
          } catch (CoordException ce) {
               retval= "";
          }
       }
       return retval;
    }

    private PlotInfo findPlotInfo(Plot p) {
       int idx= findPlot(p);
       return getPlots().get(idx);
    }


    private int findPlot(Plot p) {
       int retval= -1;
       Iterator<PlotInfo> i= getPlots().iterator();
       boolean found= false;
       PlotInfo plotInfo= null;
       PlotInfo retPlotInfo= null;
       for(; (i.hasNext() && !found); ) {
           plotInfo= i.next();
           if (p==plotInfo._p) {
               found= true;
               retPlotInfo= plotInfo;
           }
       }
       if (found) retval= getPlots().indexOf(retPlotInfo);
       //System.out.println("findPlot: found= " + found + "  retval= "+ retval);
       return retval;
    }

    protected void computeAllTransformsForObject(FixedObject fixedObj) {
          int length= getPlots().size();
          PlotInfo plotInfo;
          List<PlotInfo> plots= getPlots();
          for(int i=0; (i<length);i++ ) {
              plotInfo= plots.get(i);
              fixedObj.getDrawer().computeTransform(i, plotInfo._p);
          }
    }

    protected void computeImageTransform(Plot p) {
          int idx= findPlot(p);
          for (FixedObject  fixedObj: _objects) {
              fixedObj.getDrawer().computeTransform(idx, p);
          }
    }

    /**
     * fire the <code>VectorDataListener</code>s.
     */
    protected void fireFixedChange(FixedObject fo, int idx) {
        List<FixedObjectGroupDataListener> newlist;
        FixedObjectGroupDataEvent ev=
                       new FixedObjectGroupDataEvent(this, fo, idx);
        synchronized (this) {
            newlist =
               new ArrayList<FixedObjectGroupDataListener>(getDataListeners());
        }

        for(FixedObjectGroupDataListener listener: newlist) {
            listener.dataChanged(ev);
        }
    }

    private static ParseInstruction matchesRAList(String s, List<ParseGroup> pgList) {
        ParseInstruction pi= null;
        for(ParseGroup pg : pgList) {
            if (StringUtil.matchesRegExpList(s,pg.getRaNameOptions(), true)) {
                pi= pg.getParseInstruction();
            }
        }
        return pi;
    }

    private static ParseInstruction matchesDecList(String s, List<ParseGroup> pgList) {
        ParseInstruction pi= null;
        for(ParseGroup pg : pgList) {
            if (StringUtil.matchesRegExpList(s,pg.getDecNameOptions(), true)) {
                pi= pg.getParseInstruction();
            }
        }
        return pi;
    }

    private static boolean matchesList(String s, String regExpArray[]) {
        return StringUtil.matchesRegExpList(s,regExpArray,true);
    }



    private void init(String title,
                      boolean   usesWorldCoordSys,
                      DataGroup extraData,
                      int      extraDataColumnRemap[]) {
        _title           = title;
        _extraDataColumnRemap= extraDataColumnRemap;
        _extraData= extraData;
        _usesWorldCoordSys= usesWorldCoordSys;
        AppProperties.addPropertyChangeListener(this);
        if (extraData != null) {
            _numColumns= BASE_NUM_COLUMNS + getExtraUsedLength();
        }
        else {
            _numColumns = BASE_NUM_COLUMNS;
        }
        _colNames= new String[_numColumns];
        initColumnTitles();
        updateTitles();

        if (_extraData != null) {
            int realLength= getExtraUsedLength();
            for(int i= 0; (i<realLength); i++) {
                _colNames[BASE_NUM_COLUMNS+i]=
                               getExtraDataElement(i).getDefaultTitle();
            }
        }
    }

    private int getExtraUsedLength() {
        int realLength= 0;
        if (_extraDataColumnRemap==null) {
            realLength= _extraData.getDataDefinitions().length;
        }
        else {
            for(int i=0; i<_extraDataColumnRemap.length; i++) {
                if (_extraDataColumnRemap[i]!=-1) realLength++;
            }
        }
        return realLength;
    }

    private void initColumnTitles() {
        _colNames[ENABLED_IDX]   = ENABLED_COL;
        _colNames[HILIGHT_IDX]   = HILIGHT_COL;
        _colNames[SHOW_NAME_IDX] = SHOW_NAME_COL;
        _colNames[SHAPE_IDX]     = SHAPE_COL;
        _colNames[TNAME_IDX]     = TNAME_COL;
    }

    protected PropertyChangeSupport getPropChange() {
        if (_propChange==null)  {
            _propChange= new PropertyChangeSupport(this);
        }
        return _propChange;
    }

    protected List<FixedObjectGroupDataListener> getDataListeners() {
        if (_dataListeners==null)  {
            _dataListeners= new ArrayList<FixedObjectGroupDataListener>(5);
        }
        return _dataListeners;
    }


    protected List<PlotInfo> getPlots() {
        if (_plots==null)  {
            _plots= new ArrayList<PlotInfo>(20);
        }
        return _plots;
    }


//===================================================================
//------------------------- Factory Methods -------------------------
//===================================================================

    //protected List newList() { return new ArrayList(200); }
    protected List newListenerList()  { return new Vector(2,2); }


    protected FixedObject makeFixedObject(DataObject da,
                                          int tnameIdx,
                                          int raIdx,
                                          int decIdx)
                                       throws NumberFormatException {
        return new FixedObject(da, tnameIdx, raIdx, decIdx);
    }

    public FixedObject makeFixedObject(WorldPt pt) {
        return new FixedObject(pt,_extraData);
    }

    public FixedObject makeFixedObject(ImagePt pt) {
        return new FixedObject(pt,_extraData);
    }

    public FixedObject makeFixedObject(ImageWorkSpacePt iwspt) {
	ImagePt pt = new ImagePt(iwspt.getX(), iwspt.getY());
        return new FixedObject(pt,_extraData);
    }

//===================================================================
//------------------------- Public Inner classes --------------------
//===================================================================

    public static class ClosestResults {
        public FixedObject      _pt;
        public double           _dist;
        public FixedObjectGroup _group;
    }

    public static class ParseGroup {
        private final String  _raNameOptions[];
        private final String  _decNameOptions[];
        private final FixedObjectGroup.ParseInstruction _parseInstruction;

        public ParseGroup(String  raNameOptions[],
                      String  decNameOptions[],
                      FixedObjectGroup.ParseInstruction parseInstruction ) {
            _raNameOptions= raNameOptions;
            _decNameOptions= decNameOptions;
            _parseInstruction= parseInstruction ;
        }

        public String[] getRaNameOptions() {
            return  _raNameOptions;
        }

        public String[] getDecNameOptions() {
            return  _decNameOptions;
        }

        public FixedObjectGroup.ParseInstruction getParseInstruction() {
            return _parseInstruction;
        }

    }

//===================================================================
//------------------------- Private Inner classes -------------------
//===================================================================

    private static DoCompare decComparator() {
        return new DoCompare(DoCompare.ComparType.DO_DEC);
    }
    private static DoCompare raComparator() {
        return new DoCompare(DoCompare.ComparType.DO_RA);
    }


    /**
     * For comparing RA or DEC
     */
    private static class DoCompare implements Comparator<FixedObject> {
        enum ComparType {DO_RA,DO_DEC};
        private ComparType _which;

       public DoCompare(ComparType whichType) { _which= whichType; }

       public int compare(FixedObject o1, FixedObject o2)  {
          double v1;
          double v2;
          int retval=0;
          if (_which == ComparType.DO_RA) {
              v1= o1.getX();
              v2= o2.getX();
          }
          else if (_which == ComparType.DO_DEC)  {
              v1= o1.getY();
              v2= o2.getY();
          }
          else {
              v1= v2= 0;
              Assert.tst(false);
          }
          if      (v1 < v2) retval= -1;
          else if (v1 > v2) retval=  1;
          return retval;
       }
    }

    /**
     *
     */
    private static class PlotInfo {
        public boolean _show= true;
        public Plot _p;
        public NewPlotNotificationListener _psl;
        PlotInfo( Plot p, NewPlotNotificationListener psl) {
           _p= p;
           _psl= psl;
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
