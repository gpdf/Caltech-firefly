package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.*;
import nom.tam.fits.ImageData;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.Cursor;
import edu.caltech.ipac.util.Assert;
import java.util.Date;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
/**
 * Lijun Zhang
 * 02/06/15
 * Refactor this class
 * Change all data type to float no matter what the bitpix value is
 *
 */
public class FitsRead implements Serializable {
    //class variable
    private static RangeValues DEFAULT_RANGE_VALUE = new RangeValues();

    static {
        FitsFactory.setAllowTerminalJunk(true);
        FitsFactory.setUseHierarch(true);
    }

    private int planeNumber;
    private int extension_number;
    private byte[] pixeldata;
    private int[] pixelhist = new int[256];
    private float[] float1d;
    private Fits fits;
    private ImageHeader imageHeader;
    private Header header;
    private BasicHDU hdu;
    private int bitpix;
    private Histogram hist = null;
    private double blankValue;
    private RangeValues rangeValues = (RangeValues) DEFAULT_RANGE_VALUE.clone();
    private int imageScaleFactor = 1;
    private int indexInFile = -1;  // -1 unknown, >=0 index in file
    private String srcDesc = null;
    private double slow = 0.0;
    private double shigh = 0.0;
    private ImageHDU imageHdu; //add this for using the old stretch methods //TODO remvoe it later
    private static ArrayList<Integer>  SUPPORTED_BIT_PIXS = new ArrayList<Integer>(Arrays.asList(8, 16, 32, -32, -64));

    /** a private constructor for image Fits file
     * @param fits
     * @param imageHdu
     * @throws FitsException
     */
    private FitsRead(Fits fits, ImageHDU imageHdu) throws FitsException {

        //assign some instant variables
        this.fits = fits;
        hdu = imageHdu;
        this.imageHdu=imageHdu;
        header = imageHdu.getHeader();
        planeNumber = header.getIntValue("SPOT_PL", 0);
        extension_number = header.getIntValue("SPOT_EXT", -1);
        checkHeader();
        long HDUOffset = getHDUOffset(imageHdu);
        imageHeader = new ImageHeader(header, HDUOffset, planeNumber);
        blankValue = imageHeader.blank_value;
        bitpix = imageHeader.bitpix;
        if (!SUPPORTED_BIT_PIXS.contains(new Integer(bitpix))){
            System.out.println("Unimplemented bitpix = " + bitpix);
        }
        //get the data and store into float array
        float1d = getImageHDUDataInFloatArray(this.fits, imageHdu);

    }


    /**
     * Flip an image left to right so that pixels read backwards
     * @param aFitsReader FitsReadLZ object for the input image
     * @return FitsReadLZ object for the new, flipped image
     */

    public static FitsRead createFitsReadFlipLR(FitsRead aFitsReader)
            throws FitsException, GeomException {
        FlipLR flipLr = new FlipLR();
        return (flipLr.do_flip(aFitsReader));
    }

    /**
     * Rotate an image so that Equatorial North is up in the new image
     *
     * @param fitsReader FitsReadLZ object for the input image
     * @return FitsReadLZ object for the new, rotated image
     */

    public static FitsRead createFitsReadNorthUp(FitsRead fitsReader)
            throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(fitsReader, 0.0, CoordinateSys.EQ_J2000));
    }

    /**
     * Rotate an image so that Galactic North is up in the new image
     *
     * @param aFitsReader FitsReadLZ object for the input image
     * @return FitsReadLZ object for the new, rotated image
     */

    public static FitsRead createFitsReadNorthUpGalactic(FitsRead aFitsReader)
            throws FitsException, IOException, GeomException {
        return (createFitsReadPositionAngle(aFitsReader, 0.0, CoordinateSys.GALACTIC));
    }

    /**
     * Rotate an image by a specified amount
     *
     * @param fitsReader             FitsReadLZ object for the input image
     * @param rotationAngle number of degrees to rotate the image counter-clockwise
     * @return FitsReadLZ object for the new, rotated image
     */
    public static FitsRead createFitsReadRotated(FitsRead fitsReader, double rotationAngle)
            throws FitsException, IOException, GeomException {

        ImageHeader imageHeader = fitsReader.getImageHeader();

        CoordinateSys inCoordinateSys = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);
        Projection projection = imageHeader.createProjection(inCoordinateSys);

        double centerX = (imageHeader.naxis1 + 1.0) / 2.0;
        double centerY = (imageHeader.naxis2 + 1.0) / 2.0;

        try {
            WorldPt worldPt1 = projection.getWorldCoords(centerX, centerY - 1);
            WorldPt worldPt2 = projection.getWorldCoords(centerX, centerY);
            double positionAngle = -VisUtil.getPositionAngle(worldPt1.getX(),
                    worldPt1.getY(), worldPt2.getX(), worldPt2.getY());

            positionAngle += rotationAngle;
            return (createFitsReadPositionAngle(fitsReader, positionAngle, CoordinateSys.EQ_J2000));
        } catch (ProjectionException pe) {
            if (SUTDebug.isDebug()) {
                System.out.println("got ProjectionException: " + pe.getMessage());
            }
            throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
        }

    }

    public static FitsRead createFitsReadWithGeom(FitsRead aFitsRead,
                                                  FitsRead aRefFitsRead,
                                                  boolean aDoscale) throws
            FitsException,
            IOException,
            GeomException {

        //update the input aFitsRead only if the aRefFitsRead is not null
        if (aRefFitsRead !=null) {
            ImageHeader refHeader = aRefFitsRead.getImageHeader();
            Geom geom = new Geom();
            //geom.override_naxis1=0;
            geom.n_override_naxis1 = aDoscale;

            ImageHeader imageHeader = geom.open_in(aFitsRead);
            double primCdelt1 = Math.abs(imageHeader.cdelt1);
            double refCdelt1 = Math.abs(refHeader.cdelt1);
            int imageScaleFactor = 1;
            boolean shouldScale = 2 * refCdelt1 < primCdelt1;
            if (aDoscale && shouldScale) {
                imageScaleFactor = (int) (primCdelt1 / refCdelt1);
                geom.override_cdelt1 = refHeader.cdelt1 * imageScaleFactor;
                geom.n_override_cdelt1 = true;
                geom.override_cdelt2 = refHeader.cdelt2 * imageScaleFactor;
                geom.n_override_cdelt2 = true;
                if (refHeader.using_cd) {
                    geom.override_CD1_1 = refHeader.cd1_1 * imageScaleFactor;
                    geom.override_CD1_2 = refHeader.cd1_2 * imageScaleFactor;
                    geom.override_CD2_1 = refHeader.cd2_1 * imageScaleFactor;
                    geom.override_CD2_2 = refHeader.cd2_2 * imageScaleFactor;
                    geom.n_override_CDmatrix = true;
                }

                geom.crpix1_base = refHeader.crpix1;
                geom.crpix2_base = refHeader.crpix2;
                geom.imageScaleFactor = imageScaleFactor;
                geom.need_crpix_adjusted = true;
                if (SUTDebug.isDebug()) {
                    System.out.println(
                            "RBH ready for do_geom:  imageScaleFactor = "
                                    + imageScaleFactor + "  geom = " + geom);
                }
            }

            //make a copy of the reference  fits
            Fits modFits = geom.do_geom(aRefFitsRead);

            FitsRead[] fitsReadArray = createFitsReadArray(modFits);
            aFitsRead = fitsReadArray[0];
            aFitsRead.imageScaleFactor = imageScaleFactor;

        }
        return aFitsRead;
    }

    /**
     * Get the world point location
     * @param imageHeader
     * @param aCoordinateSys
     * @return
     * @throws FitsException
     */
    private static WorldPt getWorldPt( ImageHeader imageHeader, CoordinateSys aCoordinateSys) throws FitsException {
        CoordinateSys inCoordinateSys = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);
        Projection proj = imageHeader.createProjection(inCoordinateSys);


        double centerX = (imageHeader.naxis1 + 1.0) / 2.0;
        double centerY = (imageHeader.naxis2 + 1.0) / 2.0;

        WorldPt worldPt;
        try {
            worldPt = proj.getWorldCoords(centerX - 1, centerY - 1);

        } catch (ProjectionException pe) {
            if (SUTDebug.isDebug()) {
                System.out.println("got ProjectionException: " + pe.getMessage());
            }
            throw new FitsException("Could not rotate image.\n -  got ProjectionException: " + pe.getMessage());
        }


        if (!aCoordinateSys.equals(imageHeader.getCoordSys())) {
            worldPt = Plot.convert(worldPt, aCoordinateSys);
        }
        return worldPt;
    }

    /**
     * The input refHeader will be modified and new keys/values are added
     * @param imageHeader
     * @param refHeader
     * @param aPositionAngle
     * @param aCoordinateSys
     * @throws FitsException
     */
    private static void updateRefHeader(ImageHeader imageHeader, Header refHeader,
                                        double aPositionAngle, CoordinateSys aCoordinateSys)
            throws FitsException {


        refHeader.addValue("CDELT1", -Math.abs(imageHeader.cdelt1), "");
        refHeader.addValue("CDELT2", Math.abs(imageHeader.cdelt2), "");
        refHeader.addValue("CRPIX1", imageHeader.naxis1 / 2, "");
        refHeader.addValue("CRPIX2", imageHeader.naxis2 / 2, "");
        refHeader.addValue("CROTA2", aPositionAngle, "");
        if (aCoordinateSys.equals(CoordinateSys.EQ_J2000)) {
            refHeader.addValue("CTYPE1", "RA---TAN", "");
            refHeader.addValue("CTYPE2", "DEC--TAN", "");
            refHeader.addValue("EQUINOX", 2000.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.EQ_B1950)) {
            refHeader.addValue("CTYPE1", "RA---TAN", "");
            refHeader.addValue("CTYPE2", "DEC--TAN", "");
            refHeader.addValue("EQUINOX", 1950.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.ECL_J2000)) {
            refHeader.addValue("CTYPE1", "ELON-TAN", "");
            refHeader.addValue("CTYPE2", "ELAT-TAN", "");
            refHeader.addValue("EQUINOX", 2000.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.ECL_B1950)) {
            refHeader.addValue("CTYPE1", "ELON-TAN", "");
            refHeader.addValue("CTYPE2", "ELAT-TAN", "");
            refHeader.addValue("EQUINOX", 1950.0, "");
        } else if (aCoordinateSys.equals(CoordinateSys.GALACTIC)) {
            refHeader.addValue("CTYPE1", "GLON-TAN", "");
            refHeader.addValue("CTYPE2", "GLAT-TAN", "");
        } else {
            throw new FitsException("Could not rotate image.\n -  unrecognized coordinate system");
        }
    }

    /**
     * a new reference header is created
     * @param geom
     * @param fitsRead
     * @param positionAngle
     * @param coordinateSys
     * @return
     * @throws FitsException
     * @throws IOException
     * @throws GeomException
     */
    private static Header getRefHeader(Geom geom, FitsRead fitsRead, double positionAngle,
                                       CoordinateSys coordinateSys)
            throws FitsException, IOException, GeomException {

        ImageHeader imageHeader = geom.open_in(fitsRead);  // throws GeomException
	   /* new try - create a Fits with CDELTs and CROTA2, discarding */
       /* CD matrix, PLATE projection stuff, and SIP corrections */
        Header refHeader = new Header();
        refHeader.setSimple(true);
        refHeader.setNaxes(2);
        /* values for cropped.fits */
        refHeader.setBitpix(16);  // ignored - geom sets it to -32
        refHeader.setNaxis(1, imageHeader.naxis1);
        refHeader.setNaxis(2, imageHeader.naxis2);
        geom.n_override_naxis1 = true;  // make geom recalculate NAXISn
    /*
        pixel at center of object
	    18398  DN at RA = 60.208423  Dec = -89.889959
	    pixel one up
	    18398  DN at RA = 59.995226  Dec = -89.889724
	    (a distance of 0.028349 arcmin or 0.00047248 degrees)
	*/

        //get the world point worldPt based on the imageHeader and aCoordinatesSys
        WorldPt worldPt = getWorldPt( imageHeader, coordinateSys);

        refHeader.addValue("CRVAL1", worldPt.getX(), "");
        refHeader.addValue("CRVAL2", worldPt.getY(), "");

        updateRefHeader(imageHeader, refHeader,positionAngle, coordinateSys);

        return refHeader;
    }
    /**
     * Rotate an image so that North is at the specified position angle in the new image
     *
     * @param fitsRead             FitsReadLZ object for the input image
     * @param positionAngle desired position angle in degrees
     * @param coordinateSys      desired coordinate system for output image
     * @return FitsReadLZ object for the new, rotated image
     */
    public static FitsRead createFitsReadPositionAngle(FitsRead fitsRead, double positionAngle,
                                                       CoordinateSys coordinateSys)
            throws FitsException, IOException, GeomException {

        Geom geom = new Geom();
        Header refHeader = getRefHeader(geom, fitsRead, positionAngle, coordinateSys);

        //create a ImageHDU with the null data
        ImageHDU refHDU = new ImageHDU(refHeader, null);
        Fits refFits = new Fits();
        refFits.addHDU(refHDU);

        refFits = geom.do_geom(refFits);  // throws GeomException
        FitsRead[] fitsReadArray = createFitsReadArray(refFits);
        fitsRead = fitsReadArray[0];
        return fitsRead;
    }

    private static boolean getImageCondition(Header  aHeader){

        int naxis = aHeader.getIntValue("NAXIS", -1);
        boolean goodImage = true;
        if (naxis == 0) {
            goodImage = false;
        } else {
            for (int i = 1; i <= naxis; i++) {
                int naxisValue = aHeader.getIntValue("NAXIS" + i, -1);

                if (naxisValue == 0) {
                    goodImage = false;
                    break;
                }
            }
        }
        return goodImage;
    }
    //TODO this method may not needed, test it to decide if it can be removed
    /**
     *
     * @param header
     * @return
     */
    private  static Cursor getHeaderEndPosition(Header header){
        String key = null;
        Cursor iter = header.iterator();
        HeaderCard card;
        for (int i = 0; iter.hasNext(); i++) {
            card = (HeaderCard) iter.next();
            key = card.getKey();

            // pass the cursor to pass the mandatory Fits Keywords
            if (key.startsWith("SIMPLE") || key.startsWith("XTENSION") ||
                    key.startsWith("PCOUNT") || key.startsWith("GCOUNT") ||
                    key.startsWith("BITPIX") || key.startsWith("NAXIS") ) {
                continue;
            }

            break;  //after the required keyword, break here
        }

        /* move past blank cards */
        while (key.length() == 0) {

            if (iter.hasNext()) {
                card = (HeaderCard) iter.next();
                key = card.getKey();
            }
        }
        return iter;
    }



    private static void updateHeader(Header header, BasicHDU HDU, int pos)
            throws FitsException {
        header.addLine( new HeaderCard(
                "SPOT_EXT", pos, "EXTENSION NUMBER (IN SPOT)"));

        header.addLine(new HeaderCard(
                "SPOT_OFF", HDU.getFileOffset(),
                "EXTENSION OFFSET (IN SPOT)"));
    }


    private static ArrayList<BasicHDU> getHDUList( BasicHDU[] HDUs) throws FitsException {
        ArrayList<BasicHDU> HDUList = new ArrayList<BasicHDU>();

        boolean hasExtension = HDUs.length>1? true:false;
        for (int j = 0; j < HDUs.length; j++) {
            if (!(HDUs[j] instanceof ImageHDU)) {
                continue;   //ignor non-image extensions
            }
            //process image HDU
            Header header = HDUs[j].getHeader();
            if (header == null)
                throw new FitsException("Missing header in FITS file");


            int naxis = header.getIntValue("NAXIS", -1);
            boolean goodImage = getImageCondition(header);

            if (goodImage) {
                if ( hasExtension ) { // update this hdu by adding keywords/values
                    updateHeader(header, HDUs[j], j);
                }

                int naxis3 = header.getIntValue("NAXIS3", -1);
                if ((naxis > 2) && (naxis3 > 1)) { //it is a cube data
                    if (SUTDebug.isDebug())
                        System.out.println("GOT A FITS CUBE");
                    BasicHDU[] splitHDUs = splitFitsCube(HDUs[j]);
                    /* for each plane of cube */
                    for (int jj = 0; jj < splitHDUs.length; jj++)
                        HDUList.add(splitHDUs[jj]);
                } else {
                    HDUList.add(HDUs[j]);
                }
            }

            //when the header is added to the new fits file, the card number could be increased if the header is a primary
            header.resetOriginalSize();

        } //end j loop
        return HDUList;
    }

    /**
     * read a fits with extensions or cube data
     * @param fits
     * @return
     * @throws FitsException
     */
    public static FitsRead[] createFitsReadArray(Fits fits)
            throws FitsException {

        //get all the Header Data Unit from the fits file
        BasicHDU[] HDUs = fits.read();
        // boolean hasExtension = HDUs.length>1? true:false;

        if (HDUs == null) {
            // Error: file doesn't seem to have any HDUs!
            throw new FitsException("Bad format in FITS file");
        }

        ArrayList<BasicHDU> HDUList = getHDUList(HDUs);

        if (HDUList.size() == 0)
            throw new FitsException("No image headers in FITS file");

        FitsRead[] fitsReadAry = new FitsRead[HDUList.size()];
        for (int i = 0; i < HDUList.size(); i++) {
            fitsReadAry[i] = new FitsRead(fits, (ImageHDU) HDUList.get(i));
            fitsReadAry[i].indexInFile = i;
        }

        return fitsReadAry;
    }


    private static BasicHDU[] splitFitsCube(BasicHDU hdu)
            throws FitsException {

        Header header = hdu.getHeader();
        int bitpix = header.getIntValue("BITPIX", -1);

        if (!SUPPORTED_BIT_PIXS.contains(new Integer(bitpix))){
            System.out.println("Unimplemented bitpix = " + bitpix);
        }


        int naxis3 = header.getIntValue("NAXIS3", 0);
        float[][][] data32 = (float[][][]) ArrayFuncs.convertArray(hdu.getData().getData(), Float.TYPE);

        BasicHDU[] retval = new BasicHDU[naxis3];
        for (int i = 0; i < naxis3; i++) {
            Header newHeader = cloneHeader(header);

            ImageData newImageData =new ImageData(data32[i]);
            retval[i] = new ImageHDU(newHeader,newImageData );
            retval[i].addValue("SPOT_PL", i + 1, "PLANE OF FITS CUBE (IN SPOT)");

            newHeader.resetOriginalSize();
        }

        return retval;
    }



    static Header cloneHeader(Header header) {
        // first collect cards from old header
        Cursor iter = header.iterator();
        String cards[] = new String[header.getNumberOfCards()];
        int i = 0;
        while (iter.hasNext()) {
            HeaderCard card = (HeaderCard) iter.next();
            //System.out.println("RBH card.toString() = " + card.toString());
            cards[i] = card.toString();
            i++;
        }
        return (new Header(cards));
    }

    public static RangeValues getDefaultFutureStretch() {
        return DEFAULT_RANGE_VALUE;
    }

    public static void setDefaultFutureStretch(RangeValues defaultRangeValues) {
        DEFAULT_RANGE_VALUE = defaultRangeValues;
    }

    /**
     * Creates a new ImageHDU given the original HDU and the new array of pixels
     * The new header part reflects the 2-dim float data
     * The new data part contains the new pixels
     * Sets NAXISn according to the actual dimensions of pixels[][], which is
     * not necessarily the dimensions of the original image
     * @param hdu    ImageHDU for the open FITS file
     * @param pixels The 2-dim float array of new pixels
     * @return The new ImageHDU
     */
    public static ImageHDU makeHDU(ImageHDU hdu, float[][] pixels)
            throws FitsException {
        Header header = hdu.getHeader();

        // first clone the header
        Cursor iter = header.iterator();
        String cards[] = new String[header.getNumberOfCards()];
        HeaderCard card;
        int i = 0;
        while (iter.hasNext()) {
            card = (HeaderCard) iter.next();
            cards[i] = card.toString();
            i++;
        }
        Header new_header = new Header(cards);

        new_header.deleteKey("BITPIX");
        new_header.setBitpix(-32);
        new_header.deleteKey("NAXIS");
        new_header.setNaxes(2);
        new_header.deleteKey("NAXIS1");
        new_header.setNaxis(1, pixels[0].length);
        new_header.deleteKey("NAXIS2");
        new_header.setNaxis(2, pixels.length);

        new_header.deleteKey("DATAMAX");
        new_header.deleteKey("DATAMIN");
        new_header.deleteKey("NAXIS3");
        new_header.deleteKey("NAXIS4");
        new_header.deleteKey("BLANK");
        new_header.deleteKey("BSCALE");
        new_header.deleteKey("BZERO");

        ImageData new_image_data = new ImageData(pixels);
        hdu = new ImageHDU(new_header, new_image_data);
        return hdu;
    }


    private long getHDUOffset(ImageHDU image_hdu) {
        long HDU_offset;
        if (extension_number == -1) {
            HDU_offset = image_hdu.getFileOffset();
        } else {
            HDU_offset = header.getIntValue("SPOT_OFF", 0);
        }

        if (HDU_offset < 0) HDU_offset = 0;
        return HDU_offset;

    }

    private void checkHeader() throws FitsException {


        // now get SPOT planeNumber from FITS cube (zero if not from a cube)
        if (SUTDebug.isDebug())
            System.out.println("RBH fetched SPOT_PL: " + planeNumber);

        // now get SPOT extension_number from FITS header
        // -1 if the image had no extensions
        if (SUTDebug.isDebug())
            System.out.println("RBH fetched SPOT_EXT: " + extension_number);

        if (header == null)
            throw new FitsException("FITS file appears corrupt");


    }

    private float[] reversePixData(float[] float1d){

        int naxis1 = imageHeader.naxis1;
        int naxis2 = imageHeader.naxis2;
        if (imageHeader.cdelt2 < 0) {
            /* pixels are upside down - reverse them in y */
            float[] temp = new float[float1d.length];
            int index_src = 0;
            for (int y = 0; y < naxis2; y++) {

                int indexDest = (naxis2 - y - 1) * naxis1;
                for (int x = 0; x < naxis1; x++) {
                    temp[indexDest++] = float1d[index_src++];
                }
            }
            float1d = temp;
            imageHeader.cdelt2 = -imageHeader.cdelt2;
            imageHeader.crpix2 =
                    imageHeader.naxis2 - imageHeader.crpix2 + 1;

        }
        return float1d;
    }
    private float[] getImageHDUDataInFloatArray(Fits fits, ImageHDU image_hdu) throws FitsException {


        //convert data to float if the bitpix is not 32
        float[] float1d =
                (float[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(image_hdu.getData().getData(), Float.TYPE) );

        /* pixels are upside down - reverse them in y */
        if (imageHeader.cdelt2 < 0) float1d = reversePixData(float1d);


        return float1d;
    }

    /*========================================
     Everything related stretch has not touched yet.  I will work it in March
    */

    //TODO work on this later after reading the paper
    public synchronized void do_stretch_old(byte passedPixelData[], boolean mapBlankToZero,
                                            int start_pixel, int last_pixel, int start_line, int last_line) throws FitsException {

        double bscale, bzero;
        double datamax, datamin;
        byte blank_pixel_value;
        Zscale.ZscaleRetval zscale_retval = null;

        if (mapBlankToZero)
            blank_pixel_value = 0;
        else
            blank_pixel_value = (byte) 255;

        //System.out.println("RBH SPOT_EXT = " + extension_number);
        //System.out.println("RBH SPOT_PL = " + plane_number);
        pixeldata=  passedPixelData;
//        _rangeValues= newRangeValues;

        datamin = imageHeader.datamin;
        datamax = imageHeader.datamax;
        bscale = imageHeader.bscale;
        bzero = imageHeader.bzero;

        Object onedimdata = null;
        switch(bitpix)
        {
            case 32:
                onedimdata =
                        (int[]) ArrayFuncs.flatten(hdu.getData().getData());
                break;
            case 16:
                onedimdata =  (short[]) ArrayFuncs.flatten( hdu.getData().getData());
                break;
            case 8:
                onedimdata =  (byte[]) ArrayFuncs.flatten( hdu.getData().getData());
                break;
            case -32:
                onedimdata =  (float[]) ArrayFuncs.flatten( hdu.getData().getData());
                break;
            case -64:
                onedimdata =  (double[]) ArrayFuncs.flatten( hdu.getData().getData());
                break;
        }

        if (( rangeValues.getLowerWhich() == RangeValues.ZSCALE) ||
                (rangeValues.getUpperWhich() == RangeValues.ZSCALE))
        {
            if ((rangeValues.getLowerWhich() == RangeValues.ZSCALE) ||
                    (rangeValues.getUpperWhich() == RangeValues.ZSCALE))
            {
                double contrast = rangeValues.getZscaleContrast();
                int opt_size = rangeValues.getZscaleSamples();
		    /* desired number of pixels in sample */
                int len_stdline = rangeValues.getZscaleSamplesPerLine();
		    /* optimal number of pixels per line */
                zscale_retval = Zscale.cdl_zscale(onedimdata,
                        imageHeader.naxis1,  imageHeader.naxis2,
                        bitpix, contrast/100.0, opt_size, len_stdline,
                        imageHeader.blank_value,
                        imageHeader.bscale,
                        imageHeader.bzero);
            }

        }

        if (hist == null)
        {
            if (((rangeValues.getLowerWhich() != RangeValues.ABSOLUTE) &&
                    (rangeValues.getLowerWhich() != RangeValues.ZSCALE)) ||
                    ((rangeValues.getUpperWhich() != RangeValues.ABSOLUTE) &&
                            (rangeValues.getUpperWhich() != RangeValues.ZSCALE)))
            {
                /* do histogram only if needed */
                computeHistogram();
            }
        }


        switch (rangeValues.getLowerWhich())
        {
            case RangeValues.ABSOLUTE:
                slow = (rangeValues.getLowerValue() - bzero) / bscale;
                break;
            case RangeValues.PERCENTAGE:
                slow = hist.get_pct(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.SIGMA:
                slow = hist.get_sigma(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.MAXMIN:
                slow = hist.get_pct(0.0, false);
                break;
            case RangeValues.ZSCALE:
                slow = zscale_retval.getZ1();
                break;
            default:
                Assert.tst(false, "illegal _rangeValues.getLowerWhich()");
        }
        switch (rangeValues.getUpperWhich())
        {
            case RangeValues.ABSOLUTE:
                shigh = (rangeValues.getUpperValue() - bzero) / bscale;
                break;
            case RangeValues.PERCENTAGE:
                shigh = hist.get_pct(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.SIGMA:
                shigh = hist.get_sigma(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.MAXMIN:
                shigh = hist.get_pct(100.0, true);
                break;
            case RangeValues.ZSCALE:
                shigh = zscale_retval.getZ2();
                break;
            default:
                Assert.tst(false, "illegal _rangeValues.getUpperWhich()");
        }

        if (SUTDebug.isDebug())
        {
            System.out.println("slow = " + slow + "    shigh = " + shigh +
                    "   bitpix = " + bitpix);
            if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LINEAR)
                System.out.println("stretching STRETCH_LINEAR");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LOG)
                System.out.println("stretching STRETCH_LOG");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LOGLOG)
                System.out.println("stretching STRETCH_LOGLOG");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_EQUAL)
                System.out.println("stretching STRETCH_EQUAL");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_SQUARED)
                System.out.println("stretching STRETCH_SQUARED");
        }

        stretch_pixels(start_pixel, last_pixel, start_line, last_line,
                bitpix, imageHeader.naxis1, blank_pixel_value,
                onedimdata, pixeldata, pixelhist);

        //byte[] glop = getHistColors();  // RBH DEBUG
    }



    public synchronized void do_stretch(byte passedPixelData[], boolean mapBlankToZero,
                                        int start_pixel, int last_pixel, int start_line, int last_line) {

        double bscale, bzero;
        double datamax, datamin;
        byte blank_pixel_value;
        Zscale.ZscaleRetval zscale_retval = null;

        if (mapBlankToZero)
            blank_pixel_value = 0;
        else
            blank_pixel_value = (byte) 255;

        //System.out.println("RBH SPOT_EXT = " + extension_number);
        //System.out.println("RBH SPOT_PL = " + planeNumber);
        pixeldata = passedPixelData;
//        rangeValues= newRangeValues;

        datamin = imageHeader.datamin;
        datamax = imageHeader.datamax;
        bscale = imageHeader.bscale;
        bzero = imageHeader.bzero;

        Object onedimdata = null;

        switch(bitpix)
        {
            case 32:
                onedimdata =
                        (int[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(float1d, Integer.TYPE));
                break;
            case 16:
                onedimdata =  (short[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(float1d, Short.TYPE));
                break;
            case 8:
                onedimdata =  (byte[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(float1d, Byte.TYPE));
                break;
            case -32:
                onedimdata =  float1d;
                break;
            case -64:
                onedimdata =  (double[]) ArrayFuncs.flatten(ArrayFuncs.convertArray(float1d, Double.TYPE));
                break;
        }

        if ((rangeValues.getLowerWhich() == RangeValues.ZSCALE) ||
                (rangeValues.getUpperWhich() == RangeValues.ZSCALE))
            if ((rangeValues.getLowerWhich() == RangeValues.ZSCALE) ||
                    (rangeValues.getUpperWhich() == RangeValues.ZSCALE)) {
                double contrast = rangeValues.getZscaleContrast();
                int opt_size = rangeValues.getZscaleSamples();

                int len_stdline = rangeValues.getZscaleSamplesPerLine();

                zscale_retval = Zscale.cdl_zscale(onedimdata,
                        imageHeader.naxis1, imageHeader.naxis2,
                        bitpix, contrast / 100.0, opt_size, len_stdline,
                        imageHeader.blank_value,
                        imageHeader.bscale,
                        imageHeader.bzero);
            }

        if (hist == null) {
            if (((rangeValues.getLowerWhich() != RangeValues.ABSOLUTE) &&
                    (rangeValues.getLowerWhich() != RangeValues.ZSCALE)) ||
                    ((rangeValues.getUpperWhich() != RangeValues.ABSOLUTE) &&
                            (rangeValues.getUpperWhich() != RangeValues.ZSCALE))) {

                computeHistogram();
            }
        }


        switch (rangeValues.getLowerWhich()) {
            case RangeValues.ABSOLUTE:
                slow = (rangeValues.getLowerValue() - bzero) / bscale;
                break;
            case RangeValues.PERCENTAGE:
                slow = hist.get_pct(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.SIGMA:
                slow = hist.get_sigma(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.MAXMIN:
                slow = hist.get_pct(0.0, false);
                break;
            case RangeValues.ZSCALE:
                slow = zscale_retval.getZ1();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getLowerWhich()");
        }
        switch (rangeValues.getUpperWhich()) {
            case RangeValues.ABSOLUTE:
                shigh = (rangeValues.getUpperValue() - bzero) / bscale;
                break;
            case RangeValues.PERCENTAGE:
                shigh = hist.get_pct(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.SIGMA:
                shigh = hist.get_sigma(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.MAXMIN:
                shigh = hist.get_pct(100.0, true);
                break;
            case RangeValues.ZSCALE:
                shigh = zscale_retval.getZ2();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getUpperWhich()");
        }

        if (SUTDebug.isDebug()) {
            System.out.println("slow = " + slow + "    shigh = " + shigh +
                    "   bitpix = " + bitpix);
            if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LINEAR)
                System.out.println("stretching STRETCH_LINEAR");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LOG)
                System.out.println("stretching STRETCH_LOG");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LOGLOG)
                System.out.println("stretching STRETCH_LOGLOG");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_EQUAL)
                System.out.println("stretching STRETCH_EQUAL");
            else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_SQUARED)
                System.out.println("stretching STRETCH_SQUARED");
        }

        stretch_pixels(start_pixel, last_pixel, start_line, last_line,
                bitpix, imageHeader.naxis1, blank_pixel_value,
                onedimdata, pixeldata, pixelhist);

        //byte[] glop = getHistColors();  // RBH DEBUG
    }

    void computeHistogram() {
        double datamin = imageHeader.datamin;
        double datamax = imageHeader.datamax;
        double bscale = imageHeader.bscale;
        double bzero = imageHeader.bzero;

        hist = new Histogram(float1d, (datamin - bzero) / bscale,
                (datamax - bzero) / bscale,
                blankValue);

    }

    private int[] getNoneLinearTblInt() {

        int[] tbl = new int[256];
        if
                ((rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LOG) ||
                (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG)) {
            double sdiff = shigh - slow;
            if (sdiff == 0.)
                sdiff = 1.;

            for (int j = 0; j < 255; ++j) {
                double atbl = Math.pow(10., j / 254.0);
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG) {
                    atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                }
                double floati = (atbl - 1.) / 9. * sdiff + slow;
                if (-floati > Integer.MAX_VALUE)
                    tbl[j] = -Integer.MAX_VALUE;
                else if (floati > Integer.MAX_VALUE)
                    tbl[j] = Integer.MAX_VALUE;
                else
                    tbl[j] = (int) floati;

                //System.out.println("tbl["+ j + "] = " + tbl[j]);
            }
            tbl[255] = Integer.MAX_VALUE;
        } else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_EQUAL) {
            hist.eq_tbl(tbl);
        } else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_SQUARED) {
            squared_tbl(tbl, slow, shigh);
        } else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_SQRT) {
            sqrt_tbl(tbl, slow, shigh);
        }


        return tbl;

    }


    //TODO work it later after implementing stretch
    private int[] getTblByte() {

        int[] tbl1 = new int[256];
        int[] tbl = new int[256];
        if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LINEAR) {
            double sdiff = shigh - slow;
            for (int j = 0; j < 255; j++) {
                tbl1[j] = (int) ((254 / sdiff) * (j - slow));
                if (tbl1[j] < 0)
                    tbl1[j] = 0;
                if (tbl1[j] > 254)
                    tbl1[j] = 254;
            }
            tbl1[255] = Integer.MAX_VALUE;
        } else if
                ((rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOG) ||
                        (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)) {
            double sdiff = shigh - slow;
            if (sdiff == 0.)
                sdiff = 1.;
            for (int j = 0; j < 255; ++j) {
                if (j <= slow)
                    tbl1[j] = 0;
                else if (j >= shigh)
                    tbl1[j] = 254;
                else {
                    if (rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_LOG) {
                        tbl1[j] = (int) (254 *
                                .43429 * Math.log((9 * (j - slow) / sdiff) + 1));
                            /* .43429 changes from natural log to common log */
                    } else {
                            /* LOGLOG */
                        double atbl = .43429 * Math.log((9 * (j - slow) / sdiff) + 1);
                        tbl1[j] = (int)
                                (254 * .43429 * Math.log((9.0 * atbl) + 1));
                    }

                }


                //System.out.println("tbl1["+ j + "] = " + tbl1[j]);
            }
            tbl1[255] = 254;
        } else if (
                (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_EQUAL) ||
                        (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED) ||
                        (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQRT)) {
            if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_EQUAL) {
                hist.eq_tbl(tbl);
            } else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_SQUARED) {
                squared_tbl(tbl, slow, shigh);
            } else if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_SQRT) {
                sqrt_tbl(tbl, slow, shigh);
            }

                /* now interpolate */
            int last_val = -1;
            for (int j = 0; j <= 255; j++) {
                int this_val = tbl[j];
                if (this_val < 0)
                    this_val = 0;
                else if (this_val > 254)
                    this_val = 254;
                for (int i = last_val + 1; i <= this_val; i++) {
                    tbl1[i] = j;
                }
                last_val = this_val;
            }
            for (int i = last_val + 1; i <= 255; i++)
                tbl1[i] = 255;
        }
        return tbl1;
    }
    //TODO work it later after implementing stretch
    private double[] getDbtlDouble(double sdiff) {

        double dtbl[] = new double[256];

        if ((rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_LOG) ||
                (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG)) {

            for (int j = 0; j < 255; ++j) {
                double atbl = Math.pow(10., j / 254.0);
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOGLOG) {
                    atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                }
                dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;

                //System.out.println("dtbl["+ j + "] = " + dtbl[j]);
            }
            dtbl[255] = Double.MAX_VALUE;
        } else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_EQUAL) {
            hist.deq_tbl(dtbl);
        } else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_SQUARED) {
            squared_tbl_dbl(dtbl, slow, shigh);
        } else if (rangeValues.getStretchAlgorithm() ==
                RangeValues.STRETCH_SQRT) {
            sqrt_tbl_dbl(dtbl, slow, shigh);
        }

        return dtbl;
    }
    //TODO work it later after implementing stretch
    /**
     * @param start_pixel
     * @param last_pixel
     * @param start_line
     * @param last_line
     * @param naxis1
     * @param blank_pixel_value
     * @param onedimdata
     * @param pixeldata
     * @param pixelhist
     * @param tblArray
     * @param bitpix
     * @param sdiff
     */
    private void calculateDataArrays(int start_pixel, int last_pixel,
                                     int start_line, int last_line, int naxis1,
                                     byte blank_pixel_value,
                                     Object onedimdata,
                                     byte[] pixeldata, int[] pixelhist, Object tblArray, int bitpix, double sdiff) {


        int delta, deltasav;

        if (sdiff >= 0)
            deltasav = 64;
        else
            deltasav = -64;

        float[] onedimdata32 = (float[]) onedimdata;
        //double[] tbl = (double[]) tblArray;
        int[] tblInt = (int[]) tblArray;
        double[] tbl = new double[tblInt.length];
        if (!tblArray.getClass().getSimpleName().equalsIgnoreCase("Double")){

            for (int i=0; i<tblInt.length; i++){
                tbl[i]= (double) tblInt[i];
            }
        }
        else{
            tbl = (double[]) tblArray;
        }
        int i = 0;
        for (int line = start_line; line <= last_line; line++) {
            int start_index = line * naxis1 + start_pixel;
            int last_index = line * naxis1 + last_pixel;
            for (int index = start_index; index <= last_index; index++) {

                // stretch each pixel
                if (onedimdata32[index] == blankValue || Double.isNaN(float1d[index])) {
                    pixeldata[i] = blank_pixel_value;
                } else if ((bitpix == -32 || bitpix == -64) && rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR) {
                    double runval = ((float1d[index] - slow) * 254 / sdiff);
                    if (runval < 0)
                        pixeldata[i] = 0;
                    else if (runval > 254)
                        pixeldata[i] = (byte) 254;
                    else
                        pixeldata[i] = (byte) runval;
                } else {
                    double runval = onedimdata32[index];

                    int pixval = 128;
                    delta = deltasav;

                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] < runval)
                        pixval += delta;
                    else
                        pixval -= delta;
                    delta >>= 1;
                    if (tbl[pixval] >= runval)
                        pixval -= 1;

                    pixeldata[i] = (byte) pixval;
                    pixeldata[i] = rangeValues.computeBiasAndContrast(pixeldata[i]);
                    pixelhist[pixeldata[i] & 0xff]++;
                }
                i++;
            }
        }
    }

    //TODO write this one later
    private void stretch_pixels_old(int start_pixel, int last_pixel,
                                    int start_line, int last_line, int bitpix, int naxis1,
                                    byte blank_pixel_value,
                                    Object onedimdata,
                                    byte[] pixeldata, int[] pixelhist)
    {
        int pixval;
        int i;
        double sdiff;
        int runval;
        int delta, deltasav;
        double floati, d_runval;
        double atbl;
        int tbl[] = new int[256];;
        int tbl1[] = new int[256];;
        int this_val, last_val;
        double dtbl[] = new double[256];

        sdiff = shigh - slow;

        for (i = 0; i < 255; i++)
            pixelhist[i] = 0;

        switch (bitpix)
        {
            case 32:
                long start_time = (new Date()).getTime();
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR)
                {
                    linear_tbl(tbl, slow, shigh);
                }
                else if
                        ((rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOG) ||
                                (rangeValues.getStretchAlgorithm() ==
                                        RangeValues.STRETCH_LOGLOG))
                {
                    sdiff = shigh - slow;
                    if(sdiff == 0.)
                        sdiff = 1.;
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        floati = (atbl - 1.) / 9. * sdiff + slow;
                        if  (-floati > Integer.MAX_VALUE)
                            tbl[j] = -Integer.MAX_VALUE;
                        else if (floati > Integer.MAX_VALUE)
                            tbl[j] =     Integer.MAX_VALUE;
                        else
                            tbl[j] = (int) floati;

                        //System.out.println("tbl["+ j + "] = " + tbl[j]);
                    }
                    tbl[255] = Integer.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.eq_tbl(tbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl(tbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl(tbl, slow, shigh);
                }


                if (sdiff >= 0)
                    deltasav = 64;
                else
                    deltasav = - 64;

                int[] onedimdata32 = (int[]) onedimdata;

                //pixeldata = new byte[onedimdata32.length];
                //Assert.tst(pixeldata.length >= onedimdata32.length);

                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdata32.length; i++)

                    for (int index= start_index; index <= last_index; index++)
                    {

                        // stretch each pixel
                        if (onedimdata32[index] == blankValue)
                            pixeldata[i] = blank_pixel_value;
                        else
                        {
                            runval = onedimdata32[index];
                            pixval = 128;
                            delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] >= runval)
                                pixval -= 1;

                            pixeldata[i] = (byte) pixval;
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);
                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;
                    }
                }
                //System.out.println("RBH ELAPSED TIME = " +
                //    ((new Date()).getTime() - start_time) + " ms");
                break;
            case 16:
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR)
                {
                    sdiff = shigh - slow;
                    for (int j=0; j<255; j++)
                    {
                        floati = (sdiff / 254) * j + slow;
                        if  (-floati > Integer.MAX_VALUE)
                            tbl[j] = -Integer.MAX_VALUE;
                        else if (floati > Integer.MAX_VALUE)
                            tbl[j] =     Integer.MAX_VALUE;
                        else
                            tbl[j] = (int) floati;
                    }
                    tbl[255] = Integer.MAX_VALUE;
                }
                else if
                        ((rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOG) ||
                                (rangeValues.getStretchAlgorithm() ==
                                        RangeValues.STRETCH_LOGLOG))
                {
                    sdiff = shigh - slow;
                    if(sdiff == 0.)
                        sdiff = 1.;
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        floati = (atbl - 1.) / 9. * sdiff + slow;
                        if  (-floati > Integer.MAX_VALUE)
                            tbl[j] = -Integer.MAX_VALUE;
                        else if (floati > Integer.MAX_VALUE)
                            tbl[j] =     Integer.MAX_VALUE;
                        else
                            tbl[j] = (int) floati;

                        //System.out.println("tbl["+ j + "] = " + tbl[j]);
                    }
                    tbl[255] = Integer.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.eq_tbl(tbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl(tbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl(tbl, slow, shigh);
                }


                if (sdiff >= 0)
                    deltasav = 64;
                else
                    deltasav = - 64;

                short[] onedimdata16 = (short[]) onedimdata;


                //pixeldata = new byte[onedimdata16.length];
                //Assert.tst(pixeldata.length >= onedimdata16.length);

                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdata16.length; i++)
                    for (int index= start_index; index <= last_index; index++)
                    {
                        // stretch each pixel
                        if (onedimdata16[index] == blankValue)
                            pixeldata[i] = blank_pixel_value;
                        else
                        {
                            runval = onedimdata16[index];
                            pixval = 128;
                            delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] >= runval)
                                pixval -= 1;

                            pixeldata[i] = (byte) pixval;
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);
                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;


                    }
                }
                break;
            case 8:
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR)
                {
                    sdiff = shigh - slow;
                    for (int j=0; j<255; j++)
                    {
                        tbl1[j] = (int) ((254 / sdiff) * (j - slow));
                        if (tbl1[j] < 0)
                            tbl1[j] = 0;
                        if (tbl1[j] > 254)
                            tbl1[j] = 254;
                    }
                    tbl1[255] = Integer.MAX_VALUE;
                }
                else if
                        ((rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOG) ||
                                (rangeValues.getStretchAlgorithm() ==
                                        RangeValues.STRETCH_LOGLOG))
                {
                    sdiff = shigh - slow;
                    if(sdiff == 0.)
                        sdiff = 1.;
                    for (int j=0; j<255; ++j)
                    {
                        if (j <= slow)
                            tbl1[j] = 0;
                        else if (j >= shigh)
                            tbl1[j] = 254;
                        else
                        {
                            if (rangeValues.getStretchAlgorithm() ==
                                    RangeValues.STRETCH_LOG)
                            {
                                tbl1[j] = (int) (254 *
                                        .43429 * Math.log((9 * (j - slow) / sdiff) + 1));
                            /* .43429 changes from natural log to common log */
                            }
                            else
                            {
                            /* LOGLOG */
                                atbl = .43429 * Math.log((9 * (j - slow) / sdiff) + 1);
                                tbl1[j] = (int)
                                        (254 * .43429 * Math.log((9.0 * atbl) + 1));
                            }

                        }


                        //System.out.println("tbl1["+ j + "] = " + tbl1[j]);
                    }
                    tbl1[255] = 254;
                }
                else if (
                        (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_EQUAL) ||
                                (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED) ||
                                (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQRT) )
                {
                    if (rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_EQUAL)
                    {
                        hist.eq_tbl(tbl);
                    }
                    else if (rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_SQUARED)
                    {
                        squared_tbl(tbl, slow, shigh);
                    }
                    else if (rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_SQRT)
                    {
                        sqrt_tbl(tbl, slow, shigh);
                    }

                /* now interpolate */
                    last_val = -1;
                    for (int j = 0; j <= 255; j++)
                    {
                        this_val = tbl[j];
                        if (this_val < 0)
                            this_val = 0;
                        else if (this_val > 254)
                            this_val = 254;
                        for (i = last_val+1; i <= this_val; i++)
                        {
                            tbl1[i] = j;
                        }
                        last_val = this_val;
                    }
                    for (i = last_val+1; i <= 255; i++)
                        tbl1[i] = 255;
                }

                byte[] onedimdata8 = (byte[]) onedimdata;

                //pixeldata = new byte[onedimdata8.length];
                //Assert.tst(pixeldata.length >= onedimdata8.length);
                sdiff = shigh - slow;

                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdata8.length; i++)
                    for (int index= start_index; index <= last_index; index++)
                    {
                        // stretch each pixel
                        pixval = onedimdata8[index] & 0xff;
                        if (pixval == blankValue)
                            pixeldata[i] = blank_pixel_value;
                        else
                        {
                            if (pixval > shigh)
                                pixeldata[i] = (byte) 254;
                            else if (pixval < slow)
                                pixeldata[i] = (byte) 0;
                            else
                                pixeldata[i] = (byte) tbl1[pixval];
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);

                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;

                    }
                }
                break;
            case -32:
                sdiff = shigh - slow;
                if(sdiff == 0.)
                    sdiff = 1.;
                if
                        ((rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOG) ||
                        (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG))
                {
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;

                        //System.out.println("dtbl["+ j + "] = " + dtbl[j]);
                    }
                    dtbl[255] = Double.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.deq_tbl(dtbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl_dbl(dtbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl_dbl(dtbl, slow, shigh);
                }

                if (sdiff > 0)
                    deltasav = 64;
                else
                    deltasav = - 64;


                float[] onedimdatam32 = (float[]) onedimdata;

                //pixeldata = new byte[onedimdatam32.length];
                //System.out.println("RBH pixeldata.length = " + pixeldata.length +
                //    "   onedimdatam32.length = " + onedimdatam32.length);
                //Assert.tst(pixeldata.length >= onedimdatam32.length);
                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdatam32.length; i++)

                    for (int index= start_index; index <= last_index; index++)

                    {
                        // stretch each pixel
                        if (Double.isNaN(onedimdatam32[index]))
                        {
                            pixeldata[i] = blank_pixel_value;
                        }
                        else
                        {
                            if (rangeValues.getStretchAlgorithm() ==
                                    RangeValues.STRETCH_LINEAR)
                            {
                                d_runval = ((onedimdatam32[index] - slow ) * 254 / sdiff);
                                if (d_runval < 0)
                                    pixeldata[i] = 0;
                                else if (d_runval > 254)
                                    pixeldata[i] = (byte) 254;
                                else
                                    pixeldata[i] = (byte) d_runval;
                            }
                            else
                            {
                                d_runval = onedimdatam32[index];
                                pixval = 128;
                                delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] >= d_runval)
                                    pixval -= 1;

                                pixeldata[i] = (byte) pixval;

                            }
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);
                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;

                    }
                }
                break;
            case -64:
                sdiff = shigh - slow;
                if(sdiff == 0.)
                    sdiff = 1.;
                if
                        ((rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOG) ||
                        (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG))
                {
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;

                        //System.out.println("dtbl["+ j + "] = " + dtbl[j]);
                    }
                    dtbl[255] = Double.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.deq_tbl(dtbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl_dbl(dtbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl_dbl(dtbl, slow, shigh);
                }

                if (sdiff > 0)
                    deltasav = 64;
                else
                    deltasav = - 64;


                double[] onedimdatam64 = (double[]) onedimdata;
                //pixeldata = new byte[onedimdatam64.length];
                //Assert.tst(pixeldata.length >= onedimdatam64.length);
                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdatam64.length; i++)

                    for (int index= start_index; index <= last_index; index++)
                    {
                        // stretch each pixel
                        if (Double.isNaN(onedimdatam64[index]))
                        {
                            pixeldata[i] = blank_pixel_value;
                        }
                        else
                        {
                            if (rangeValues.getStretchAlgorithm() ==
                                    RangeValues.STRETCH_LINEAR)
                            {
                                d_runval = ((onedimdatam64[index] - slow ) * 254 / sdiff);
                                if (d_runval < 0)
                                    pixeldata[i] = 0;
                                else if (d_runval > 254)
                                    pixeldata[i] = (byte) 254;
                                else
                                    pixeldata[i] = (byte) d_runval;
                            }
                            else
                            {
                                d_runval = onedimdatam64[index];
                                pixval = 128;
                                delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] >= d_runval)
                                    pixval -= 1;

                                pixeldata[i] = (byte) pixval;

                            }
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);

                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;

                    }
                }
                break;
        }
    }

    /* pixeldata and pixelhist are return values */
    private void stretch_pixels(int start_pixel, int last_pixel,
                                int start_line, int last_line, int bitpix, int naxis1,
                                byte blank_pixel_value,
                                Object onedimdata,
                                byte[] pixeldata, int[] pixelhist) {

        int tbl[] = new int[256];
        int pixval;
        int i;
        int runval;
        int delta, deltasav;
        double floati, d_runval;
        double atbl;

        int tbl1[] = new int[256];;
        int this_val, last_val;
        double dtbl[] = new double[256];

        double sdiff = shigh - slow;

        for (i = 0; i < 255; i++)
            pixelhist[i] = 0;

        switch (bitpix)
        {
            case 32:
                long start_time = (new Date()).getTime();
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR)
                {
                    linear_tbl(tbl, slow, shigh);
                }
                else if
                        ((rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOG) ||
                                (rangeValues.getStretchAlgorithm() ==
                                        RangeValues.STRETCH_LOGLOG))
                {
                    sdiff = shigh - slow;
                    if(sdiff == 0.)
                        sdiff = 1.;
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        floati = (atbl - 1.) / 9. * sdiff + slow;
                        if  (-floati > Integer.MAX_VALUE)
                            tbl[j] = -Integer.MAX_VALUE;
                        else if (floati > Integer.MAX_VALUE)
                            tbl[j] =     Integer.MAX_VALUE;
                        else
                            tbl[j] = (int) floati;

                        //System.out.println("tbl["+ j + "] = " + tbl[j]);
                    }
                    tbl[255] = Integer.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.eq_tbl(tbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl(tbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl(tbl, slow, shigh);
                }


                if (sdiff >= 0)
                    deltasav = 64;
                else
                    deltasav = - 64;

                int[] onedimdata32 = (int[]) onedimdata;

                //pixeldata = new byte[onedimdata32.length];
                //Assert.tst(pixeldata.length >= onedimdata32.length);

                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdata32.length; i++)

                    for (int index= start_index; index <= last_index; index++)
                    {

                        // stretch each pixel
                        if (onedimdata32[index] == blankValue)
                            pixeldata[i] = blank_pixel_value;
                        else
                        {
                            runval = onedimdata32[index];
                            pixval = 128;
                            delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] >= runval)
                                pixval -= 1;

                            pixeldata[i] = (byte) pixval;
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);
                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;
                    }
                }
                //System.out.println("RBH ELAPSED TIME = " +
                //    ((new Date()).getTime() - start_time) + " ms");
                break;
            case 16:
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR)
                {
                    sdiff = shigh - slow;
                    for (int j=0; j<255; j++)
                    {
                        floati = (sdiff / 254) * j + slow;
                        if  (-floati > Integer.MAX_VALUE)
                            tbl[j] = -Integer.MAX_VALUE;
                        else if (floati > Integer.MAX_VALUE)
                            tbl[j] =     Integer.MAX_VALUE;
                        else
                            tbl[j] = (int) floati;
                    }
                    tbl[255] = Integer.MAX_VALUE;
                }
                else if
                        ((rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOG) ||
                                (rangeValues.getStretchAlgorithm() ==
                                        RangeValues.STRETCH_LOGLOG))
                {
                    sdiff = shigh - slow;
                    if(sdiff == 0.)
                        sdiff = 1.;
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        floati = (atbl - 1.) / 9. * sdiff + slow;
                        if  (-floati > Integer.MAX_VALUE)
                            tbl[j] = -Integer.MAX_VALUE;
                        else if (floati > Integer.MAX_VALUE)
                            tbl[j] =     Integer.MAX_VALUE;
                        else
                            tbl[j] = (int) floati;

                        //System.out.println("tbl["+ j + "] = " + tbl[j]);
                    }
                    tbl[255] = Integer.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.eq_tbl(tbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl(tbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl(tbl, slow, shigh);
                }


                if (sdiff >= 0)
                    deltasav = 64;
                else
                    deltasav = - 64;

                short[] onedimdata16 = (short[]) onedimdata;


                //pixeldata = new byte[onedimdata16.length];
                //Assert.tst(pixeldata.length >= onedimdata16.length);

                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdata16.length; i++)
                    for (int index= start_index; index <= last_index; index++)
                    {
                        // stretch each pixel
                        if (onedimdata16[index] == blankValue)
                            pixeldata[i] = blank_pixel_value;
                        else
                        {
                            runval = onedimdata16[index];
                            pixval = 128;
                            delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] < runval)
                                pixval += delta;
                            else
                                pixval -= delta;
                            delta >>= 1;
                            if (tbl[pixval] >= runval)
                                pixval -= 1;

                            pixeldata[i] = (byte) pixval;
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);
                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;


                    }
                }
                break;
            case 8:
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR)
                {
                    sdiff = shigh - slow;
                    for (int j=0; j<255; j++)
                    {
                        tbl1[j] = (int) ((254 / sdiff) * (j - slow));
                        if (tbl1[j] < 0)
                            tbl1[j] = 0;
                        if (tbl1[j] > 254)
                            tbl1[j] = 254;
                    }
                    tbl1[255] = Integer.MAX_VALUE;
                }
                else if
                        ((rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOG) ||
                                (rangeValues.getStretchAlgorithm() ==
                                        RangeValues.STRETCH_LOGLOG))
                {
                    sdiff = shigh - slow;
                    if(sdiff == 0.)
                        sdiff = 1.;
                    for (int j=0; j<255; ++j)
                    {
                        if (j <= slow)
                            tbl1[j] = 0;
                        else if (j >= shigh)
                            tbl1[j] = 254;
                        else
                        {
                            if (rangeValues.getStretchAlgorithm() ==
                                    RangeValues.STRETCH_LOG)
                            {
                                tbl1[j] = (int) (254 *
                                        .43429 * Math.log((9 * (j - slow) / sdiff) + 1));
                            /* .43429 changes from natural log to common log */
                            }
                            else
                            {
                            /* LOGLOG */
                                atbl = .43429 * Math.log((9 * (j - slow) / sdiff) + 1);
                                tbl1[j] = (int)
                                        (254 * .43429 * Math.log((9.0 * atbl) + 1));
                            }

                        }


                        //System.out.println("tbl1["+ j + "] = " + tbl1[j]);
                    }
                    tbl1[255] = 254;
                }
                else if (
                        (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_EQUAL) ||
                                (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED) ||
                                (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQRT) )
                {
                    if (rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_EQUAL)
                    {
                        hist.eq_tbl(tbl);
                    }
                    else if (rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_SQUARED)
                    {
                        squared_tbl(tbl, slow, shigh);
                    }
                    else if (rangeValues.getStretchAlgorithm() ==
                            RangeValues.STRETCH_SQRT)
                    {
                        sqrt_tbl(tbl, slow, shigh);
                    }

                /* now interpolate */
                    last_val = -1;
                    for (int j = 0; j <= 255; j++)
                    {
                        this_val = tbl[j];
                        if (this_val < 0)
                            this_val = 0;
                        else if (this_val > 254)
                            this_val = 254;
                        for (i = last_val+1; i <= this_val; i++)
                        {
                            tbl1[i] = j;
                        }
                        last_val = this_val;
                    }
                    for (i = last_val+1; i <= 255; i++)
                        tbl1[i] = 255;
                }

                byte[] onedimdata8 = (byte[]) onedimdata;

                //pixeldata = new byte[onedimdata8.length];
                //Assert.tst(pixeldata.length >= onedimdata8.length);
                sdiff = shigh - slow;

                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdata8.length; i++)
                    for (int index= start_index; index <= last_index; index++)
                    {
                        // stretch each pixel
                        pixval = onedimdata8[index] & 0xff;
                        if (pixval == blankValue)
                            pixeldata[i] = blank_pixel_value;
                        else
                        {
                            if (pixval > shigh)
                                pixeldata[i] = (byte) 254;
                            else if (pixval < slow)
                                pixeldata[i] = (byte) 0;
                            else
                                pixeldata[i] = (byte) tbl1[pixval];
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);

                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;

                    }
                }
                break;
            case -32:
                sdiff = shigh - slow;
                if(sdiff == 0.)
                    sdiff = 1.;
                if
                        ((rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOG) ||
                        (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG))
                {
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;

                        //System.out.println("dtbl["+ j + "] = " + dtbl[j]);
                    }
                    dtbl[255] = Double.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.deq_tbl(dtbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl_dbl(dtbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl_dbl(dtbl, slow, shigh);
                }

                if (sdiff > 0)
                    deltasav = 64;
                else
                    deltasav = - 64;


                float[] onedimdatam32 = (float[]) onedimdata;

                //pixeldata = new byte[onedimdatam32.length];
                //System.out.println("RBH pixeldata.length = " + pixeldata.length +
                //    "   onedimdatam32.length = " + onedimdatam32.length);
                //Assert.tst(pixeldata.length >= onedimdatam32.length);
                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdatam32.length; i++)

                    for (int index= start_index; index <= last_index; index++)

                    {
                        // stretch each pixel
                        if (Double.isNaN(onedimdatam32[index]))
                        {
                            pixeldata[i] = blank_pixel_value;
                        }
                        else
                        {
                            if (rangeValues.getStretchAlgorithm() ==
                                    RangeValues.STRETCH_LINEAR)
                            {
                                d_runval = ((onedimdatam32[index] - slow ) * 254 / sdiff);
                                if (d_runval < 0)
                                    pixeldata[i] = 0;
                                else if (d_runval > 254)
                                    pixeldata[i] = (byte) 254;
                                else
                                    pixeldata[i] = (byte) d_runval;
                            }
                            else
                            {
                                d_runval = onedimdatam32[index];
                                pixval = 128;
                                delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] >= d_runval)
                                    pixval -= 1;

                                pixeldata[i] = (byte) pixval;

                            }
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);
                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;

                    }
                }
                break;
            case -64:
                sdiff = shigh - slow;
                if(sdiff == 0.)
                    sdiff = 1.;
                if
                        ((rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LOG) ||
                        (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG))
                {
                    for (int j=0; j<255; ++j)
                    {
                        atbl = Math.pow(10., j/254.0);
                        if (rangeValues.getStretchAlgorithm() ==
                                RangeValues.STRETCH_LOGLOG)
                        {
                            atbl = Math.pow(10., (atbl - 1.0) / 9.0);
                        }
                        dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;

                        //System.out.println("dtbl["+ j + "] = " + dtbl[j]);
                    }
                    dtbl[255] = Double.MAX_VALUE;
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_EQUAL)
                {
                    hist.deq_tbl(dtbl);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQUARED)
                {
                    squared_tbl_dbl(dtbl, slow, shigh);
                }
                else if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_SQRT)
                {
                    sqrt_tbl_dbl(dtbl, slow, shigh);
                }

                if (sdiff > 0)
                    deltasav = 64;
                else
                    deltasav = - 64;


                double[] onedimdatam64 = (double[]) onedimdata;
                //pixeldata = new byte[onedimdatam64.length];
                //Assert.tst(pixeldata.length >= onedimdatam64.length);
                i = 0;
                for (int line = start_line; line <= last_line; line++)
                {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;

                    //for (i = 0; i < onedimdatam64.length; i++)

                    for (int index= start_index; index <= last_index; index++)
                    {
                        // stretch each pixel
                        if (Double.isNaN(onedimdatam64[index]))
                        {
                            pixeldata[i] = blank_pixel_value;
                        }
                        else
                        {
                            if (rangeValues.getStretchAlgorithm() ==
                                    RangeValues.STRETCH_LINEAR)
                            {
                                d_runval = ((onedimdatam64[index] - slow ) * 254 / sdiff);
                                if (d_runval < 0)
                                    pixeldata[i] = 0;
                                else if (d_runval > 254)
                                    pixeldata[i] = (byte) 254;
                                else
                                    pixeldata[i] = (byte) d_runval;
                            }
                            else
                            {
                                d_runval = onedimdatam64[index];
                                pixval = 128;
                                delta = deltasav; /* 64 if ra normal, -64 if ra reversed */

                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] < d_runval)
                                    pixval += delta;
                                else
                                    pixval -= delta;
                                delta >>= 1;
                                if (dtbl[pixval] >= d_runval)
                                    pixval -= 1;

                                pixeldata[i] = (byte) pixval;

                            }
                            pixeldata[i]= rangeValues.computeBiasAndContrast(pixeldata[i]);

                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;

                    }
                }
                break;
        }



        /////
       /* switch (bitpix) {
            case 32:
                long start_time = (new Date()).getTime();
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR) {
                    linear_tbl(tbl, slow, shigh);
                } else {
                    tbl = getNoneLinearTblInt();
                }

                calculateDataArrays(start_pixel, last_pixel, start_line, last_line, naxis1, blank_pixel_value, onedimdata,
                        pixeldata, pixelhist, tbl, 32, sdiff);


                break;
            case 16:
                if (rangeValues.getStretchAlgorithm() ==
                        RangeValues.STRETCH_LINEAR) {
                    sdiff = shigh - slow;
                    for (int j = 0; j < 255; j++) {
                        double floati = (sdiff / 254) * j + slow;
                        if (-floati > Integer.MAX_VALUE)
                            tbl[j] = -Integer.MAX_VALUE;
                        else if (floati > Integer.MAX_VALUE)
                            tbl[j] = Integer.MAX_VALUE;
                        else
                            tbl[j] = (int) floati;
                    }
                    tbl[255] = Integer.MAX_VALUE;
                } else {
                    tbl = getNoneLinearTblInt();
                }

                calculateDataArrays(start_pixel, last_pixel, start_line, last_line, naxis1, blank_pixel_value, onedimdata,
                        pixeldata, pixelhist, tbl, 16, sdiff);

                break;
            case 8:

                int[] tbl1 = getTblByte();
                byte[] onedimdata8 = (byte[]) onedimdata;

                int i = 0;
                for (int line = start_line; line <= last_line; line++) {
                    int start_index = line * naxis1 + start_pixel;
                    int last_index = line * naxis1 + last_pixel;


                    for (int index = start_index; index <= last_index; index++) {
                        // stretch each pixel
                        int pixval = onedimdata8[index] & 0xff;
                        if (pixval == blankValue)
                            pixeldata[i] = blank_pixel_value;
                        else {
                            if (pixval > shigh)
                                pixeldata[i] = (byte) 254;
                            else if (pixval < slow)
                                pixeldata[i] = (byte) 0;
                            else
                                pixeldata[i] = (byte) tbl1[pixval];
                            pixeldata[i] = rangeValues.computeBiasAndContrast(pixeldata[i]);

                            pixelhist[pixeldata[i] & 0xff]++;
                        }
                        i++;

                    }
                }
                break;
            case -32:
                sdiff = shigh - slow;
                if (sdiff == 0.)
                    sdiff = 1.;

                double[] dtbl = getDbtlDouble(sdiff);


                calculateDataArrays(start_pixel, last_pixel, start_line, last_line, naxis1, blank_pixel_value, onedimdata,
                        pixeldata, pixelhist, dtbl, -32, sdiff);
                break;
            case -64:
                sdiff = shigh - slow;
                if (sdiff == 0.)
                    sdiff = 1.;
                dtbl = getDbtlDouble(sdiff);
                calculateDataArrays(start_pixel, last_pixel, start_line, last_line, naxis1, blank_pixel_value, onedimdata,
                        pixeldata, pixelhist, dtbl, -64, sdiff);
                break;
        }*/
    }


    /**
     * Return an array where each element corresponds to an element of
     * the histogram, and the value in each element is the screen pixel
     * value which would result from an image pixel which falls into that
     * histogram bin.
     *
     * @return array of byte (4096 elements)
     */
    public byte[] getHistColors()
    {
        int start_pixel = 0;
        int last_pixel = 4095;
        int start_line = 0;
        int last_line = 0;
        int bitpix = -64;
        int naxis1 = 1;
        byte blank_pixel_value = 0;
        byte[] pixeldata = new byte[4096];
        int[] pixelhist = new int[256];


	/*
	int[] onedimdata32;
	Object onedimdata = null;
	onedimdata = onedimdatam32;
	*/

        double[] hist_bin_values = new double[4096];
        for (int i = 0; i < 4096; i++)
        {
            hist_bin_values[i] = hist.getDNfromBin(i);
        }


        Object onedimdata = (Object) hist_bin_values;

        stretch_pixels(start_pixel, last_pixel, start_line, last_line,
                bitpix, naxis1, blank_pixel_value, onedimdata, pixeldata, pixelhist);

	/*
	for (int i = 0; i < 240; i++)
	{
	    int pixelDN = pixeldata[i] & 0xff;
	    System.out.println("hist_bin_values[" + i + "] = " +
		hist_bin_values[i]  + "  -> pixval = " + pixelDN);
	}
	*/
        return pixeldata;
    }

    /**
     * fill the 256 element table with linear values
     */
    private void linear_tbl(int tbl[], double slow, double shigh) {
        double floati;
        double sdiff;

        sdiff = shigh - slow;
        for (int j = 0; j < 255; j++) {
            floati = (sdiff / 254) * j + slow;
            if (-floati > Integer.MAX_VALUE)
                tbl[j] = -Integer.MAX_VALUE;
            else if (floati > Integer.MAX_VALUE)
                tbl[j] = Integer.MAX_VALUE;
            else
                tbl[j] = (int) floati;
        }
        tbl[255] = Integer.MAX_VALUE;
    }

    /**
     * fill the 256 element table with values for a squared stretch
     */
    private void squared_tbl(int tbl[], double slow, double shigh) {
        double floati;
        double sdiff;

        sdiff = shigh - slow;
        if (sdiff == 0.)
            sdiff = 1.;
        for (int j = 0; j < 255; ++j) {
            floati = Math.sqrt(sdiff * sdiff / 254 * j) + slow;
            //System.out.println("RBH j = " + j + "  floati = " + floati);
            if (-floati > Integer.MAX_VALUE)
                tbl[j] = -Integer.MAX_VALUE;
            else if (floati > Integer.MAX_VALUE)
                tbl[j] = Integer.MAX_VALUE;
            else
                tbl[j] = (int) floati;

            //System.out.println("tbl["+ j + "] = " + tbl[j]);
        }
        tbl[255] = Integer.MAX_VALUE;
    }

    /**
     * fill the 256 element table with values for a squared stretch
     * for floating point pixels
     */
    private void squared_tbl_dbl(double tbl[], double slow, double shigh) {
        double floati;
        double sdiff;

        sdiff = shigh - slow;
        if (sdiff == 0.)
            sdiff = 1.;
        for (int j = 0; j < 255; ++j) {
            floati = Math.sqrt(sdiff * sdiff / 254 * j) + slow;
            //System.out.println("RBH j = " + j + "  floati = " + floati);
            tbl[j] = floati;

            //System.out.println("tbl["+ j + "] = " + tbl[j]);
        }
        tbl[255] = Double.MAX_VALUE;
    }

    /**
     * fill the 256 element table with values for a square root stretch
     */
    private void sqrt_tbl(int tbl[], double slow, double shigh) {
        double floati;
        double sdiff;

        sdiff = shigh - slow;
        if (sdiff == 0.)
            sdiff = 1.;
        for (int j = 0; j < 255; ++j) {
            floati = (Math.sqrt(sdiff) / 254 * j);
            floati = floati * floati + slow;
            //System.out.println("RBH j = " + j + "  floati = " + floati);
            if (-floati > Integer.MAX_VALUE)
                tbl[j] = -Integer.MAX_VALUE;
            else if (floati > Integer.MAX_VALUE)
                tbl[j] = Integer.MAX_VALUE;
            else
                tbl[j] = (int) floati;

            //System.out.println("tbl["+ j + "] = " + tbl[j]);
        }
        tbl[255] = Integer.MAX_VALUE;
    }

    /**
     * fill the 256 element table with values for a square root stretch
     * for floating point pixels
     */
    private void sqrt_tbl_dbl(double tbl[], double slow, double shigh) {
        double floati;
        double sdiff;

        sdiff = shigh - slow;
        if (sdiff == 0.)
            sdiff = 1.;
        for (int j = 0; j < 255; ++j) {
            floati = (Math.sqrt(sdiff) / 254 * j);
            floati = floati * floati + slow;
            tbl[j] = floati;

            //System.out.println("tbl["+ j + "] = " + tbl[j]);
        }
        tbl[255] = Double.MAX_VALUE;
    }

    public byte[] getData8() {
        return (pixeldata);
    }



    public int[] getScreenHistogram() {
        pixelhist[255] = 0;  // pixelhist[255] is count of blank pixels
        return (pixelhist);
    }

    /**
     * Get flux of pixel at given "ImagePt" coordinates
     * "ImagePt" coordinates have 0,0 lower left corner of lower left pixel
     * of THIS image
     *
     * @param ipt ImagePt coordinates
     */

    public double getFlux(ImagePt ipt)
            throws PixelValueException {
        double x = ipt.getX() - 0.5;
        double y = ipt.getY() - 0.5;

        int xint = (int) Math.round(x);
        int yint = (int) Math.round(y);

        int index = yint * imageHeader.naxis1 + xint;


        if ((xint < 0) || (xint >= imageHeader.naxis1) ||
                (yint < 0) || (yint >= imageHeader.naxis2))
            throw new PixelValueException("location not on the image");
        if (!SUPPORTED_BIT_PIXS.contains(new Integer(imageHeader.bitpix))){
            System.out.println("Unimplemented bitpix = " +imageHeader.bitpix);
            throw new PixelValueException("illegal bitpix");
        }

        double raw_dn = float1d[index];


        if ((raw_dn == imageHeader.blank_value) || (Double.isNaN(raw_dn)))
            throw new PixelValueException("No flux available");

        double flux;
        if (imageHeader.origin.startsWith("Palomar Transient Factory")) {
            flux = -2.5 * .43429 * Math.log(raw_dn / imageHeader.exptime) +
                    imageHeader.imagezpt +
                    imageHeader.extinct * imageHeader.airmass;
			/* .43429 changes from natural log to common log */
        } else {
            flux = raw_dn * imageHeader.bscale + imageHeader.bzero;
        }

        return (flux);
    }

    public String getFluxUnits() {
        String retval = imageHeader.bunit;
        if (imageHeader.bunit.startsWith("HITS")) {
            retval = "frames";
        }
        if (imageHeader.origin.startsWith(ImageHeader.PALOMAR_ID)) {
            retval = "mag";
        }
        return (retval);
    }

    public int getProjectionType() {
        return getImageHeader().maptype;
    }

    private boolean checkDistortion(ImageHeader H1, ImageHeader H2) {
        boolean result=false;
        if ((H1.ap_order == H2.ap_order) &&
                (H1.a_order == H2.a_order) &&
                (H1.bp_order == H2.bp_order) &&
                (H1.b_order == H2.b_order)) {
            result = true;
            for (int i = 0; i <= H1.a_order; i++) {
                for (int j = 0; j <= H1.a_order; j++) {
                    if ((i + j <= H1.a_order) && (i + j > 0)) {
                        if (H1.a[i][j] != H2.a[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.ap_order; i++) {
                for (int j = 0; j <= H1.ap_order; j++) {
                    if ((i + j <= H1.ap_order) && (i + j > 0)) {
                        if (H1.ap[i][j] != H2.ap[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.b_order; i++) {
                for (int j = 0; j <= H1.b_order; j++) {
                    if ((i + j <= H1.b_order) && (i + j > 0)) {
                        if (H1.b[i][j] != H2.b[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.bp_order; i++) {
                for (int j = 0; j <= H1.bp_order; j++) {
                    if ((i + j <= H1.bp_order) && (i + j > 0)) {
                        if (H1.bp[i][j] != H2.bp[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
        }
        return result;

    }
    private boolean checkOther(ImageHeader H1, ImageHeader H2){
        boolean result=false;
        if (
                (H1.naxis1 == H2.naxis1) &&
                        (H1.naxis2 == H2.naxis2) &&
                        (H1.crpix1 == H2.crpix1) &&
                        (H1.crpix2 == H2.crpix2) &&
                        (H1.cdelt1 == H2.cdelt1) &&
                        (H1.cdelt2 == H2.cdelt2) &&
                        (H1.crval1 == H2.crval1) &&
                        (H1.crval2 == H2.crval2) &&
                        (H1.crota2 == H2.crota2) &&
                        (H1.getJsys() == H2.getJsys()) &&
                        (H1.file_equinox == H2.file_equinox)) {
                        /* OK so far - now check distortion correction */
            if (H1.map_distortion &&
                    H2.map_distortion) {
                result = checkDistortion(H1,H2);

            } else {
                result = true;
            }
        }
        return result;
    }
    private boolean checkPlate(ImageHeader H1, ImageHeader H2){

        boolean result=false;
        if (  (H1.plate_ra == H2.plate_ra) &&
                (H1.plate_dec == H2.plate_dec) &&
                (H1.x_pixel_offset == H2.x_pixel_offset) &&
                (H1.y_pixel_offset == H2.y_pixel_offset) &&
                (H1.plt_scale == H2.plt_scale) &&
                (H1.x_pixel_size == H2.x_pixel_size) &&
                (H1.y_pixel_size == H2.y_pixel_size)) {

            result=true;

              /* OK so far - now check coefficients */
            for (int i = 0; i < 6; i++) {
                if (H1.ppo_coeff[i] != H2.ppo_coeff[i]) {
                    result = false;
                    break;
                }
            }
            for (int i = 0; i < 20; i++) {
                if (H1.amd_x_coeff[i] != H2.amd_x_coeff[i]) {
                    result = false;
                    break;
                }
                if (H1.amd_y_coeff[i] != H2.amd_y_coeff[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    public boolean isSameProjection(FitsRead second_fitsread) {
        boolean result = false;

        ImageHeader H1 = getImageHeader();
        ImageHeader H2 = second_fitsread.getImageHeader();

        if (H1.maptype == H2.maptype) {
            if (H1.maptype == Projection.PLATE) {
                result = checkPlate(H1,H2);
            } else {
                result = checkOther(H1, H2);
            }
        }
        return result;
    }


    public Fits getFits() {
        return (fits);
    }

    public BasicHDU getHDU() {
        return hdu;
    }

    public Header getHeader() {
        return header;
    }

    public ImageHeader getImageHeader() {
        return (imageHeader);
    }

    public RangeValues getRangeValues() {
        return (rangeValues);
    }

    public void setRangeValues(RangeValues rangeValues) {
        this.rangeValues = rangeValues;
    }

    public int getImageScaleFactor() {
        return (imageScaleFactor);
    }

    /**
     * get a description of the fits file that created this fits read
     * This can be any text.
     *
     * @return the description of the fits file
     */
    public String getSourceDec() {
        return (srcDesc);
    }

    /**
     * Set a description of the fits file that created this fits read.
     * This can be any text.
     *
     * @param s the description
     */
    public void setSourceDesc(String s) {
        srcDesc = s;
    }

    Histogram getHistogram() {
        if (hist == null) {
            computeHistogram();
        }
        return hist;
    }

    /**
     * return the index of where this fits data was i a fits file.  If a -1
     * is returned the we do not know or many this FitsReadLZ was created with
     * geom.  Otherwise if a number >= 0 other is return then that is the
     * location in the fits file
     *
     * @return index of where this fits data was in file
     */
    public int getIndexInFile() {
        return indexInFile;
    }

    /**
     * return the plane number indicating which plane in a FITS cube
     * this image came from.
     * return value:
     * 0:  this was the only image - there was no cube
     * 1:  this was the first plane in the FITS cube
     * 2:  this was the second plane in the FITS cube
     * etc.
     */
    public int getPlaneNumber() {
        return planeNumber;
    }

    /**
     * return the extension number indicating which extension this image
     * was in the original FITS image
     * return value:
     * -1:  this was the only image, the primary one - there were no extensions
     * 0:  this was the primary image (not an extension) in a FITS file with
     * extensions
     * 1:  this was the first extension in the FITS file
     * 2:  this was the second extension in the FITS file
     * etc.
     */
    public int getExtensionNumber() {
        return extension_number;
    }
    public float[] getDataFloat()
    {
        return float1d;
    }

    public void freeResources() {
        pixeldata = null;
        pixelhist = null;
        float1d = null;
        fits = null;
        imageHeader = null;
        header = null;
        hist = null;
        rangeValues = null;
    }

    public long getDataSize() {
        long retval = 0;
        if (pixeldata != null) retval += pixeldata.length;
        if (pixelhist != null) retval += pixelhist.length * 4;
        if (float1d != null) retval += float1d.length * 8;
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
