/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.SpacialType;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.ui.input.DegreeInputField;
import edu.caltech.ipac.firefly.ui.input.FileUploadField;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * User: roby
 * Date: Nov 4, 2009
 * Time: 10:01:46 AM
 */


/**
 * @author Trey Roby
*/
abstract class SpatialOps {

    private SpacialBehaviorPanel.HasRangePanel rangePanel;
    private static final WebClassProperties prop = new WebClassProperties(SpatialOps.class);


    public abstract List<Param> getParams();
    public abstract void setParams(List<Param> paramList);
    public abstract boolean validate() throws ValidationException;
    public abstract SpacialType getSpacialType();

    public boolean getRequireUpload() { return false; }
    public boolean getRequireTarget() { return true; }

    protected SpatialOps(SpacialBehaviorPanel.HasRangePanel rangePanel) {
        this.rangePanel = rangePanel;
    }
    protected SpatialOps() { this(null); }

    public void doUpload(AsyncCallback<String> cb) {
        cb.onSuccess("ok");
    }

    public void updateMax(int maxArcSec) {
        if (rangePanel!=null) rangePanel.updateMax(maxArcSec);
    }

    protected List<Param> makeList(Param... initParams) {
        List<Param> l= new ArrayList<Param>(10);
        l.addAll(Arrays.asList(initParams));
        return l;
    }

    protected static Param findParam(List<Param> list, String key)  {
        Param retval= null;
        for(Param p : list) {
            if (p.isKey(key)) {
                retval= p;
                break;
            }
        }
        return retval;
    }

    protected void setIfDefined(List<Param> list, InputField field, String name) {
        Param p= findParam(list,name);
        if (p!=null) field.setValue(p.getValue());
    }

    protected static void updateRadiusField(DegreeInputField df , List<Param> list, String degreeName) {
        Param p= findParam(list, CatalogRequest.DISPLAY_UNITS);
        DegreeFieldDef.Units units= DegreeFieldDef.Units.DEGREE;
        if (p!=null) {
            try {
                units= Enum.valueOf(DegreeFieldDef.Units.class, p.getValue());
            } catch (Exception e) {
                // do nothing
            }
        }
        df.setUnits(units);

        p= findParam(list, degreeName);
        if (p!=null) {
            double deg= StringUtils.getDouble(p.getValue(),0);
            df.setValue(deg+"");
        }
    }


    public static class Cone extends SpatialOps {


        private DegreeInputField degreeField;

        public Cone(DegreeInputField degreeField, SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.degreeField = degreeField;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Cone; }

        @Override
        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) degreeField.getFieldDef();
            double degreeVal= df.getDoubleValue(degreeField.getValue());
//            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.CONE.getDesc()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.DEGREE.toString()),
                    new Param(CatalogRequest.DISPLAY_UNITS, degreeField.getUnits().toString()),
                    new Param(CatalogRequest.RADIUS, degreeVal+ "")
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(degreeField,paramList, CatalogRequest.RADIUS);
        }

        @Override
        public boolean validate() throws ValidationException {
            return degreeField.validate();
        }

    }



    public static class Elliptical extends SpatialOps {
        private final DegreeInputField smAxis;
        private final InputField pa;
        private final InputField ratio;

        public Elliptical(DegreeInputField smAxis,
                          InputField pa,
                          InputField ratio,
                          SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.smAxis = smAxis;
            this.pa = pa;
            this.ratio = ratio;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Elliptical; }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) smAxis.getFieldDef();
            double dVal= df.getDoubleValue( smAxis.getValue());
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.ELIPTICAL.getDesc()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.DEGREE.toString()),
                    new Param(CatalogRequest.DISPLAY_UNITS, smAxis.getUnits().toString()),
                    new Param(CatalogRequest.RADIUS, dVal+""),
                    new Param(CatalogRequest.PA, pa.getNumberValue().doubleValue()+""),
                    new Param(CatalogRequest.RATIO, ratio.getNumberValue().doubleValue()+"")
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            Param p;
            updateRadiusField(smAxis,paramList, CatalogRequest.RADIUS);
            p= findParam(paramList, CatalogRequest.PA);
            if (p!=null) pa.setValue(p.getValue());
            p= findParam(paramList, CatalogRequest.RATIO);
            if (p!=null) ratio.setValue(p.getValue());
        }

        @Override
        public boolean validate() throws ValidationException {
            return smAxis.validate() && pa.validate() && ratio.validate();
        }
    }


    public static class Box extends SpatialOps {
        private final DegreeInputField sideField;

        public Box(DegreeInputField sideField, SpacialBehaviorPanel.HasRangePanel rangePanel) {
            super(rangePanel);
            this.sideField = sideField;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Box; }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) sideField.getFieldDef();
            double dVal= df.getDoubleValue(sideField.getValue());

            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.BOX.getDesc()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.DEGREE.toString()),
                    new Param(CatalogRequest.DISPLAY_UNITS, sideField.getUnits().toString()),
                    new Param(CatalogRequest.SIZE, dVal+"")
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(sideField,paramList,CatalogRequest.SIZE);
        }

        public boolean validate() throws ValidationException {
            return sideField.validate();
        }
    }


    public abstract static class Ibe extends SpatialOps {
        private final InputField intersect;
        private final InputField size;
        private final InputField subSize;
        private final InputField mCenter;

        public Ibe(InputField intersect, InputField size, InputField subSize, InputField mCenter) {
            this.intersect = intersect;
            this.size = size;
            this.subSize = subSize;
            this.mCenter = mCenter;

        }


        public List<Param> getParams() {

            return makeList(
                    new Param("intersect", intersect.getValue()),
                    new Param("size", size.getValue()),
                    new Param("subsize", subSize.getValue()),
                    new Param("mcenter", mCenter.getValue())
            );
        }

        @Override
        public void setParams(List<Param> paramList) {
            setIfDefined(paramList,intersect,"intersect");
            setIfDefined(paramList,size,"size");
            setIfDefined(paramList,subSize,"subsize");
            setIfDefined(paramList,mCenter,"mcenter");
        }

        public boolean validate() throws ValidationException {
            return intersect.validate() && size.validate() && subSize.validate();
        }
    }

    public static class IbeSingle extends Ibe {

        public IbeSingle(InputField intersect, InputField size, InputField subSize, InputField mCenter) {
            super(intersect, size, subSize, mCenter);
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.IbeSingleImage; }
    }


    public static class IbeTableUpload extends Ibe {

        private FileUploadField uploadField;

        public IbeTableUpload(InputField intersect,
                              InputField size,
                              InputField subSize,
                              InputField mCenter,
                              FileUploadField uploadField ) {
            super(intersect, size, subSize, mCenter);
            this.uploadField = uploadField;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.IbeMultiTableUpload; }

        @Override
        public boolean getRequireUpload() { return true; }

        public boolean getRequireTarget() { return false; }

        @Override
        public void doUpload(AsyncCallback<String> cb) {
            uploadField.submit(cb);
        }

        @Override
        public boolean validate() throws ValidationException {
            if (StringUtils.isEmpty(uploadField.validate())) {
                throw new ValidationException(prop.getError("fileUpload"));
            }
            return super.validate();
        }


    }

    public static class IbeTablePrevSearch extends Ibe {

        public IbeTablePrevSearch(InputField intersect, InputField size, InputField subSize, InputField mCenter) {
            super(intersect, size, subSize, mCenter);
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.IbeMultiPrevSearch; }

        public boolean getRequireTarget() { return false; }



    }



    public static class Polygon extends SpatialOps {

        private final InputField polygonValue;

        public Polygon(InputField polygonValues) {
            this.polygonValue = polygonValues;
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.Polygon; }

        public List<Param> getParams() {
            String fv= polygonValue.getValue();
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.POLYGON.getDesc()),
                    new Param(CatalogRequest.POLYGON, fv)
                    );
        }

        @Override
        public void setParams(List<Param> paramList) {
            Param p= findParam(paramList, CatalogRequest.POLYGON);
            if (p!=null) polygonValue.setValue(p.getValue());
        }
        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            return polygonValue.validate();
        }
    }



    public static class TableUpload extends SpatialOps {

        private final DegreeInputField radiusField;
        private FileUploadField uploadField;
        private CheckBox oneToOneCB;

        public TableUpload(DegreeInputField radiusField,
                           FileUploadField uploadField,
                           SpacialBehaviorPanel.HasRangePanel rangePanel,
                           CheckBox oneToOneCB) {
            super(rangePanel);
            this.radiusField = radiusField;
            this.uploadField = uploadField;
            this.oneToOneCB = oneToOneCB;
        }


        @Override
        public SpacialType getSpacialType() { return SpacialType.MultiTableUpload; }

        public List<Param> getParams() {

            DegreeFieldDef df = (DegreeFieldDef) radiusField.getFieldDef();
            double degreeVal= df.getDoubleValue(radiusField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);

            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.TABLE.getDesc()),
                    new Param(CatalogRequest.FILE_NAME, uploadField.getValue()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.DEGREE.toString()),
                    new Param(CatalogRequest.DISPLAY_UNITS, radiusField.getUnits().toString()),
                    new Param(CatalogRequest.RADIUS, valInAS + ""),
                    new Param(CatalogRequest.ONE_TO_ONE, oneToOneCB.getValue() ? "1" : "0")
            );
        }

        @Override
        public void doUpload(AsyncCallback<String> cb) {
            uploadField.submit(cb);
        }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(radiusField,paramList, CatalogRequest.RADIUS);
            Param p= findParam(paramList, CatalogRequest.FILE_NAME);
            if (p!=null) uploadField.setValue(p.getValue());
            p= findParam(paramList, CatalogRequest.ONE_TO_ONE);
            oneToOneCB.setValue(p!=null && "1".equals(p.getValue()));
        }

        @Override
        public boolean getRequireUpload() { return true; }

        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            if (StringUtils.isEmpty(uploadField.validate())) {
                throw new ValidationException(prop.getError("fileUpload"));
            }
            return true;
        }
   }


    public static class PrevSearch extends SpatialOps { //todo

        private final DegreeInputField radiusField;
        private CheckBox oneToOneCB;

        public PrevSearch(DegreeInputField radiusField,
                           SpacialBehaviorPanel.HasRangePanel rangePanel,
                           CheckBox oneToOneCB) {
            super(rangePanel);
            this.radiusField = radiusField;
            this.oneToOneCB = oneToOneCB;
        }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) radiusField.getFieldDef();
            double degreeVal= df.getDoubleValue(radiusField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.TABLE.getDesc()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.DEGREE.toString()),
                    new Param(CatalogRequest.DISPLAY_UNITS, radiusField.getUnits().toString()),
                    new Param(CatalogRequest.RADIUS, valInAS + ""),
                    new Param(CatalogRequest.ONE_TO_ONE, oneToOneCB.getValue() ? "1" : "0")
            );
        }


        @Override
        public SpacialType getSpacialType() { return SpacialType.MultiPrevSearch; }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(radiusField,paramList, CatalogRequest.RADIUS);
            Param p= findParam(paramList, CatalogRequest.ONE_TO_ONE);
            oneToOneCB.setValue(p!=null && "1".equals(p.getValue()));
        }
        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            return true;
        }
    }


    public static class MultiPoint extends SpatialOps { //todo

        private final DegreeInputField radiusField;
        private CheckBox oneToOneCB;

        public MultiPoint(DegreeInputField radiusField,
                           SpacialBehaviorPanel.HasRangePanel rangePanel,
                           CheckBox oneToOneCB) {
            super(rangePanel);
            this.radiusField = radiusField;
            this.oneToOneCB = oneToOneCB;
        }

        public List<Param> getParams() {
            DegreeFieldDef df = (DegreeFieldDef) radiusField.getFieldDef();
            double degreeVal= df.getDoubleValue(radiusField.getValue());
            double valInAS= DegreeFieldDef.getArcsecValue(degreeVal, DegreeFieldDef.Units.DEGREE);
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.TABLE.getDesc()),
                    new Param(CatalogRequest.RAD_UNITS, CatalogRequest.RadUnits.DEGREE.toString()),
                    new Param(CatalogRequest.DISPLAY_UNITS, radiusField.getUnits().toString()),
                    new Param(CatalogRequest.RADIUS, valInAS + ""),
                    new Param(CatalogRequest.ONE_TO_ONE, oneToOneCB.getValue() ? "1" : "0")
            );
        }


        @Override
        public SpacialType getSpacialType() { return SpacialType.MultiPoints; }

        @Override
        public void setParams(List<Param> paramList) {
            updateRadiusField(radiusField,paramList, CatalogRequest.RADIUS);
            Param p= findParam(paramList, CatalogRequest.ONE_TO_ONE);
            oneToOneCB.setValue(p!=null && "1".equals(p.getValue()));
        }
        public boolean getRequireTarget() { return false; }

        public boolean validate() throws ValidationException {
            return true;
        }
    }




    public static class AllSky extends SpatialOps {

        public List<Param> getParams() {
            return makeList(
                    new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.ALL_SKY.getDesc())
            );
        }

        @Override
        public SpacialType getSpacialType() { return SpacialType.AllSky; }

        @Override
        public void setParams(List<Param> paramList) {
            //todo
        }

        public boolean validate() throws ValidationException { return true; }
    }


}






