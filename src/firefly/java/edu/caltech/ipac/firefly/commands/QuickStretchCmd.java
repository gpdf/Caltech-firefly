/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.commands;

import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.ReplotDetails;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebHistogramOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.visualize.plot.RangeValues;


public class QuickStretchCmd extends BaseGroupVisCmd {
    private RangeValues _range;
    private final String _baseLabel;


    public QuickStretchCmd(String commandName,
                           float percent) {
        this(commandName,100-percent, percent,RangeValues.PERCENTAGE);
    }


    public QuickStretchCmd(String commandName,
                           float lowerFactor,
                           float upperFactor,
                           int   stretchType) {
        super(commandName);

        _baseLabel= getLabel();
        _range=  new RangeValues(stretchType, lowerFactor,
                                 stretchType, upperFactor,
                                 RangeValues.STRETCH_LINEAR);
        updateLabel();

        Listener l= new Listener(stretchType, lowerFactor,upperFactor);

        AllPlots.getInstance().addListener(Name.REPLOT, l);
        AllPlots.getInstance().addListener(Name.FITS_VIEWER_CHANGE, l);

    }

    private RangeValues getCurrentRV() {
        WebPlot plot= getPlotView().getPrimaryPlot();
        PlotState state= plot.getPlotState();
        return state.getRangeValues(state.firstBand());
    }

    public QuickStretchCmd(String commandName, int stretchAlgorythm) {
        super(commandName);
        _baseLabel= getLabel();
        _range=  new RangeValues(RangeValues.ZSCALE,
                                 1,
                                 RangeValues.ZSCALE,
                                 1,
                                 stretchAlgorythm, 25,600,120);
    }

    protected void doExecute() {

        for(MiniPlotWidget mpwItem : getGroupActiveList()) {
            WebPlotView pv= mpwItem.getPlotView();
            WebPlot plot= pv.getPrimaryPlot();
            if (plot==null) continue; //don't do anything if plot is null.
            Band bands[]=  plot.getPlotState().getBands();
            StretchData stretchData[]= new StretchData[bands.length];
            int i=0;
            for(Band band : bands) {
                stretchData[i++]= new StretchData(band,_range,true);
            }
            WebHistogramOps.recomputeStretch(plot,stretchData);
        }
    }

    private void updateLabel() {
        int algor= _range.getStretchAlgorithm();
        String label= "";
        switch (algor) {
            case RangeValues.STRETCH_LINEAR : label= "Linear: "+_baseLabel; break;
            case RangeValues.STRETCH_LOG : label= "Log: "+_baseLabel; break;
            case RangeValues.STRETCH_LOGLOG : label= "Log-Log: "+_baseLabel; break;
            case RangeValues.STRETCH_EQUAL : label= "Histogram Equalization: "+_baseLabel; break;
            case RangeValues.STRETCH_SQUARED : label= "Squared: "+_baseLabel; break;
            case RangeValues.STRETCH_SQRT : label= "Square Root: "+_baseLabel; break;
        }
        setLabel(label);
    }



    private class Listener implements WebEventListener {

        private final float _lowerFactor;
        private final float _upperFactor;
        private final int _stretchType;
        Listener(int stretchType, float lowerFactor, float upperFactor )  {
            _lowerFactor= lowerFactor;
            _upperFactor= upperFactor;
            _stretchType= stretchType;
        }


        public void eventNotify(WebEvent ev) {
            if (getPlotView()==null) return;
            ReplotDetails details= null;
            if (ev.getData() instanceof ReplotDetails) {
                details= (ReplotDetails)ev.getData();
            }

            if (details==null || details.getReplotReason()== ReplotDetails.Reason.IMAGE_RELOADED) {
                WebPlot plot= getPlotView().getPrimaryPlot();
                if (plot!=null && plot.getBands().length>0) {
                    _range=  new RangeValues(_stretchType,
                                             _lowerFactor,
                                             _stretchType,
                                             _upperFactor,
                                             getCurrentRV().getStretchAlgorithm());
                    updateLabel();
                }
            }
        }
    };
}
