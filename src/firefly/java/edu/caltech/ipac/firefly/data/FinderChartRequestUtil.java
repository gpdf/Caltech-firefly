/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: 9/2/14
 * Time: 10:36 AM
 */


import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.HashMap;

/**
 * @author Trey Roby
 */
public class FinderChartRequestUtil {
    private static final String DEF = Character.toString('\0');

    public static enum ImageSet {DSS(DEF, "dss", "dss_bands", dssCombo, null, DEF),
                                 IRIS("IRAS (IRIS)", "iris", "iras_bands", irisCombo, "iraspsc", "IRAS"),
                                 ISSA(DEF, "issa", null, issaCombo, null, DEF),
                                 MSX(DEF, "msx", null, msxCombo, null, DEF),
                                 TWOMASS("2MASS", "2mass","twomass_bands", twoMassCombo, "fp_psc", DEF),
                                 WISE("WISE (AllWISE)", "wise", "wise_bands", wiseCombo, "wise_allwise_p3as_psd", DEF),
                                 SDSS("SDSS (DR7)", "sdss", "sdss_bands",sDssCombo, null, "SDSS (DR10)");

        public WebPlotRequest.ServiceType srvType = WebPlotRequest.ServiceType.valueOf(this.name());
        public String title;
        public String subgroup;
        public String band;
        public String[] comboAry;
        public String catalog;
        public String catalogTitle;

        ImageSet(String title, String subgroup, String band, String[] comboAry, String catalog, String catalogTitle) {
            this.title = title.equals(DEF) ? srvType.toString() : title;
            this.subgroup = subgroup;
            this.band = band;
            this.comboAry = comboAry;
            this.catalog = catalog;
            this.catalogTitle = catalogTitle.equals(DEF) ? this.title : catalogTitle;
        }

        public static ImageSet lookup(WebPlotRequest.ServiceType srvType) {
            return valueOf(srvType.name());
        }
    }

    public static enum Artifact {
        diff_spikes_3("WISE Diffraction Spikes (dots)", "Wise.Artifact.Spikes.level3.Selected"),
        halos("WISE Halos (squares)", "Wise.Artifact.halos.Selected"),
        ghost("WISE Optical Ghosts (diamonds)", "Wise.Artifact.ghost.Selected"),
        latents("WISE Latents (x's)", "Wise.Artifact.latents.Selected"),
        pers_arti("2MASS Persistence Artifacts (crosses)", "2Mass.Artifact.Pers.Selected"),
        glint_arti("2MASS Glints Artifacts (diamonds)", "2Mass.Artifact.Glints.Selected");

        public String desc;
        public String enablePref;

        Artifact(String desc, String enablePref) {
            this.desc = desc;
            this.enablePref = enablePref;
        }

        public static boolean isArtifacts(String desc) {
            for (Artifact art : Artifact.values()) {
                if (art.desc.equals(desc)) {
                    return true;
                }
            }
            return false;
        }
    }

    // --------  input field names and possible values
    public static final String FD_CAT_BY_BOUNDARY = "catalog_by_img_boundary";
    public static final String FD_CAT_BY_RADIUS = "catalog_by_radius";
    public static final String FD_ONE_TO_ONE = "one_to_one";
    public static final String FD_OVERLAY_CAT = "overlay_catalog";
    public static final String FD_SOURCES = "sources";
    public static final String FD_FILENAME = "filename";
    public static final String FD_SUBSIZE = "subsize";

    public static enum Source {DSS, IRIS, twomass, WISE, SDSS}
    public static enum Band {dss_bands, iras_bands, twomass_bands, wise_bands, SDSS_bands}
    public static enum Radius {iras_radius, twomass_radius, wise_radius, sdss_radius}
    // --------

    /**
     * Finder Chart services
     * combo string format: option;title
     */
    private final static String dssCombo[]={
            "poss1_blue;DSS1 Blue",
            "poss1_red;DSS1 Red",
            "poss2ukstu_blue;DSS2 Blue",
            "poss2ukstu_red;DSS2 Red",
            "poss2ukstu_ir;DSS2 IR",
            /*"quickv;Quick-V Survey",
            "phase2_gsc2;HST Phase 2 Target Positioning(GSC 2)",
            "phase2_gsc1;HST Phase 1 Target Positioning(GSC 1)",
            "all;The best of a combined list of all plates"*/};

    private final static String twoMassCombo[] = {
            "j;J",
            "h;H",
            "k;K", };

    private final static String issaCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};

    private final static String irisCombo[]= {
            "12;12 microns",
            "25;25 microns",
            "60;60 microns",
            "100;100 microns"};

    private final static String msxCombo[] = {
            "3;A (8.28 microns)",
            "4;C (12.13 microns)",
            "5;D (14.65 microns)",
            "6;E (21.3 microns)"};

    private final static String wiseCombo[]={
            "3a.1;w1",
            "3a.2;w2",
            "3a.3;w3",
            "3a.4;w4"};

    private final static String sDssCombo[]={
            "u;u","g;g","r;r","i;i","z;z"
    };

    private static HashMap<String, Integer> thumbnailSizeMap = new HashMap<String, Integer>() {
        {
            put("small",128);
            put("medium",192);
            put("large",256);
        }
    };




    public static WebPlotRequest makeWebPlotRequest(WorldPt pt,
                                                    float radius,
                                                    int width,
                                                    String band,
                                                    String expandedTitle,
                                                    WebPlotRequest.ServiceType service ) {

        WebPlotRequest wpReq= getWebPlotRequest(service, band, pt, radius);
        if (!StringUtils.isEmpty(expandedTitle)) wpReq.setExpandedTitle(expandedTitle);
        wpReq.setExpandedTitleOptions(WebPlotRequest.ExpandedTitleOptions.PREFIX);
        wpReq.setZoomType(ZoomType.TO_WIDTH);
        wpReq.setZoomToWidth(width);
        wpReq.setZoomToHeight(width); // set width and height the same
        wpReq.setPostCropAndCenter(true);
        wpReq.setRotateNorth(true);
        wpReq.setSaveCorners(true);
        wpReq.setInitialColorTable(1);
        wpReq.setHideTitleDetail(true);
        wpReq.setPreferenceColorKey("FcColorKey");
        wpReq.setTitleOptions(WebPlotRequest.TitleOptions.SERVICE_OBS_DATE);
        wpReq.setTitle(getComboTitle(band));
        return wpReq;
    }


    private static WebPlotRequest getWebPlotRequest(WebPlotRequest.ServiceType service, String band, WorldPt pt, Float radius) {
        WebPlotRequest wpReq=null;
        switch (service) {
            case DSS:
                wpReq= WebPlotRequest.makeDSSRequest(pt, getComboValue(band),radius);
                break;
            case IRIS:
                wpReq= WebPlotRequest.makeIRISRequest(pt, getComboValue(band), radius);
                break;
            case ISSA:
                wpReq= WebPlotRequest.makeISSARequest(pt, getComboValue(band),radius);
                break;
            case MSX:
                wpReq= WebPlotRequest.makeMSXRequest(pt, getComboValue(band),radius);
                break;
            case SDSS:
                wpReq= WebPlotRequest.makeSloanDSSRequest(pt, getComboValue(band), radius);
                break;
            case TWOMASS:
                wpReq= WebPlotRequest.make2MASSRequest(pt, getComboValue(band),radius);
                break;
            case WISE:
                String[] pair= getComboValue(band).split("\\.");
                wpReq= WebPlotRequest.makeWiseRequest(pt, pair[0], pair[1], radius);
                break;
        }
        if (wpReq!=null) wpReq.setDrawingSubGroupId(ImageSet.lookup(service).subgroup);
        return wpReq;
    }

    public static String getComboValue(String combo) {
        String sAry[]= combo.split(";");
        return sAry.length>0 ? sAry[0] : combo;
    }
    public static String getComboTitle(String combo) {
        String sAry[]= combo.split(";");
        return sAry.length>1 ? sAry[1] : combo;
    }

    public static int getPlotWidth(String sizeKey) {
        if (sizeKey!=null && thumbnailSizeMap.containsKey(sizeKey))  {
            return thumbnailSizeMap.get(sizeKey);
        }
        else {
            return thumbnailSizeMap.get("medium");
        }
    }

}

