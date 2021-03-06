/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;


public class RangeValues implements Cloneable, Serializable,HandSerialize {


    public static final String PERCENTAGE_STR = "Percent";
    public static final String ABSOLUTE_STR   = "Absolute";
    public static final String SIGMA_STR      = "Sigma";

    public static final int PERCENTAGE = 88;
    public static final int MAXMIN     = 89;
    public static final int ABSOLUTE   = 90;
    public static final int ZSCALE     = 91;
    public static final int SIGMA      = 92;


    public static final String LINEAR_STR= "Linear";
    public static final String LOG_STR= "Log";
    public static final String LOGLOG_STR= "LogLog";
    public static final String EQUAL_STR= "Equal";
    public static final String SQUARED_STR= "Squared";
    public static final String SQRT_STR= "Sqrt";

    public static final int STRETCH_LINEAR= 44;
    public static final int STRETCH_LOG   = 45;
    public static final int STRETCH_LOGLOG= 46;
    public static final int STRETCH_EQUAL = 47;
    public static final int STRETCH_SQUARED = 48;
    public static final int STRETCH_SQRT    = 49;

    private int    _lowerWhich;
    private double _lowerValue;
    private int    _upperWhich;
    private double _upperValue;
    private int    _algorithm= STRETCH_LINEAR;
    private int    _zscale_contrast;
    private int    _zscale_samples; /* desired number of pixels in sample */
    private int    _zscale_samples_per_line; /* optimal number of pixels per line */
    private double _bias;
    private double _contrast;

    public RangeValues() {
       this( PERCENTAGE, 1.0, PERCENTAGE, 99.0, STRETCH_LINEAR, 25, 600, 120);
    }

    public RangeValues( int    lowerWhich,
                        double lowerValue,
                        int    upperWhich,
                        double upperValue,
                        int    algorithm) {
       this( lowerWhich, lowerValue, upperWhich, upperValue, algorithm, 25, 600, 120);
    }

    public RangeValues( int    lowerWhich,
                        double lowerValue,
                        int    upperWhich,
                        double upperValue,
                        int    algorithm,
                        int    zscale_contrast,
                        int    zscale_samples,
                        int    zscale_samples_per_line) {

       this( lowerWhich, lowerValue, upperWhich, upperValue, algorithm,
               zscale_contrast, zscale_samples, zscale_samples_per_line,
               0.5, 1.0);
    }

    public RangeValues( int    lowerWhich,
                        double lowerValue,
                        int    upperWhich,
                        double upperValue,
                        int    algorithm,
                        int    zscale_contrast,
                        int    zscale_samples,
                        int    zscale_samples_per_line,
                        double bias,
                        double contrast) {

       _lowerWhich= lowerWhich;
       _lowerValue= lowerValue;
       _upperWhich= upperWhich;
       _upperValue= upperValue;
       _algorithm = algorithm;
       _zscale_contrast = zscale_contrast;
       _zscale_samples = zscale_samples;
       _zscale_samples_per_line = zscale_samples_per_line;
       _bias = bias;
       _contrast = contrast;
    }

    /**
     *
     * @param stretchType the stretch type, possible values:  "Percent", "Absolute", "Sigma"
     * @param lowerValue the lower value based on the stretch type
     * @param upperValue the upper value based on the stretch type
     * @param algorithm The Stretch algorithm, possible values "Linear", "Log", "LogLog", "Equal", "Squared", "Sqrt"
     *
     * @return
     */
    public static RangeValues create(String stretchType,
                                     double lowerValue,
                                     double upperValue,
                                     String algorithm) {
        int s= PERCENTAGE;
        if (!StringUtils.isEmpty(stretchType)) {
            if (stretchType.equalsIgnoreCase(PERCENTAGE_STR)) s=PERCENTAGE;
            else if (stretchType.equalsIgnoreCase(ABSOLUTE_STR)) s=ABSOLUTE;
            else if (stretchType.equalsIgnoreCase(SIGMA_STR)) s=SIGMA;
        }
        int a= STRETCH_LINEAR;
        if (!StringUtils.isEmpty(algorithm)) {
            if (algorithm.equalsIgnoreCase(LINEAR_STR)) a= STRETCH_LINEAR;
            else if (algorithm.equalsIgnoreCase(LOG_STR)) a=STRETCH_LOG;
            else if (algorithm.equalsIgnoreCase(LOGLOG_STR)) a= STRETCH_LOGLOG;
            else if (algorithm.equalsIgnoreCase(EQUAL_STR)) a= STRETCH_EQUAL;
            else if (algorithm.equalsIgnoreCase(SQUARED_STR)) a= STRETCH_SQUARED;
            else if (algorithm.equalsIgnoreCase(SQRT_STR)) a= STRETCH_SQRT;
        }
        return new RangeValues(s,lowerValue,s,upperValue,a);
    }



    public int    getLowerWhich() { return _lowerWhich; }
    public double getLowerValue() { return _lowerValue; }

    public int    getUpperWhich() { return _upperWhich; }
    public double getUpperValue() { return _upperValue; }

    public int getZscaleContrast() { return _zscale_contrast; }
    public int getZscaleSamples() { return _zscale_samples; }
    public int getZscaleSamplesPerLine() { return _zscale_samples_per_line; }

    public int getStretchAlgorithm() { return _algorithm; }

    public byte computeBiasAndContrast(byte data) {
        short value = data>=0?data:(short)(2*(Byte.MAX_VALUE+1)+data);
        short offset = (short)(Byte.MAX_VALUE*(_bias-0.5)*-4);
        short shift = (short)(Byte.MAX_VALUE*(1-_contrast));

        value = (short)( offset+(value*_contrast)+shift );
        if (value>(Byte.MAX_VALUE*2)) value = Byte.MAX_VALUE*2;
        if (value<0) value = 0;

        return (byte) value;
    }

    public Object clone() {
        return new RangeValues( _lowerWhich, _lowerValue, _upperWhich,
		_upperValue, _algorithm,
		_zscale_contrast, _zscale_samples, _zscale_samples_per_line, _bias, _contrast );
    }

    public String toString() { return serialize(); }


    public static RangeValues makeDefaultSigma() {
        return new RangeValues(SIGMA,-2F,SIGMA,10F,STRETCH_LINEAR);
    }
    public static RangeValues makeDefaultPercent() {
        return new RangeValues(PERCENTAGE,-2F,PERCENTAGE,10F,STRETCH_LINEAR);
    }
    public static RangeValues makeDefaultZScale() {
        return new RangeValues(ZSCALE,1F,ZSCALE,1F,STRETCH_LINEAR,25, 600, 120);
    }

    public static RangeValues parse(String sIn) {
        if (StringUtils.isEmpty(sIn)) return null;

        try {
            String s[]= sIn.split(",");
            int i= 0;
            int    lowerWhich=              Integer.parseInt(s[i++]);
            double lowerValue=              StringUtils.parseDouble(s[i++]);
            int    upperWhich=              Integer.parseInt(s[i++]);
            double upperValue=              StringUtils.parseDouble(s[i++]);
            int    algorithm=               Integer.parseInt(s[i++]);
            int    zscale_contrast=         Integer.parseInt(s[i++]);
            int    zscale_samples=          Integer.parseInt(s[i++]);
            int    zscale_samples_per_line= Integer.parseInt(s[i]);

            return new RangeValues(lowerWhich,
                                   lowerValue,
                                   upperWhich,
                                   upperValue,
                                   algorithm,
                                   zscale_contrast,
                                   zscale_samples,
                                   zscale_samples_per_line);

        } catch (Exception e) {
            return null;
        }

    }


    public String serialize() {

        return getLowerWhich()+","+
               getLowerValue()+","+
               getUpperWhich()+","+
               getUpperValue()+","+
               getStretchAlgorithm()+","+
               getZscaleContrast()+","+
               getZscaleSamples()+","+
               getZscaleSamplesPerLine();
    }


}
