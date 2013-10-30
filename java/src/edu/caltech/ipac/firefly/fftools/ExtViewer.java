package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 10/22/13
 * Time: 11:03 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Window;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ExtViewer {


    private static final Map<String,AppMessenger> _externalTargets= new HashMap<String, AppMessenger>(5);


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    /**
     *
     * @param jspr the request from java script
     */
    public static void showTable(JscriptRequest jspr, String target) {
        TableServerRequest wpr= TableJSInterface.convertToRequest(jspr);
        AppMessenger mess;
        if (_externalTargets.containsKey(target)) {
            mess= _externalTargets.get(target);
        }
        else {
            mess= new AppMessenger();
            _externalTargets.put(target,mess);
        }
        mess.sendTableToApp(wpr, target);
    }


    /**
     *
     * @param jspr the request from java script
     */
    public static void plot(JscriptRequest jspr, String target) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        AppMessenger mess;
        if (_externalTargets.containsKey(target)) {
            mess= _externalTargets.get(target);
        }
        else {
            mess= new AppMessenger();
            _externalTargets.put(target,mess);
        }
        mess.sendPlotToApp(wpr, target);
    }

    /**
     *
     * @param jsprArray the array of request from java script
     * @param target the window target to plot to
     */
    public static void plotMulti(JsArray<JscriptRequest> jsprArray, String target) {
        List<WebPlotRequest> reqList= new ArrayList<WebPlotRequest>(jsprArray.length());
        for(int i= 0; (i<jsprArray.length()); i++) {
            WebPlotRequest r= RequestConverter.convertToRequest(jsprArray.get(i),false);
            if (r!=null) {
                reqList.add(r);
            }
        }
        AppMessenger mess;
        if (_externalTargets.containsKey(target)) {
            mess= _externalTargets.get(target);
        }
        else {
            mess= new AppMessenger();
            _externalTargets.put(target,mess);
        }
        if (reqList.size()>0) mess.sendPlotsToApp(reqList, target);
    }


    /**
     *
     * @param red the request from java script
     * @param green the request from java script
     * @param blue the request from java script
     * @param target the window target to plot to
     */
    public static void plot3Color(JscriptRequest red,
                                  JscriptRequest green,
                                  JscriptRequest blue,
                                  String target) {
        WebPlotRequest redReq= RequestConverter.convertToRequest(red,false);
        WebPlotRequest greenReq= RequestConverter.convertToRequest(green,false);
        WebPlotRequest blueReq= RequestConverter.convertToRequest(blue,false);
        if (redReq!=null || greenReq!=null || blueReq!=null) {
            AppMessenger mess;
            if (_externalTargets.containsKey(target)) {
                mess= _externalTargets.get(target);
            }
            else {
                mess= new AppMessenger();
                _externalTargets.put(target,mess);
            }
            mess.send3ColorPlotToApp(redReq,greenReq,blueReq, target);
        }
        else {
            FFToolEnv.logDebugMsg("3 color external request failed, no valid request");
        }

    }


    public static void plotExternal_OLD_2(JscriptRequest jspr, String target) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        findURLAndMakeFull(wpr);
        String url= getHost(GWT.getModuleBaseURL()) + "/fftools/app.html"; // TODO: need to fixed this
        List<Param> pList= new ArrayList(5);
        pList.add(new Param(Request.ID_KEY, "FFToolsImageCmd"));
        pList.add(new Param(CommonParams.DO_PLOT, "true"));
        for(Param p : wpr.getParams()) {
            if (p.getName()!=Request.ID_KEY) pList.add(p);
        }

        url= WebUtil.encodeUrl(url, WebUtil.ParamType.POUND, pList);
        if (target==null) target= "_blank";
        Window.open(url, target, "");
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static void findURLAndMakeFull(WebPlotRequest wpr) {
        if (wpr.containsParam(WebPlotRequest.URL)) {
            String url= wpr.getURL();
            url= FFToolEnv.modifyURLToFull(url);
            wpr.setURL(url);
        }

    }

    private static String getHost(String url) {
        String retval= null;
        if (url!=null && url.length()>8) {
            int lastSlash= url.indexOf("/",9);
            if (lastSlash>-1) {
                retval=  url.substring(0,lastSlash);
            }
        }
        return retval;
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
