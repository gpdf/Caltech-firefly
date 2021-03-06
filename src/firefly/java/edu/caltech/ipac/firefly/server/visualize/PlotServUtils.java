/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.astro.net.TargetNetwork;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.GridLayer;
import edu.caltech.ipac.visualize.draw.ScalableObjectPosition;
import edu.caltech.ipac.visualize.draw.VectorObject;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotGroup;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.output.PlotOutput;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 9:46:23 AM
 */


/**
 * @author Trey Roby
 */
public class PlotServUtils {

    public static final String FUNCTION = "Function: ";
    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);
    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final int PLOT_FULL_WIDTH = -25;
    private static final int PLOT_FULL_HEIGHT = -25;
    private static int _nameCnt=1;

    private static final String JPG_NAME_EXT=FileUtil.jpg;
    private static final String PNG_NAME_EXT=FileUtil.png;
    private static final String GIF_NAME_EXT=FileUtil.gif;
    private static final String _hostname;
    private static final String _pngNameExt="." + PNG_NAME_EXT;

    public static final String STARTING_READ_MSG = "Retrieving Data";
    public static final String READ_PERCENT_MSG = "Retrieving ";
    public static final String ENDING_READ_MSG = "Loading Data";
    public static final String CREATING_MSG =  "Creating Images";
    public static final String PROCESSING_MSG =  "Processing Images";

    static {
        _hostname= FileUtil.getHostname();
    }

    static void createThumbnail(ImagePlot plot,
                                PlotImages images,
                                boolean justName,
                                int     thumbnailSize) throws IOException, FitsException {

        PlotGroup plotGroup= plot.getPlotGroup();
        float saveZLevel= plotGroup.getZoomFact();
        int div= Math.max( plotGroup.getGroupImageWidth(),
                           plotGroup.getGroupImageHeight() );
        float tZoomLevel= thumbnailSize /(float)div;
        plotGroup.setZoomTo(tZoomLevel);

        File f= new File(VisContext.getVisSessionDir(),images.getTemplateName()+"_thumb" +"."+JPG_NAME_EXT);
        String relFile= VisContext.replaceWithUsersBaseDirPrefix(f);

        if (!justName) new PlotOutput(plot).writeThumbnail(f,PlotOutput.JPEG);

        PlotImages.ThumbURL tn= new PlotImages.ThumbURL(relFile,plot.getScreenWidth(),plot.getScreenHeight());
        images.setThumbnail(tn);

        plotGroup.setZoomTo(saveZLevel);
    }


    static void writeThumbnail(ImagePlot plot, File f, int thumbnailSize) throws IOException, FitsException {
        ImagePlot tPlot= (ImagePlot)plot.makeSharedDataPlot();
        int div= Math.max( plot.getPlotGroup().getGroupImageWidth(), plot.getPlotGroup().getGroupImageHeight() );

        tPlot.getPlotGroup().setZoomTo(thumbnailSize/(float)div);

        int ext= f.getName().endsWith(JPG_NAME_EXT) ? PlotOutput.JPEG : PlotOutput.PNG;

        new PlotOutput(tPlot).writeThumbnail(f,ext);
        tPlot.freeResources();
    }

    static PlotImages writeImageTiles(File      imagefileDir,
                                      String    root,
                                      ImagePlot plot,
                                      boolean   fullScreen,
                                      int tileCnt) throws IOException {

        PlotOutput po= new PlotOutput(plot);
        List<PlotOutput.TileFileInfo> results;
        int outType= (plot.getPlotGroup().getZoomFact()<.55) ? PlotOutput.JPEG : PlotOutput.PNG;
        if (fullScreen) {
            results= po.writeTilesFullScreen(imagefileDir, root,PlotOutput.PNG, tileCnt>0);
        }
        else {
            results= po.writeTiles(imagefileDir, root,outType,tileCnt);
        }
        PlotImages images= new PlotImages(root,results.size(), plot.getScreenWidth(), plot.getScreenHeight(), plot.getZoomFactor());
        PlotImages.ImageURL imageURL;
        String relFile;
        int idx= 0;
        for(PlotOutput.TileFileInfo info : results) {
            relFile= VisContext.replaceWithUsersBaseDirPrefix(info.getFile());
            imageURL= new PlotImages.ImageURL(relFile,
                                              info.getX(), info.getY(),
                                              info.getWidth(), info.getHeight(),
                                              idx++,
                                              info.isCreated());
            images.add(imageURL);
        }
        return images;
    }



    static PlotImages makeImageTilesNoCreation(File      imagefileDir,
                                               String    root,
                                               float     zfact,
                                               int       screenWidth,
                                               int       screenHeight) {

        List<PlotOutput.TileFileInfo> results;
        int outType= (zfact<.55) ? PlotOutput.JPEG : PlotOutput.PNG;
        results= PlotOutput.makeTilesNoCreation(imagefileDir, zfact, root,outType, screenWidth, screenHeight);
        PlotImages images= new PlotImages(root,results.size(), screenWidth, screenHeight, zfact);
        PlotImages.ImageURL imageurl;
        String relFile;
        int idx= 0;
        for(PlotOutput.TileFileInfo info : results) {
            relFile= VisContext.replaceWithUsersBaseDirPrefix(info.getFile());
            imageurl= new PlotImages.ImageURL(relFile,
                                              info.getX(), info.getY(),
                                              info.getWidth(), info.getHeight(),
                                              idx++, false);
            images.add(imageurl);
        }
        return images;
    }



    public static File createRotateNorthFile(File originalFile,
                                             FitsRead originalFR,
                                             CoordinateSys rotateNorthType) throws FitsException,
                                                                                   IOException,
                                                                                   GeomException {
        FitsRead northFR= null;
        if (rotateNorthType.equals(CoordinateSys.GALACTIC)) {
            northFR= FitsRead.createFitsReadNorthUpGalactic(originalFR);
        }
        else if (rotateNorthType.equals(CoordinateSys.EQ_J2000)){
            northFR= FitsRead.createFitsReadNorthUp(originalFR);
        }
        else {
            WebAssert.argTst(false, "only supports galactic and j2000");

        }
        String fname= originalFile.getName();
        File f= File.createTempFile(FileUtil.getBase(fname)+"-rot-north",
                                    "."+FileUtil.FITS,
                                    VisContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        ImagePlot.writeFile(stream, new FitsRead[]{northFR});
        FileUtil.silentClose(stream);
        return f;
    }


    public static File createRotatedFile(File originalFile, FitsRead originalFR, double angle) throws FitsException,
                                                                                                      IOException,
                                                                                                      GeomException {
        FitsRead rotateFR= FitsRead.createFitsReadRotated(originalFR, angle);
        String fname= originalFile.getName();
        String angleStr= String.format("%2f", angle);
        File f= File.createTempFile(FileUtil.getBase(fname)+"-rot-"+angleStr,
                                    "."+FileUtil.FITS,
                                    VisContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        ImagePlot.writeFile(stream, new FitsRead[]{rotateFR});
        FileUtil.silentClose(stream);
        return f;
    }

    public static File createFlipYFile(File originalFile, FitsRead originalFR) throws FitsException,
                                                                                      IOException,
                                                                                      GeomException {
        FitsRead rotateFR= FitsRead.createFitsReadFlipLR(originalFR);
        String fname= originalFile.getName();
        String base= FileUtil.getBase(fname);
        int idx= base.indexOf("-flip");
        if (idx>-1) base= base.substring(0,idx);
        File f= File.createTempFile(base+"-flip", "."+FileUtil.FITS,
                                    VisContext.getVisSessionDir());
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f), (int) FileUtil.MEG);
        ImagePlot.writeFile(stream, new FitsRead[]{rotateFR});
        FileUtil.silentClose(stream);
        return f;
    }


    public static void initRequestFromState(WebPlotRequest req, PlotState oldState, Band band) {
        req.setInitialRangeValues(oldState.getRangeValues(band));
        req.setInitialColorTable(oldState.getColorTableId());
        req.setInitialZoomLevel(oldState.getZoomLevel());
    }

    public static PlotState createInitializedState(WebPlotRequest req[], PlotState initializerState) {
        PlotState state= new PlotState(initializerState.isThreeColor());
        state.setContextString(PlotServUtils.makeAndCachePlotCtx());
        initState(state, req, initializerState);
        VisContext.getPlotCtx(state.getContextString()).setPlotState(state);
        for(Band band : initializerState.getBands()) {
            state.setOriginalFitsFileStr(initializerState.getOriginalFitsFileStr(band), band);
        }
        return state;
    }


    private static void initState(PlotState state, WebPlotRequest req[], PlotState initializerState) {
        Band bands[]= initializerState.getBands();
        Assert.argTst(req.length==bands.length,
                      "there must be the same number of WebPlotRequest as there are bands in the initializerState");
        for(int i= 0; (i<bands.length); i++) {
            state.setWebPlotRequest(req[i],bands[i]);
            state.setRangeValues(initializerState.getRangeValues(bands[i]), bands[i]);
        }
        state.setNewPlot(false);
        state.setColorTableId(initializerState.getColorTableId());
        state.setZoomLevel(initializerState.getZoomLevel());
    }

    public static long getTileModTime(String fname) {
        File f= VisContext.convertToFile(fname);
        return f.canRead() ? f.lastModified() : -1;
    }


    public static File createImageFile(String fname,
                                       PlotState state,
                                       int x,
                                       int y,
                                       int width,
                                       int height) throws IOException {

        File f= VisContext.convertToFile(fname);
        if (!f.canRead()) {
            f= createOneTile(state, f,x,y,width,height);
        }
        return f;
    }

    public static void writeFullImageFileToStream(OutputStream oStream, PlotState state) throws IOException {

        File f= getUniquePngFileName("imageDownload", VisContext.getVisSessionDir());
        createFullTile(state, f);
        FileUtil.writeFileToStream(f, oStream);
    }

    public static File createImageThumbnail(String fname,
                                            PlotState state,
                                            String ctxStr) throws IOException {


        try {
            File f= VisContext.convertToFile(fname);
            if (!f.canRead()) {
                PlotClientCtx ctx= VisServerOps.prepare(state);
                if (ctx!=null)  {
                    boolean revalidated= revalidatePlot(ctx);
                    if (revalidated) {
                        try {
                            writeThumbnail(ctx.getPlot(),f,state.getThumbnailSize());
                        } catch (FitsException e) {
                            IOException fe= new IOException("Could not create thumbnail for: " +ctxStr);
                            fe.initCause(e);
                            throw fe;
                        }
                    }
                    else {
                        throw new IOException("Could not find revalidate context for : " +ctxStr);
                    }
                }
                else {
                    throw new IOException("Could not find context for : " +ctxStr);
                }
            }
            return f;
        } catch (FailedRequestException e) {
            throw new IOException("Could not find context for : " +ctxStr, e);
        }

    }


    public static void writeFileToStream(File f, OutputStream oStream)
            throws IOException {
        if (!isValidForDownload(f)) {
            throw new IOException("file is not in a public directory");
        }
        _log.info("Downloading fits file: "+ f.getPath());
        statsLog("download-fits", "fsize(MB)", (float)f.length()/ StringUtils.MEG , "fname", f.getPath());
        FileUtil.writeFileToStream(f,oStream);
    }


    public static boolean isValidForDownload(File f) {
        return (VisContext.convertToFile(f.getPath())!=null);
    }

    static File createFullTile(PlotState state, File f) throws IOException {
        return  createFullTile(state, f, null, null, null, null);
    }

    static File createFullTile(PlotState state, File f,
                               List<FixedObjectGroup> fog,
                               List<VectorObject> vectorList,
                               List<ScalableObjectPosition> scaleList,
                               GridLayer gridLayer) throws IOException {
        return  createOneTile(state,f,0,0,PLOT_FULL_WIDTH,PLOT_FULL_HEIGHT,
                              fog,vectorList, scaleList, gridLayer);
    }

    static File createFullTile(ImagePlot plot,
                               File f,
                               List<FixedObjectGroup> fog,
                               List<VectorObject> vectorList,
                               List<ScalableObjectPosition> scaleList,
                               GridLayer gridLayer) throws IOException {
        return  createOneTile(plot,f,0,0,PLOT_FULL_WIDTH,PLOT_FULL_HEIGHT,
                              fog,vectorList, scaleList, gridLayer);
    }



    static File createOneTile(PlotState state, File f, int x, int y, int width, int height) throws IOException {
        return createOneTile(state,f,x,y,width,height,null,null,null,null);
    }

    static File createOneTile(PlotState state,
                              File f,
                              int x,
                              int y,
                              int width,
                              int height,
                              List<FixedObjectGroup> fogList,
                              List<VectorObject> vectorList,
                              List<ScalableObjectPosition> scaleList,
                              GridLayer gridLayer)
            throws IOException {

        try {
            PlotClientCtx ctx= VisServerOps.prepare(state);
            if (ctx!=null)  {
                boolean revalidated= revalidatePlot(ctx);
                if (revalidated) {
                    return createOneTile(ctx.getPlot(),f,x,y,width,height,fogList,vectorList,scaleList,gridLayer);
                }
                else {
                    throw new IOException("Could not find revalidate context for : " +state.getContextString());
                }
            }
            else {
                throw new IOException("Could not find context for : " +state.getContextString());
            }
        } catch (FailedRequestException e) {
            throw new IOException("Could not find context for : " +state.getContextString(), e);
        }

    }

    static File createOneTile(ImagePlot plot,
                              File f,
                              int x,
                              int y,
                              int width,
                              int height,
                              List<FixedObjectGroup> fogList,
                              List<VectorObject> vectorList,
                              List<ScalableObjectPosition> scaleList,
                              GridLayer gridLayer) throws IOException {

        PlotOutput po= new PlotOutput(plot);
        if (fogList!=null) po.setFixedObjectGroupList(fogList);
        if (gridLayer!=null) po.setGridLayer(gridLayer);
        if (vectorList!=null) po.setVectorList(vectorList);
        if (scaleList!=null) po.setScaleList(scaleList);
        int ext= f.getName().endsWith(JPG_NAME_EXT) ? PlotOutput.JPEG : PlotOutput.PNG;
        if (width== PLOT_FULL_WIDTH) width= plot.getScreenWidth();
        if (height== PLOT_FULL_HEIGHT) height= plot.getScreenHeight();

        po.writeTile(f, ext, x, y, width, height, null);
        return f;

    }
    static boolean confirmFileData(PlotClientCtx ctx)  {
        boolean retval= true;
        try {
            PlotState state= ctx.getPlotState();

            for(Band band : state.getBands()) {
                if (!VisContext.getWorkingFitsFile(state,band).canRead() ||
                    !VisContext.getOriginalFile(state,band).canRead()) {
                    retval= false;
                    break;
                }
            }
        } catch (Exception e) {
            retval= false;
        }
        ctx.updateAccessTime();
        return retval;
    }


    static String makeTileBase(PlotState state) {
        File f= null;
        String fName= state.getOriginalFitsFileStr(state.firstBand());
        if (fName!=null)  f= VisContext.convertToFile(fName);
        String baseStr= null;
        if (f!=null) {
            baseStr= FileUtil.getBase(f);
        }
        else if (state.isThreeColor()) {
            baseStr= "Blank-3color-nobands";
        }
        else if (isBlank(state)) {
            baseStr= "Blank";
        }
        return  baseStr +"-"+ state.getContextString() +"-"+state.serialize().hashCode();
    }

    public static void updateProgress(ProgressStat pStat) {
        Cache cache= UserCache.getInstance();
        if (pStat.getId()!=null) cache.put(new StringKey(pStat.getId()), pStat);
    }

    public static void updateProgress(String key, ProgressStat.PType type, String progressMsg) {
        if (key!=null) {
            updateProgress(new ProgressStat(key,type,progressMsg));
        }

    }

    public static void updateProgress(WebPlotRequest r, ProgressStat.PType type, String progressMsg) {
        if (r!=null) {
            String key= r.getProgressKey();
            if (key!=null) updateProgress(new ProgressStat(key,type,progressMsg));
        }
    }

    public static Header getTopFitsHeader(File f) {
        Header header= null;
        try {
            Fits fits= new Fits(f);
            header=  fits.getHDU(0).getHeader();
            fits.getStream().close();
        } catch (FitsException e) {
            // quite fail
        } catch (IOException e) {
            // quite fail
        }
        return header;
    }


    private static String getServiceDateHeaderKey(WebPlotRequest.ServiceType sType) {
        String header= "none";
        switch (sType) {
            case TWOMASS:
                header= "ORDATE";
                break;
            case DSS:
                header= "DATE-OBS";
                break;
            case WISE:
                header= "MIDOBS";
                break;
            case SDSS:
                header= "DATE-OBS";
                break;
            case IRIS:
                header= "DATEIRIS";
                break;
        }
        return header;
    }

    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, File f) {
        Header header=  getTopFitsHeader(f);
        if (header!=null) {
            return getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header);
        }
        else {
            return "";
        }
    }

    public static String getDateValueFromServiceFits(WebPlotRequest.ServiceType sType, Header header) {
        return getDateValueFromServiceFits(getServiceDateHeaderKey(sType), header);
    }

    public static String getDateValueFromServiceFits(String headerKey, Header header) {
        long currentYear = Math.round(Math.floor(System.currentTimeMillis()/1000/3600/24/365.25) +1970);
        long year;
        String dateValue= header.getStringValue(headerKey);
        if (headerKey.equals("ORDATE")) {
            if (dateValue.length()>5) {
                dateValue= dateValue.subSequence(0,2)+"-"+dateValue.subSequence(2,4)+"-"+
                        dateValue.subSequence(4,6);
                year = 2000+Integer.parseInt(dateValue.subSequence(0,2).toString());
                if (year > currentYear) {
                    dateValue = "19"+dateValue;
                } else {
                    dateValue = "20"+dateValue;
                }
            }
        } else if (headerKey.equals("DATE-OBS")) {
            dateValue = dateValue.split("T")[0];
            if (dateValue.contains("/")) {
                String newDate = "";
                for (String v: dateValue.split("/")) {
                    if (newDate.length()==0) {
                        newDate = v;
                    } else {
                        newDate = v + "-" + newDate;
                    }
                }
                year = 2000+Integer.parseInt(newDate.subSequence(0,2).toString());
                if (year > currentYear) {
                    dateValue = "19"+newDate;
                } else {
                    dateValue = "20"+newDate;
                }
            }
        } else if (headerKey.equals("MIDOBS")) {
            dateValue = dateValue.split("T")[0];
        } else if (headerKey.equals("DATEIRIS")) {
            dateValue = "1983";
        }
        return dateValue;
    }

    public enum RevalidateSource { WORKING, ORIGINAL}

    static boolean revalidatePlot(PlotClientCtx ctx)  {
        return revalidatePlot(ctx,RevalidateSource.WORKING,false);
    }

    static boolean revalidatePlot(PlotClientCtx ctx, RevalidateSource source, boolean force)  {
        boolean retval= true;
        synchronized (ctx)  { // keep the test from happening at the same time with this ctx
            try {
                ImagePlot plot= ctx.getPlot();
                if (plot==null || force) {

                    PlotState state= ctx.getPlotState();
                    long start= System.currentTimeMillis();
                    boolean first= true;
                    StringBuilder lenStr= new StringBuilder(30);
                    for(Band band : state.getBands()) {
                        if (lenStr.length()>0) lenStr.append(", ");
                        int bandIdx= (band==Band.NO_BAND) ? ImagePlot.NO_BAND : band.getIdx();
                        File fitsFile=  (source==RevalidateSource.WORKING) ?
                                                 VisContext.getWorkingFitsFile(state,band) :
                                                 VisContext.getOriginalFile(state,band);

                        boolean blank= isBlank(state,band);

                        if (fitsFile.canRead() || blank) {
                            VisContext.purgeOtherPlots(state);
                            FitsRead fr[];
                            if (blank) fr= createBlankFITS(state.getWebPlotRequest(band));
                            else       fr= readFits(fitsFile);
                            RangeValues rv= state.getRangeValues(band);
                            int imageIdx= state.getImageIdx(band);
                            if (first) {
                                plot= makeImagePlot(fr[imageIdx],
                                                    state.getZoomLevel(),
                                                    state.isThreeColor(),
                                                    state.getColorTableId(), rv);
                                plot.getPlotGroup().setZoomTo(state.getZoomLevel());
                                if (state.isThreeColor()) plot.setThreeColorBand(fr[imageIdx],bandIdx);
                                ctx.setPlot(plot);
                                first= false;
                            }
                            else {
                                plot.addThreeColorBand(fr[imageIdx],bandIdx);
                                plot.getHistogramOps(bandIdx).recomputeStretch(rv);
                            }
                            Counters.getInstance().incrementVis("Revalidate");
                            lenStr.append(FileUtil.getSizeAsString(fitsFile.length()));
                        }
                    }
                    ctx.setPlot(plot);
                    plot.getPlotGroup().setZoomTo(state.getZoomLevel());
                    retval= true;
                    String sizeStr= (state.isThreeColor() ? ", 3 Color: file sizes: " : ", file size: ");
                    long elapse= System.currentTimeMillis()-start;
                    _log.info("revalidation success: "+ ctx.getKey(),
                              "time: " + UTCTimeUtil.getHMSFromMills(elapse)+
                                      sizeStr +   lenStr);
                }
            } catch (Exception e) {
                _log.warn(e,"revalidation failed: this should rarely happen: ",
                          e.toString());
                retval= false;
            }
            ctx.updateAccessTime();

        }
        return retval;
    }

    public static boolean isBlank(PlotState state) {
        return isBlank(state,state.firstBand());
    }

    public static boolean isBlank(PlotState state, Band band) {
        boolean retval= false;
        if (band==null || (state.getWorkingFitsFileStr(band)==null && state.getOriginalFitsFileStr(band)==null)) {
            WebPlotRequest req= state.getWebPlotRequest(band);
            retval= (req.getRequestType()== RequestType.BLANK);
        }
        return retval;
    }


    static File createHistImage(JComponent c,
                                Icon hd,
                                File dir,
                                Color bgColor,
                                String fname) throws IOException {
        int w= c.getWidth();
        int h= c.getHeight();
        BufferedImage cHistImage     = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2= cHistImage.createGraphics();
        g2.setColor(bgColor);
        g2.fillRect(0, 0, w, h);
        hd.paintIcon(c, g2, 0, 0);
        File f= new File(dir,fname);
        writeImage(cHistImage, f);
        return f;
    }

    static void writeImage(BufferedImage image, File f) throws IOException {
        OutputStream chistOut= new BufferedOutputStream( new FileOutputStream(f),4096);
        Iterator writers = ImageIO.getImageWritersByFormatName("png");
        ImageWriter writer = (ImageWriter)writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(chistOut);
        writer.setOutput(ios);
        ImageWriteParam param= writer.getDefaultWriteParam();
        param.setDestinationType(new ImageTypeSpecifier(image));
        writer.write(image);
    }



    static void statsLog(String function, Object... sAry) {
        _statsLog.stats(function, sAry);
    }

    public static File findWorkingFitsName(File f) {
        String ext= FileUtil.getExtension(f);
        File retval= f;
        if (ext!=null && ext.equalsIgnoreCase(FileUtil.GZ)) {
            retval= new File(VisContext.getVisSessionDir(), FileUtil.getBase(f.getName()));
        }
        return retval;
    }

      static FitsRead [] readFits(File fitsFile) throws FitsException,
                                                        FailedRequestException,
                                                        IOException {
        Fits fits= new Fits(fitsFile.getPath());
        FitsRead fr[];
        try {
            fr = FitsRead.createFitsReadArray(fits);
        } finally {
            fits.getStream().close();
        }
        return fr;
    }


    public static int cnvtBand(Band band) {
        return (band== Band.NO_BAND) ? ImagePlot.NO_BAND : band.getIdx();
    }

    public static Band cnvtBand(int bIdx) {
        Band retval;
        switch (bIdx) {
            case ImagePlot.NO_BAND :  retval= Band.NO_BAND; break;
            case ImagePlot.RED     :  retval= Band.RED; break;
            case ImagePlot.GREEN   :  retval= Band.GREEN; break;
            case ImagePlot.BLUE    :  retval= Band.BLUE; break;
            default : retval= Band.NO_BAND; break;
        }
        return retval;
    }

    public static String makeAndCachePlotCtx() {
        PlotClientCtx ctx= new PlotClientCtx();
        VisContext.putPlotCtx(ctx);
        return ctx.getKey();
    }

    static void setPixelAccessInfo(ImagePlot plot, PlotState state) {
        if (plot.isThreeColor()) {
            if (plot.isColorBandInUse(ImagePlot.RED)) {
                setPixelAccessInfoBand(plot,state, Band.RED);
            }
            if (plot.isColorBandInUse(ImagePlot.GREEN)) {
                setPixelAccessInfoBand(plot,state, Band.GREEN);
            }
            if (plot.isColorBandInUse(ImagePlot.BLUE)) {
                setPixelAccessInfoBand(plot,state, Band.BLUE);
            }
        }
        else {
            setPixelAccessInfoBand(plot,state, Band.NO_BAND);
        }
    }

    static void setPixelAccessInfoBand(ImagePlot plot,
                                       PlotState state,
                                       Band band) {


        FitsRead resultFr= plot.getHistogramOps(cnvtBand(band)).getFitsRead();
        ImageHeader ih= resultFr.getImageHeader();
        state.setFitsHeader(ih.makeMiniHeader(), band);
    }


     static File getUniquePngFileName(String nameBase, File dir) {
        File f= new File(dir,nameBase + "-" + _nameCnt +"-"+ _hostname+ _pngNameExt);
        f= FileUtil.createUniqueFileFromFile(f);
        _nameCnt++;
        return f;
    }

    static ImagePlot makeImagePlot(FitsRead  fr,
                                   float     initialZoomLevel,
                                   boolean   threeColor,
                                   int       initColorID,
                                   RangeValues stretch) throws FitsException {
        return new ImagePlot(null, fr,initialZoomLevel, threeColor,
                            initColorID, stretch,true);
    }


    public static String convertZoomToString(float level) {
        String retval;
        int zfInt= (int)(level*10000);

        if      (zfInt>=10000) retval= ((int)level)+"x";
        else if (zfInt==312)   retval= "1/32x";
        else if (zfInt==625)   retval= "1/16x";
        else if (zfInt==1250)  retval= "1/8x";
        else if (zfInt==2500)  retval= "1/4x";
        else if (zfInt==5000)  retval= "1/2x";
        else                   retval= String.format("%.3fx", level);

        return retval;
    }

    public static Circle getRequestArea(WebPlotRequest request) {
        Circle retval = null;
        WorldPt wp= request.getWorldPt();

        if (wp==null && request.containsParam(WebPlotRequest.OBJECT_NAME)) {
            String objName= request.getObjectName();
            if (!StringUtils.isEmpty(objName)) {
                try {
                    wp= TargetNetwork.resolveToWorldPt(objName, request.getResolver());
                } catch (Exception e) {
                    wp= null;
                }
            }
        }

        float side = request.getSizeInDeg();
        if (wp != null) retval = new Circle(wp, side);

        return retval;
    }

    public static FitsRead[] createBlankFITS(WebPlotRequest r) throws FailedRequestException, IOException {
        FitsRead retval[];
        Circle c=PlotServUtils.getRequestArea(r);
        if (c!=null) {
            int w= r.getBlankPlotWidth();
            int h= r.getBlankPlotHeight();
            if (w>0 && h>0) {
                float asScale= r.getBlankArcsecPerPix();
                if (asScale>0) {
                    double degScale= asScale/3600.0;
                    Fits blankFits = BlankFITS.createBlankFITS(c.getCenter(), w, h, degScale);
                    try {
                        retval = FitsRead.createFitsReadArray(blankFits);
                    } catch (FitsException e) {
                        throw new FailedRequestException("Could not create blank image", "FITS read could not be created",e);
                    } finally {
                        if (blankFits.getStream()!=null) blankFits.getStream().close();
                    }
                }
                else {
                    throw new FailedRequestException("Blank image BlankArcsecPerPix must be greater than 0.");
                }
            }
            else {
                throw new FailedRequestException("Blank image width and height must both be greater than 0.");
            }
        }
        else {
            throw new FailedRequestException("Blank image requires a center position");
        }
        return retval;
    }
}

