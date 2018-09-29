package model.experiments.vision.MARK_II;

import junit.framework.TestCase;
import model.MARK_II.connectTypes.RegionToRegionRectangleConnect;

import java.io.File;
import java.io.IOException;

/**
 * @author Q Liu (quinnliu@vt.edu)
 * @date 6/9/2015.
 */
public class BigNeocortexTest extends TestCase {

    private BigNeocortex bigNeocortex;

    public void setUp() throws IOException {
        int maxSizeOfARegionInMB = 256;
        // pass it an array of all Region names
        // Example List:
        // index_0 = root, 60, 60, 4, 20, 3
        // index_1 = A   , 60, 60, 4, 20, 3

        // NOTE: new region every 6 elements
        String[] regionParameterListInOrder = {"root", "60", "60", "4", "20", "3",
                                               "A", "60", "60", "4", "20", "3"};

        // NOTE: new connection pattern every 7 elements
        String[] regionConnectionParameterListInOrder = {
                "0", "0", "30", "60", "A", "4", "4",
                "change to region A"};

        String pathAndFolderName = "" +
                "./src/test/java/model/experiments/vision/MARK_II" +
                "/BigNeocortexTest__0";
        this.bigNeocortex = new BigNeocortex(maxSizeOfARegionInMB,
                regionParameterListInOrder, new
                RegionToRegionRectangleConnect(),
                regionConnectionParameterListInOrder,
                pathAndFolderName);
    }

    public void test_instantiateAndSaveAllUnconnectedRegions() {
        assertEquals(1, 2-1);
        // TODO: fully test if JSON files are in 2 different folders
    }

    public void test_createUniqueFolderToSaveBigNeocortex() {
        File path = new File("" +
                "./src/test/java/model/experiments/vision/MARK_II/");
        // currently there is no folder by that name in the folder MARK_II
        assertFalse(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex", path.listFiles()));

        String pathAndFolderName = "" +
                "./src/test/java/model/experiments/vision/MARK_II" +
                "/test_createUniqueFolderToSaveBigNeocortex";
        this.bigNeocortex.createUniqueFolderToSaveBigNeocortex(pathAndFolderName);
        assertTrue(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex", path.listFiles()));

        assertFalse(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex__0", path.listFiles()));
        this.bigNeocortex.createUniqueFolderToSaveBigNeocortex(pathAndFolderName);

        assertTrue(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex__0", path.listFiles()));

        assertFalse(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex__1", path.listFiles()));
        this.bigNeocortex.createUniqueFolderToSaveBigNeocortex(pathAndFolderName);
        assertTrue(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex__1", path.listFiles()));

        assertFalse(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex__2", path.listFiles()));
        this.bigNeocortex.createUniqueFolderToSaveBigNeocortex(pathAndFolderName);
        assertTrue(this.bigNeocortex.isFolderInList
                ("test_createUniqueFolderToSaveBigNeocortex__2", path.listFiles()));

        File folder = new File(pathAndFolderName);
        deleteFolder(folder);
        File folder__0 = new File(pathAndFolderName + "__0");
        deleteFolder(folder__0);
        File folder__1 = new File(pathAndFolderName + "__1");
        deleteFolder(folder__1);
        File folder__2 = new File(pathAndFolderName + "__2");
        deleteFolder(folder__2);
    }

    void deleteFolder(File file){
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteFolder(f);
            }
        }
        file.delete();
    }

    public void test_isFolderInList() {
        File path = new File("" +
                "./src/test/java/model/experiments/vision/");

        assertFalse(this.bigNeocortex.isFolderInList("fakeFolder", path
                .listFiles()));
        assertTrue(this.bigNeocortex.isFolderInList
                ("MARK_II", path.listFiles()));
    }
}
