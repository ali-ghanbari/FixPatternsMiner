package model.MARK_II;

import junit.framework.TestCase;
import model.MARK_II.connectTypes.AbstractRegionToRegionConnect;
import model.MARK_II.connectTypes.RegionToRegionRectangleConnect;
import model.util.*;
import model.util.Rectangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Quinn Liu (quinnliu@vt.edu)
 * @version July 29, 2013
 */
public class RegionTest extends TestCase {
    private Region region;

    public void setUp() {
        this.region = new Region("region", 5, 7, 4, 20, 3);
    }
    

    public void test_Region() {
        try {
            this.region = new Region("V1", 0, 7, 4, 20, 3);
            fail("should've thrown an exception!");
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "numberOfColumnsAlongXAxis in Region constructor cannot be less than 1",
                    expected.getMessage());
        }

        try {
            this.region = new Region("V1", 5, 7, 0, 20, 3);
            fail("should've thrown an exception!");
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "cellsPerColumn in Region constructor cannot be less than 1",
                    expected.getMessage());
        }

        try {
            this.region = new Region("V1", 5, 7, 1, -20, 3);
            fail("should've thrown an exception!");
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "percentMinimumOverlapScore in Region constructor must be between 0 and 100",
                    expected.getMessage());
        }
    }

    public void test_getBottomLayerXYAxisLength() {
        Region bottomLayer = new Region("bottomLayer", 25, 35, 1, 50, 1);
        AbstractRegionToRegionConnect connectType = new RegionToRegionRectangleConnect();
        connectType.connect(bottomLayer, this.region, 0, 0);

        Dimension bottomLayerDimensions = this.region
                .getBottomLayerXYAxisLength();
        assertEquals(25, bottomLayerDimensions.width);
        assertEquals(35, bottomLayerDimensions.height);
    }

    public void test_maximumActiveDutyCycle() {
        try {
            List<Column> neighborColumns = null;
            this.region.maximumActiveDutyCycle(neighborColumns);
            fail("should've thrown an exception!");
        } catch (IllegalArgumentException expected) {
            assertEquals("neighborColumns in Column class method "
                            + "maximumActiveDutyCycle cannot be null",
                    expected.getMessage());
        }

        Column column1 = new Column(2, new ColumnPosition(0, 0));
        Column column2 = new Column(2, new ColumnPosition(0, 0));
        Column column3 = new Column(2, new ColumnPosition(0, 0));

        column1.setActiveDutyCycle(0.1f);
        column2.setActiveDutyCycle(0.2f);
        column3.setActiveDutyCycle(0.3f);

        List<Column> neighborColumns = new ArrayList<Column>();
        neighborColumns.add(column1);
        neighborColumns.add(column2);
        neighborColumns.add(column3);

        assertEquals(0.3f, this.region.maximumActiveDutyCycle(neighborColumns),
                0.001);
    }

    public void test_getColumns() {
        try {
            this.region.getColumns(new Rectangle(new Point(0, 0), new Point(5, 6)));
        } catch (IllegalArgumentException expected) {
            assertEquals("In class Region method " +
                    "getColumns the input parameter Rectangle is larger than the" +
                    "Column[][] 2D array", expected.getMessage());
        }

        Region parent = new Region("parent", 6, 8, 4, 20, 3); // 6 rows 8 columns
        Column[][] partialParent = parent.getColumns(new Rectangle(new Point(2, 2), new Point(7, 5)));
        int numberOfRows = partialParent.length;
        int numberOfColumns = partialParent[0].length;
        assertEquals(3, numberOfRows);
        assertEquals(5, numberOfColumns);

        // TODO: connect parent to child Region

    }

    public void test_toString() {
        Region region2 = new Region("region2", 5, 7, 4, 20, 3);
        Region region3 = new Region("region3", 5, 7, 4, 20, 3);

        this.region.addChildRegion(region2);
        this.region.addChildRegion(region3);

        String correctConsoleOutput = "\n==================================\n"
                + "-----------Region Info------------\n"
                + "       name of this region: region\n"
                + "     child region(s) names: region2, region3, \n"
                + "   # of Columns along Rows: 5\n"
                + "# of Columns along Columns: 7\n"
                + " 	           # of layers: 4\n"
                + "percentMinimumOverlapScore: 20.0 %\n"
                + "      desiredLocalActivity: 3\n"
                + "          inhibitionRadius: 1\n"
                + "===================================";

        assertEquals(correctConsoleOutput, this.region.toString());
    }
}
