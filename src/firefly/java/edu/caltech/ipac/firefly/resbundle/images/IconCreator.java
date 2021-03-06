/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.resbundle.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
/**
 * User: roby
 * Date: Sep 1, 2009
 * Time: 11:22:02 AM
 */


/**
 * @author Trey Roby
 */
public interface IconCreator extends ClientBundle {


//    @Source("up-pointer.gif")
//    public ImageResource getUpPointer();
//
//    @Source("left-down-pointer.gif")
//    public ImageResource getLeftDownPointer();

    @Source("wrench-24x24.png")
    public ImageResource getToolsIcon();

    @Source("cyan_down_arrow.gif")
    public ImageResource getCyanDownArrow();

    @Source("cyan_right_arrow.gif")
    public ImageResource getCyanRightArrow();

//    @Source("past_searches.png")
//    public ImageResource getHistoryTags();

    @Source("blue_delete_10x10.png")
    public ImageResource getBlueDelete10x10();

    @Source("One_gear-20x20-single.png")
    public ImageResource getOneGearSingle();

    @Source("preferences.png")
    public ImageResource getPreferences();

    @Source("transparent.gif")
    public ImageResource getTransparent();

    @Source("alerts.png")
    public ImageResource getAttention();

//    @Source("expand-4arrow-24x24.png")
//    public ImageResource getExapand4Arrow();
//    @Source("one-tile-24x24.png")
    @Source("icons-2014/Images-One.png")
    public ImageResource getOneTile();

    @Source("table-image-24x24.png")
    public ImageResource getTableImage();

//    @Source("expand-24x24.jpeg")
//    @Source("expand-test.png")

//    @Source("expand-simpleV2-24x24.png")
    @Source("icons-2014/24x24_ExpandArrows.png")
    public ImageResource getExpandIcon();


    @Source("icons-2014/24x24_ExpandArrows-grid-3.png")
    public ImageResource getExpandToGridIcon();


//    @Source("expand-borderV2-24x24.png")
    @Source("icons-2014/24x24_ExpandArrowsWhiteOutline.png")
    public ImageResource getBorderedExpandIcon();

//    @Source("list-24x24.png")
    @Source("icons-2014/ListOptions.png")
    public ImageResource getList();

//    @Source("grid-24x24.png")
//    @Source("large-tiles-24x24.png")
    @Source("icons-2014/Images-Tiled.png")
    public ImageResource getGrid();

    @Source("blue-dot-10x10.png")
    public ImageResource getBlueDot();

    @Source("green-dot-10x10.png")
    public ImageResource getGreenDot();

//    @Source("close-black-24x24.png")
//    public ImageResource getCloseExpandedMode();

    @Source("backButton-start.png")
    public ImageResource getBackButtonStart();

    @Source("backButton-middle.png")
    public ImageResource getBackButtonMiddle();

    @Source("backButton-end.png")
    public ImageResource getBackButtonEnd();

    @Source("iPad_add_button.png")
    public ImageResource getIpadAddButtonPicture();

    @Source("2x2_grid.png")
    public ImageResource getGridView();

    @Source("icons-2014/Help-16x16.png")
    public ImageResource getHelpSmall();


    @Source("list.png")
    public ImageResource getTableView();

    @Source("exclamation16x16.gif")
    public ImageResource exclamation();

    @Source("xyview.png")
    ImageResource getXYPlotView();

    @Source("pdf_24x24.png")
    public ImageResource getPdf();

    @Source("icons-2014/TurnOnLayers_24x24.png")
    public ImageResource getPlotLayersSmall();




    public static class Creator  {
        private final static IconCreator _instance=
                (IconCreator) GWT.create(IconCreator.class);
        public static IconCreator getInstance() {
            return _instance;
        }
    }
}

