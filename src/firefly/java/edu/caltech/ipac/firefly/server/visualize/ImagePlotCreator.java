/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/17/11
 * Time: 3:33 PM
 */


import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.HistogramOps;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ImagePlotCreator {

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final String OBS_DATE="Obs date";
    private static final String MID_OBS="Mid obs";

    static ImagePlotInfo[] makeAllNoBand(String workingCtxStr,
                                         PlotState stateAry[],
                                         FileReadInfo[] readAry,
                                         ZoomChoice zoomChoice) throws FailedRequestException,
                                                                       FitsException,
                                                                       GeomException,
                                                                       IOException {
         // never use this method with three color plots

         ImagePlotInfo piAry[]= new ImagePlotInfo[readAry.length];
         FileReadInfo readInfo;
         Map<Band,WebFitsData> wfDataMap= new LinkedHashMap<Band,WebFitsData>(5);
         for(int i= 0; (i<readAry.length); i++)  {
             readInfo= readAry[i];
             WebPlotRequest req= stateAry[i].getPrimaryWebPlotRequest();
             if (readAry.length>3) {
                 PlotServUtils.updateProgress(req, ProgressStat.PType.CREATING,
                                              PlotServUtils.CREATING_MSG+": "+ (i+1)+" of "+readAry.length);
             }
             else  {
                 PlotServUtils.updateProgress(req, ProgressStat.PType.CREATING, PlotServUtils.CREATING_MSG);
             }
             ImagePlot plot= createImagePlot(stateAry[i],readInfo,zoomChoice, readAry.length>1);
             WebFitsData wfData= makeWebFitsData(plot,readInfo.getBand(),readInfo.getOriginalFile());
             wfDataMap.put(Band.NO_BAND,wfData);
             Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<Band,ModFileWriter>(1);
             if (readInfo.getModFileWriter()!=null) fileWriterMap.put(Band.NO_BAND,readInfo.getModFileWriter());
             piAry[i]= new ImagePlotInfo(stateAry[i],plot, readInfo.getDataDesc(), wfDataMap,fileWriterMap);
             VisContext.shouldContinue(workingCtxStr);
         }

         return piAry;
     }

    static ImagePlotInfo makeOneImagePerBand(String workingCtxStr,
                                             PlotState state,
                                             Map<Band, FileReadInfo[]> readInfoMap,
                                             ZoomChoice zoomChoice)  throws FailedRequestException,
                                                                            FitsException,
                                                                            GeomException,
                                                                            IOException {


         ImagePlotInfo retval;
         ImagePlot plot= null;
         boolean first= true;
         Map<Band,WebFitsData> wfDataMap= new LinkedHashMap<Band,WebFitsData>(5);
         Map<Band,ModFileWriter> fileWriterMap= new LinkedHashMap<Band,ModFileWriter>(5);
         for(Map.Entry<Band,FileReadInfo[]> entry :  readInfoMap.entrySet()) {
             Band band= entry.getKey();
             FileReadInfo readInfoAry[]= entry.getValue();
             FileReadInfo readInfo= readInfoAry[state.getImageIdx(band)];
             int bIdx= PlotServUtils.cnvtBand(readInfo.getBand());
             if (first) {
                 plot= createImagePlot(state,readInfo,zoomChoice,false);
                 if (state.isThreeColor()) {
                     plot.setThreeColorBand(readInfo.getFitsRead(),bIdx);
                     plot.setThreeColorBandVisible(bIdx, state.isBandVisible(readInfo.getBand()));
                 }
                 if (readInfo.getModFileWriter()!=null) {
                     fileWriterMap.put(band,readInfo.getModFileWriter());
                 }
                 first= false;
             }
             else {
                 ModFileWriter mfw= createBand(state,plot,readInfo);
                 if (mfw!=null) {
                     fileWriterMap.put(band,mfw);
                 }
                 else if (readInfo.getModFileWriter()!=null) {
                     fileWriterMap.put(band,readInfo.getModFileWriter());
                 }
             }
             WebFitsData wfData= makeWebFitsData(plot, readInfo.getBand(), readInfo.getOriginalFile());
             wfDataMap.put(band, wfData);
             VisContext.shouldContinue(workingCtxStr);
         }
         String desc= make3ColorDataDesc(readInfoMap);
         retval= new ImagePlotInfo(state,plot,desc, wfDataMap,fileWriterMap);

         if (first) _log.error("something is wrong, plot not setup correctly - no color bands specified");
         return retval;
     }

    /**
     * Create the ImagePlot.  If this is the first time the plot has been created the compute the
     * appropriate zoom, otherwise using the zoom level in the PlotState object.
     * Using the FitsRead in the PlotData object.  Record the zoom level in the PlotData object.
     * @param state plot state
     * @param readInfo the object containing the fits file read information
     * @return the image plot object
     * @throws nom.tam.fits.FitsException if creating plot fails
     */
    static ImagePlot createImagePlot(PlotState state,
                                     FileReadInfo readInfo,
                                     ZoomChoice zoomChoice,
                                     boolean    isMultiImage) throws FitsException {

        RangeValues rv= state.getPrimaryRangeValues();
        if (rv==null) {
            rv= FitsRead.getDefaultFutureStretch();
            state.setRangeValues(rv,state.firstBand());
        }

        // todo uncomment
//        state.setDecimationLevel(pd._band,pd._decimation);
        float zoomLevel= zoomChoice.getZoomLevel();

        ImagePlot plot= PlotServUtils.makeImagePlot( readInfo.getFitsRead(), zoomLevel,
                                                     state.isThreeColor(),
                                                     state.getColorTableId(), rv);


        if (state.isNewPlot()) { // new plot requires computing the zoom level

            zoomLevel= computeZoomLevel(plot,zoomChoice);
            plot.getPlotGroup().setZoomTo(zoomLevel);
            state.setZoomLevel(zoomLevel);
        }

        state.setZoomLevel(zoomLevel);
        initPlotTitle(state,plot,readInfo.getDataDesc(),isMultiImage);
        return plot;
    }

    public static ModFileWriter createBand(PlotState state, ImagePlot plot, FileReadInfo readInfo)
                                                                throws FitsException,
                                                                       IOException,
                                                                       GeomException {
        ModFileWriter retval= null;
        int bIdx= PlotServUtils.cnvtBand(readInfo.getBand());
        plot.addThreeColorBand(readInfo.getFitsRead(),bIdx);
        plot.setThreeColorBandVisible(bIdx, state.isBandVisible(readInfo.getBand()));
        HistogramOps histOps= plot.getHistogramOps(bIdx);
        FitsRead tmpFR= histOps.getFitsRead();
        if (tmpFR!=readInfo.getFitsRead() && readInfo.getWorkingFile()!=null) { // testing to see it the fits read got geomed when the band was added
            state.setImageIdx(0, readInfo.getBand());
            retval = new ModFileWriter.GeomFileWriter(readInfo.getWorkingFile(),0,tmpFR,readInfo.getBand(),false);
        }

        RangeValues rv= state.getRangeValues(readInfo.getBand());
        if (rv==null) {
            rv= FitsRead.getDefaultFutureStretch();
            state.setRangeValues(rv, readInfo.getBand());
        }
        histOps.recomputeStretch(rv,true);
        return retval;
    }

    static void initPlotTitle(PlotState state,
                              ImagePlot plot,
                              String dataDesc,
                              boolean isMultiImage) {
        WebPlotRequest req= state.getPrimaryWebPlotRequest();
        WebPlotRequest.TitleOptions titleOps= req.getTitleOptions();
        String headerKey= req.getHeaderKeyForTitle();
        if ((isMultiImage && (titleOps== WebPlotRequest.TitleOptions.NONE ||titleOps== WebPlotRequest.TitleOptions.FILE_NAME)) ||
                  (titleOps==WebPlotRequest.TitleOptions.HEADER_KEY && StringUtils.isEmpty(headerKey))) {
            titleOps= WebPlotRequest.TitleOptions.HEADER_KEY;
            headerKey= "EXTNAME";
        }
        String s= req.getPlotDescAppend();
        plot.setPlotDesc("");
        Header header= plot.getFitsRead().getHeader();


        switch (titleOps) {
            case NONE:
                plot.setPlotDesc("");
                break;
            case PLOT_DESC:
                String base= req.getTitle()==null ? "" : req.getTitle();
                plot.setPlotDesc(base+ dataDesc);
                break;
            case FILE_NAME:
                break;
            case HEADER_KEY:
                HeaderCard card= header.findCard(headerKey);
                if (card==null && state.getCubeCnt(state.firstBand())>0) {
                    card= header.findCard("PLANE"+state.getImageIdx(state.firstBand()));
                }
                String hTitle= card!=null ? card.getValue() : "";
                plot.setPlotDesc(hTitle);
                break;
            case PLOT_DESC_PLUS:
                plot.setPlotDesc(req.getTitle()+ (s!=null ? " "+s : ""));
                break;
            case SERVICE_OBS_DATE:
                if (req.getRequestType()== RequestType.SERVICE) {
//                    String desc= req.getServiceType()== WebPlotRequest.ServiceType.WISE ? MID_OBS : OBS_DATE;
                    String title= req.getTitle() + ": " +
//                                  desc + ": " +
                                  PlotServUtils.getDateValueFromServiceFits(req.getServiceType(), header);
                    plot.setPlotDesc(title);
                }
                break;
        }
    }

//    private static String getServiceDateDesc(WebPlotRequest.ServiceType sType, FitsRead fr) {
//        String preFix;
//        String header= "none";
//        preFix= OBS_DATE;
//        switch (sType) {
//            case TWOMASS:
//                header= "ORDATE";
//                break;
//            case DSS:
//                header= "DATE-OBS";
//                break;
//            case WISE:
//                preFix= MID_OBS;
//                header= "MIDOBS";
//                break;
//            case SDSS:
//                header= "DATE-OBS";
//                break;
//            case IRIS:
//                header= "DATEIRIS";
//                break;
//        }
//        return preFix + ": " + PlotServUtils.getDateValueFromServiceFits(header, fr);
//    }


//    private static String getDateValue(String addDateTitleStr, FitsRead fr) {
//        String retval = "";
//        if (addDateTitleStr!=null && addDateTitleStr.contains(";")) {
//            String dateAry[]= addDateTitleStr.split(";");
//            retval = dateAry[1] + ": " + PlotServUtils.getDateValueFromServiceFits(dateAry[0], fr);
//        }
//        return retval;
//    }




    private static String make3ColorDataDesc(Map<Band, FileReadInfo[]> readInfoMap) {

        StringBuffer desc= new StringBuffer(100);
        desc.append("3 Color: ");
        for(Map.Entry<Band,FileReadInfo[]> entry : readInfoMap.entrySet()) {
            desc.append(readInfoMap.get(entry.getKey())[0].getDataDesc());
        }
        return desc.toString();
    }



    static float computeZoomLevel(ImagePlot plot, ZoomChoice zoomChoice) {
        int width=  plot.getImageDataWidth();
        int height= plot.getImageDataHeight();
        float retval= zoomChoice.getZoomLevel();
        if (zoomChoice.isSmartZoom()) {
            retval= computeSmartZoom(width,height,zoomChoice.getZoomType());
        }
        else if (zoomChoice.getZoomType()== ZoomType.TO_WIDTH) {
            retval= (float)zoomChoice.getWidth() / (float)width ;
            if (zoomChoice.hasMaxZoomLevel()) {
                if (retval>zoomChoice.getMaxZoomLevel()) retval=zoomChoice.getMaxZoomLevel();
            }
        }
        else if (zoomChoice.getZoomType()== ZoomType.FULL_SCREEN) {
            retval= VisUtil.getEstimatedFullZoomFactor(VisUtil.FullType.WIDTH_HEIGHT,width, height,
                                                       zoomChoice.getWidth(), zoomChoice.getHeight());
            if (zoomChoice.hasMaxZoomLevel()) {
                if (retval>zoomChoice.getMaxZoomLevel()) retval=zoomChoice.getMaxZoomLevel();
            }
        }
        else if (zoomChoice.getZoomType()== ZoomType.ARCSEC_PER_SCREEN_PIX) {
            retval= (float)plot.getPixelScale() / zoomChoice.getArcsecPerScreenPix();
        }
        return retval;
    }

    static float computeSmartZoom(int width, int height, ZoomType zoomType) {
        float zoomLevel;
        if (width> 6200 || height> 6200 )      zoomLevel= 1F/32F;
        else if (width> 2800 || height> 2800 ) zoomLevel= 1F/16F;
        else if (width> 2000 || height> 2000 ) zoomLevel= 1F/8F;
        else if (width> 1200 || height> 1200 ) zoomLevel= 1F/4F;
        else if (width> 500 || height> 500 )   zoomLevel= 1F/2F;
        else if (width< 100 && height< 100 )   zoomLevel= 4F;
        else if (width< 30 && height< 30 )     zoomLevel= 4F;
        else                  zoomLevel= 1;

        width*= zoomLevel;
        height*= zoomLevel;
        switch (zoomType) {
            case SMART_SMALL:
                if (width>600 || height > 600) zoomLevel/=4;
                else if (width>200 || height > 200) zoomLevel/=2;
                break;
            case SMART_LARGE:
                if (width<400 || height < 400) zoomLevel/=2;
                zoomLevel*=2;
                break;
        }

        return zoomLevel;
    }

    public static WebFitsData makeWebFitsData(ImagePlot plot, Band band, File f) {
        long fileLength= (f!=null && f.canRead()) ? f.length() : 0;
        int idx= PlotServUtils.cnvtBand(band);
        HistogramOps ops= plot.getHistogramOps(idx);
        return new WebFitsData( ops.getDataMin(), ops.getDataMax(),
                                fileLength, plot.getFluxUnits(idx));
    }
}

