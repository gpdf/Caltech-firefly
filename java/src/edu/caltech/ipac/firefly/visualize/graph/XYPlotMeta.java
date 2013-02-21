package edu.caltech.ipac.firefly.visualize.graph;

import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;

/**
 * Metadata for XYPlotWidget
 * @author tatianag
 * $Id: XYPlotMeta.java,v 1.18 2012/12/11 21:10:01 tatianag Exp $
 */
public class XYPlotMeta {

    public enum PlotStyle {LINE("line"), LINE_POINTS("linePoints"), POINTS("points");
        String key;
        PlotStyle(String key) {this.key = key;}
        static PlotStyle getPlotStyle(String style) {
            for (PlotStyle ps : values()) {
                if (ps.key.equals(style)) { return ps; }
            }
            return LINE;
        }
    }

    public enum ShowLegendRule {ALWAYS("always"), ON_EXPAND("onExpand");
        String key;
        ShowLegendRule(String key) {this.key = key;}
        static ShowLegendRule getShowLegendRule(String style) {
            for (ShowLegendRule rule : values()) {
                if (rule.key.equals(style)) { return rule; }
            }
            return ON_EXPAND;
        }
    }

    String title;

    String xTickLabelFormat;
    String yTickLabelFormat;

    int xTickCount;
    int yTickCount;

    int xSize;
    int ySize;

    XYPlotMetaSource source;

    boolean plotError;
    boolean plotSpecificPoints;
    PlotStyle plotDataPoints;
    boolean logScale = true;

    public UserMeta userMeta;


    public XYPlotMeta(String plotTitle, int xSize, int ySize, XYPlotMetaSource source) {
        this.title = plotTitle;
        this.xSize = xSize;
        this.ySize = ySize;
        this.source = source;
        xTickLabelFormat = null;
        yTickLabelFormat = null;
        xTickCount = -1;
        yTickCount = -1;
        plotError = false;
        plotDataPoints = source.getPlotStyle();
        plotSpecificPoints = true;
        logScale = true;
        this.userMeta = new UserMeta();
    }

    public String getTitle() { return title; }

    public int getXSize() { return xSize; }

    public int getYSize() { return ySize; }

    public String getXName(XYPlotData data) { return source.getXName(data); }

    public String getYName(XYPlotData data) { return source.getYName(data); }

    public String getDefaultXUnits(XYPlotData data) { return source.getDefaultXUnits(data); }

    public String getDefaultYUnits(XYPlotData data) { return source.getDefaultYUnits(data); }

    public boolean plotError() {
        return plotError;
    }

    public PlotStyle plotDataPoints() {
        return plotDataPoints;
    }

    public boolean alwaysShowLegend() {
        return source.getShowLegendRule().equals(ShowLegendRule.ALWAYS);
    }


    public boolean plotSpecificPoints() {
        return plotSpecificPoints;
    }

    public boolean logScale() { return logScale; }

    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }

    public void setUserMeta(UserMeta userMeta) {
        this.userMeta = userMeta;
    }

    public void setPlotError(boolean plotError) {
        this.plotError = plotError;
    }

    public void setPlotDataPoints(PlotStyle plotDataPoints) {
        this.plotDataPoints = plotDataPoints;
    }


    public void setPlotSpecificPoints(boolean plotSpecificPoints) {
        this.plotSpecificPoints = plotSpecificPoints;
    }

    public String findXColName(List<String> colNames) {
        return findXColName(colNames, false);
    }

    public String findDefaultXColName(List<String> colNames) {
        return findXColName(colNames, true);
    }


    private String findXColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.xCol) || ignoreUserMeta) {
            return findName(source.getXColNames(), colNames);
        } else {
            return findName(userMeta.xCol, colNames);
        }
    }

    public String findYColName(List<String> colNames) {
        return findYColName(colNames, false);
    }

    public String findDefaultYColName(List<String> colNames) {
        return findYColName(colNames, true);
    }


    private String findYColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.yCol) || ignoreUserMeta) {
            return findName(source.getYColNames(), colNames);
        } else {
            return findName(userMeta.yCol, colNames);
        }
    }

    public String findErrorColName(List<String> colNames) {
        return findErrorColName(colNames, false);
    }

    public String findDefaultErrorColName(List<String> colNames) {
        return findErrorColName(colNames, true);
    }


    private String findErrorColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.errorCol) || ignoreUserMeta) {
            return findName(source.getErrorColNames(), colNames);
        } else {
            return findName(userMeta.errorCol, colNames);
        }
    }

    public String findOrderColName(List<String> colNames) {
        return findOrderColName(colNames, false);
    }

    public String findDefaultOrderColName(List<String> colNames) {
        return findOrderColName(colNames, true);
    }


    private String findOrderColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.orderCol) || ignoreUserMeta) {
            return findName(source.getOrderColNames(), colNames);
        } else {
            return findName(userMeta.orderCol, colNames);
        }
    }

    /*
    public void addUserColumnsToDefault() {
        if (userMeta == null || !userMeta.addToDefault ) { return; }
        source.addColNamesToDefault(userMeta.getXCol(), userMeta.getYCol(), userMeta.getErrorCol(), userMeta.getOrderCol());
        userMeta.clearCols();
    }
    */

    public static String findName(String name, List<String> colNames) {
        // case insensitive
        return findName(new String[]{name}, colNames);
    }

    public static String findName(String [] names, List<String> colNames) {
        if (names != null) {
            for (String n : names) {
                for (String cn : colNames) {
                    if (n.equalsIgnoreCase(cn)) {
                        return cn;
                    }
                }
            }
        }
        return null;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setChartSize(int xSize, int ySize) {
        this.xSize = xSize;
        this.ySize = ySize;
    }

    public boolean hasUserMeta() {
        return userMeta != null && userMeta.wasSet();
    }


    public static class UserMeta {
        MinMax xLimits = null;
        MinMax yLimits = null;
        String xCol = null;
        String yCol = null;
        String errorCol = null;
        String orderCol = null;
        boolean addToDefault = false;

        public UserMeta() {
            this.xLimits = null;
            this.yLimits = null;
            this.xCol = null;
            this.yCol = null;
            this.errorCol = null;
            this.orderCol = null;
        }

        public boolean wasSet() {
            return xLimits != null || yLimits != null || xCol != null || yCol != null ||
                    errorCol != null || orderCol != null;
        }

        public void setXLimits(MinMax xLimits) { this.xLimits = xLimits; }
        public void setYLimits(MinMax yLimits) { this.yLimits = yLimits; }
        public void setXCol(String xCol) { this.xCol = xCol; }
        public void setYCol(String yCol) { this.yCol = yCol; }
        public void setErrorCol(String errorCol) { this.errorCol = errorCol; }
        public void setOrderCol(String orderCol) { this.orderCol = orderCol; }

        public MinMax getXLimits() { return xLimits; }
        public MinMax getYLimits() { return yLimits; }
        public String getXCol() { return xCol; }
        public String getYCol() { return yCol; }
        public String getErrorCol() { return errorCol; }
        public String getOrderCol() { return orderCol; }

        public boolean hasXMin() { return xLimits != null && xLimits.getMin() != Double.NEGATIVE_INFINITY; }
        public boolean hasXMax() { return xLimits != null && xLimits.getMax() != Double.POSITIVE_INFINITY;}
        public boolean hasYMin() { return yLimits != null && yLimits.getMin() != Double.NEGATIVE_INFINITY;}
        public boolean hasYMax() { return yLimits != null && yLimits.getMax() != Double.POSITIVE_INFINITY;}

        public void clearCols() {
            xCol = null;
            yCol = null;
            errorCol = null;
            orderCol = null;
            addToDefault = false;
        }

    }

}
