package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;
/**
 * User: roby
 * Date: Jun 22, 2009
 * Time: 10:10:10 AM
 */


/**
 * @author Trey Roby
 */
public class CoverageChooser {


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public WebPlotRequest getRequest(WorldPt wp,
                                     float size,
                                     String baseTitle,
                                     ZoomType smartType) {
        return getRequest(wp,size,baseTitle,smartType,false, WebPlotRequest.GridOnStatus.FALSE,0);

    }


    public WebPlotRequest getRequest(WorldPt wp,
                                     float size,
                                     String baseTitle,
                                     ZoomType smartType,
                                     boolean  blank,
                                     WebPlotRequest.GridOnStatus gridOn,
                                     int      width) {
        String title;
        WebPlotRequest request;
        size *= 2.2;  //image fetch takes radius as diameter
        float radiusAS = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC, size);
        if (blank) {
            float asSize= size*3600;
            float pixPerAS= asSize/(float)width;
            request= WebPlotRequest.makeBlankPlotRequest(wp,pixPerAS,width,width); // pass width to width & height to make a square
            request.setTitle(baseTitle);
        }
        else if (radiusAS < 500) {
            request = WebPlotRequest.make2MASSRequest(wp, size);
            title = baseTitle + " 2MASS k";
            request.setTitle(title);
        } else if (radiusAS < 1800) {
            request = WebPlotRequest.makeDSSOrIRISRequest(wp, size);
            title = baseTitle + " DSS";
            request.setTitle(title);
        } else if (size < 12.5) {
            if (size < 1) size = 1;
            else if (size < 2) size = 2;
            else if (size < 3) size = 3;
            else if (size < 4) size = 4;
            else size = 5;
            request = WebPlotRequest.makeIRISRequest(wp, size);
            title = baseTitle + " IRAS:IRIS 100";
            request.setTitle(title);
        } else {
            request = WebPlotRequest.makeAllSkyPlotRequest();
            title = baseTitle + " All Sky";
            if (size < 30) {
                WorldPt wpGal = VisUtil.convert(wp, CoordinateSys.GALACTIC);


                size *= 2;
                WorldPt startWP = new WorldPt(wpGal.getLon() - size, wpGal.getLat() - size);
                WorldPt endWP = new WorldPt(wpGal.getLon() + size, wpGal.getLat() + size);

                double correctX1 = startWP.getLon();
                double correctY1 = startWP.getLat();
                double correctX2 = endWP.getLon();
                double correctY2 = endWP.getLat();

                if (correctX1 < 0) {
                    correctX1 = 0;
                    correctX2 = size * 2;
                }
                if (correctX1 > 360) {
                    correctX1 = 360 - (size * 2 + 1);
                    correctX2 = 359.75;
                }
                if (correctX2 < 0) {
                    correctX2 = 0;
                    correctX1 = size * 2;
                }
                if (correctX2 > 360) {
                    correctX2 = 360 - (size * 2 + 1);
                    correctX1 = 359.75;
                }


                if (correctY1 < -90) {
                    correctY1 = -90;
                    correctY2 = -90 + size * 2;
                }
                if (correctY1 > 90) {
                    correctY1 = 90 - (size * 2 + 1);
                    correctY2 = 89.75;
                }
                if (correctY2 < -90) {
                    correctY2 = -90;
                    correctY1 = -90 + size * 2;
                }
                if (correctY2 > 90) {
                    correctY2 = 90 - (size * 2 + 1);
                    correctY1 = 89.75;
                }

                if (Math.abs(correctX1-correctY1)<30 && Math.abs(correctY1-correctY2)<30) {
                    title += "-cropped";
                    request.setPostCrop(true);
                    request.setCropPt1(new WorldPt(correctX1, correctY1, CoordinateSys.GALACTIC));
                    request.setCropPt2(new WorldPt(correctX2, correctY2, CoordinateSys.GALACTIC));
                }

            }
            request.setTitleOptions(WebPlotRequest.TitleOptions.PLOT_DESC);
            request.setZoomType(ZoomType.SMART);
            request.setGridOn(gridOn);
            if (WebPlotRequest.isSmartZoom(smartType)) request.setZoomType(smartType);

        }
        return request;
    }


//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

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
