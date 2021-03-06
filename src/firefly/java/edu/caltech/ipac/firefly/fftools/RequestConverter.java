/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 6/28/12
 * Time: 3:13 PM
 */


import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * @author Trey Roby
 */
public class RequestConverter {

    private static final String FITS_VIEW_DESC= "Fits Viewer ";
    private static final String WP_ERR= "format: ra;dec;CoordSys or ra;dec;CoordSys;name or ra;dec;CoordSys;name;resolver";

// =====================================================================
// -------------------- Public static Methods --------------------------
// =====================================================================


    static WebPlotRequest convertToRequest(JscriptRequest jspr) {
        return convertToRequest(jspr,false);
    }


    static WebPlotRequest convertToRequest(JscriptRequest jspr, boolean addAdvertise) {
        if (jspr==null) return null;

        RequestType typeGuess= null;
        WebPlotRequest wpr= new WebPlotRequest();

        //--------- This is a group of special parameters, don't use short cut routines

        if (jspr.containsKey(WebPlotRequest.FILE)) {
            wpr.setFileName(jspr.getParam(WebPlotRequest.FILE));
            typeGuess= RequestType.FILE;
        }

        if (jspr.containsKey(WebPlotRequest.URL)) {
            wpr.setURL(jspr.getParam(WebPlotRequest.URL));
            typeGuess= RequestType.URL;
        }

        if (jspr.containsKey(WebPlotRequest.SURVEY_KEY))  typeGuess= RequestType.SERVICE;



        if (jspr.containsKey(WebPlotRequest.INIT_RANGE_VALUES)) {
            try {
                String rvStr= jspr.getParam(WebPlotRequest.INIT_RANGE_VALUES);
                RangeValues rv= RangeValues.parse(rvStr);
                if (rv!=null) wpr.setInitialRangeValues(rv);
            } catch (NumberFormatException e) {
                showParseError(jspr, WebPlotRequest.INIT_RANGE_VALUES, "RangeValues");
            }
        }


        if (jspr.containsKey(WebPlotRequest.ROTATION_ANGLE)) {
            try {
                wpr.setRotationAngle(Double.parseDouble(jspr.getParam(WebPlotRequest.ROTATION_ANGLE)));
            } catch (NumberFormatException e) {
                showParseError(jspr, WebPlotRequest.ROTATION_ANGLE, "Double");
            }
        }


        //--------- Use short cut routines for these parameters

        setWorldPtValues(jspr, wpr, WebPlotRequest.WORLD_PT,
                                    WebPlotRequest.OVERLAY_POSITION,
                                    WebPlotRequest.CROP_WORLD_PT1,
                                    WebPlotRequest.CROP_WORLD_PT2
        );

        setBooleanValues(jspr,wpr, WebPlotRequest.ROTATE_NORTH,
                                   WebPlotRequest.ROTATE,
                                   WebPlotRequest.POST_CROP,
                                   WebPlotRequest.ALLOW_IMAGE_SELECTION,
                                   WebPlotRequest.POST_CROP_AND_CENTER,
                                   WebPlotRequest.MULTI_IMAGE_FITS,
                                   WebPlotRequest.HIDE_TITLE_DETAIL,
                                   WebPlotRequest.MINIMAL_READOUT,
                                   WebPlotRequest.FLIP_X,
                                   WebPlotRequest.FLIP_Y
                                   );

        setStringValues(jspr,wpr, WebPlotRequest.TITLE,
                                  WebPlotRequest.POST_TITLE,
                                  WebPlotRequest.PRE_TITLE,
                                  WebPlotRequest.TITLE_FILENAME_MODE_PFX,
                                  WebPlotRequest.SURVEY_KEY,
                                  WebPlotRequest.SURVEY_KEY_ALT,
                                  WebPlotRequest.HEADER_KEY_FOR_TITLE,
                                  WebPlotRequest.PLOT_TO_DIV,
                                  WebPlotRequest.PLOT_DESC_APPEND,
                                  WebPlotRequest.OBJECT_NAME,
                                  WebPlotRequest.DOWNLOAD_FILENAME_ROOT,
                                  WebPlotRequest.DRAWING_SUB_GROUP_ID,
                                  WebPlotRequest.GRID_ID,
                                  WebPlotRequest.CROP_PT1,
                                  WebPlotRequest.CROP_PT2,
                                  WebPlotRequest.PLOT_ID
        );

        setIntValues(jspr, wpr, WebPlotRequest.INIT_COLOR_TABLE,
                                WebPlotRequest.ZOOM_TO_WIDTH,
                                WebPlotRequest.ZOOM_TO_HEIGHT,
                                WebPlotRequest.BLANK_PLOT_WIDTH,
                                WebPlotRequest.MULTI_IMAGE_IDX,
                                WebPlotRequest.BLANK_PLOT_HEIGHT
                     );

        setFloatValues(jspr, wpr, WebPlotRequest.INIT_ZOOM_LEVEL,
                                  WebPlotRequest.SIZE_IN_DEG,
                                  WebPlotRequest.ZOOM_ARCSEC_PER_SCREEN_PIX,
                                  WebPlotRequest.BLANK_ARCSEC_PER_PIX
        );


        setEnumValue(jspr,wpr,ZoomType.class,                  WebPlotRequest.ZOOM_TYPE,true);
        setEnumValue(jspr,wpr,Resolver.class,                  WebPlotRequest.RESOLVER,false);
        setEnumValue(jspr,wpr,WebPlotRequest.ServiceType.class,WebPlotRequest.SERVICE,false);
        setEnumValue(jspr,wpr,WebPlotRequest.TitleOptions.class,WebPlotRequest.TITLE_OPTIONS,true);
        setEnumValue(jspr,wpr,WebPlotRequest.ExpandedTitleOptions.class,WebPlotRequest.EXPANDED_TITLE_OPTIONS,true);
        setEnumValue(jspr,wpr,WebPlotRequest.GridOnStatus.class,WebPlotRequest.GRID_ON,true);


        if (jspr.containsKey(WebPlotRequest.BLANK_ARCSEC_PER_PIX) &&
            jspr.containsKey(WebPlotRequest.BLANK_PLOT_WIDTH) &&
            jspr.containsKey(WebPlotRequest.BLANK_PLOT_HEIGHT)) {
            typeGuess= RequestType.BLANK;
        }



        //--------- Must be smart about type- is is required, but this code makes a guess if it is not defined

        // this if is so the users never has to specify the type, maybe should be moved to a lower level
        if (jspr.containsKey(WebPlotRequest.TYPE)) {
            setEnumValue(jspr,wpr,RequestType.class,WebPlotRequest.TYPE,true);
        }
        else if (typeGuess!=null) {
            wpr.setRequestType(typeGuess);
        }
        else if (wpr.getRequestId()!=null && !WebPlotRequest.ID_NOT_DEFINED.equals(wpr.getRequestId())) {
            wpr.setRequestType(RequestType.PROCESSOR);
        }

        if (addAdvertise)  wpr.setAdvertise(true);


        return wpr;
    }


    //====================================================
    //--------------------- Utility routines
    //====================================================

    private static void setBooleanValues(JscriptRequest jspr, WebPlotRequest wpr, String... key) {
        for(String k : key)  {
            if (jspr.containsKey(k)) {
                wpr.setParam(k, Boolean.parseBoolean(jspr.getParam(k)) + "");
            }
        }

    }
    private static void setStringValues(JscriptRequest jspr, WebPlotRequest wpr, String... key) {
        for(String k : key)  {
            if (jspr.containsKey(k)) {
                wpr.setParam(k, jspr.getParam(k));
            }
        }
    }

    private static void setIntValues(JscriptRequest jspr, WebPlotRequest wpr, String... key) {
        for(String k : key) {
            if (jspr.containsKey(k)) {
                try {
                    wpr.setParam(k, Integer.parseInt(jspr.getParam(k))+""); // parse the int for validation then turn it back to a string
                } catch (NumberFormatException e) {
                    showParseError(jspr, k, "int");
                }
            }
        }
    }

    private static void setFloatValues(JscriptRequest jspr, WebPlotRequest wpr, String... key) {
        for(String k : key) {
            if (jspr.containsKey(k)) {
                try {
                    wpr.setParam(k,Float.parseFloat(jspr.getParam(k))+""); //  parse the float for validation, then turn it back to a string
                } catch (NumberFormatException e) {
                    showParseError(jspr, k, "Float");
                }
            }
        }
    }

    private static void setWorldPtValues(JscriptRequest jspr, WebPlotRequest wpr, String... key) {
        for(String k : key) {
            if (jspr.containsKey(k)) {
                try {
                    WorldPt wp= ResolvedWorldPt.parse(jspr.getParam(k) + "");
                    if (wp!=null) {
                        wpr.setParam(k,wp);
                    }
                    else {
                        showParseError(jspr, k, WP_ERR);
                    }
                } catch (NumberFormatException e) {
                    showParseError(jspr, k, WP_ERR);
                }
            }
        }
    }

    private static <T extends Enum> void setEnumValue(JscriptRequest jspr,
                                                      WebPlotRequest wpr,
                                                      Class<T> enumClass,
                                                      String key,
                                                      boolean convertToUpper) {
        if (jspr.containsKey(key)) {
            String s= jspr.getParam(key);
            if (convertToUpper) s= s.toUpperCase();
            if (s!=null) {
                try {
                    wpr.setParam(key,  Enum.valueOf(enumClass, s) + "");
                } catch (Exception e) {
                    showParseError(jspr, key, "enum: " + enumClass.toString());
                }
            }
        }
    }




    private static void showParseError(JscriptRequest jsrp, String key, String toType) {
        showParseError(key,jsrp.getParam(key),toType);
    }

    private static void showParseError(String param, String value, String toType) {
        FFToolEnv.logParamParseError(FITS_VIEW_DESC,param,value, toType);
    }
}

