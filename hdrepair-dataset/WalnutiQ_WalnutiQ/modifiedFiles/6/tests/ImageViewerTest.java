package model;

import model.util.BoundingBox;
import java.awt.geom.Point2D;
import model.util.RegionConsoleViewer;
import java.io.IOException;
import java.awt.Point;

/**
 * @author Quinn Liu (quinnliu@vt.edu)
 * @version Apr 28, 2014
 */
public class ImageViewerTest extends junit.framework.TestCase {
	private int[][] image1;

	private ImageViewer imageViewer;
	private SaccadingRetina retina;

	public void setUp() throws IOException {
		this.image1 = new int[8][10];
		for (int x = 0; x < this.image1.length; x++) {
			for (int y = 0; y < this.image1[0].length; y++) {
				this.image1[x][y] = 0;
			}
		}
		this.image1[0][0] = 1;
		this.image1[1][0] = 1;
		this.image1[1][1] = 1;
		this.image1[0][1] = 1;

		this.image1[0][2] = 1;
		this.image1[0][3] = 1;
		this.image1[1][2] = 1;

		this.image1[0][4] = 1;
		this.image1[1][4] = 1;

		this.image1[0][6] = 1;

		this.retina = new SaccadingRetina(66, 66, new Point(33, 33), 33);
		this.imageViewer = new ImageViewer("2.bmp", this.retina);
	}

	public void test_updateRetinaWithSeenPartOfImageBasedOnCurrentPosition() {
		int[][] seenArea1 = this.retina
				.getDoubleIntArrayRepresentationOfVisionCells();
		RegionConsoleViewer.printDoubleIntArray(seenArea1);
		
		System.out.println("\n================================================================");

		this.imageViewer.updateRetinaWithSeenPartOfImageBasedOnCurrentPosition(
				new Point(33, 33), 16);

		int[][] seeanArea2 = this.retina
				.getDoubleIntArrayRepresentationOfVisionCells();
		RegionConsoleViewer.printDoubleIntArray(seeanArea2); // TODO: printed image should be zoomed in
		// and upright 
	}

	public void test_moveRetinaInsideOfBoundingBox() {
		SaccadingRetina retina2 = new SaccadingRetina(66, 66,
				new Point2D.Double(1.1, -0.1), 3.1);
		assertEquals(1.1, retina2.getPosition().getX());
		assertEquals(-0.1, retina2.getPosition().getY());
		assertEquals(3.1, retina2.getDistanceBetweenImageAndRetina());

		BoundingBox boxRetinaIsStuckIn = new BoundingBox(1, 2, 3);

		this.imageViewer.moveRetinaInsideOfBoundingBox(retina2,
				boxRetinaIsStuckIn);

		// retina2.setPosition(new Point2D.Double(1.0, 0.0));
		// retina2.setDistanceBetweenImageAndRetina(3.0);

		assertEquals(1.0, retina2.getPosition().getX());
		assertEquals(0.0, retina2.getPosition().getY());
		assertEquals(3.0, retina2.getDistanceBetweenImageAndRetina());

	}

	public void test_getSeenAreaFromMainImage() throws IOException {
		int[][] seenArea = this.imageViewer.getSeenAreaFromMainImage();
		assertEquals(66, seenArea.length);
		assertEquals(66, seenArea[0].length);
		// RegionConsoleViewer.printDoubleIntArray(seenArea);
	}

	// image1         image2
	// 1111101000
	// 1110100000     11000
	// 0000000000     00000
	// 0000000000 =>  00000
	// 0000000000     00000
	// 0000000000
	// 0000000000
	// 0000000000
	public void test_convertImage() {
		int[][] image2 = this.imageViewer.convertImage(this.image1, 2);
		assertEquals(4, image2.length);
		assertEquals(5, image2[0].length);
		assertEquals(1, image2[0][0]);
		assertEquals(1, image2[0][1]);
		assertEquals(0, image2[0][2]);
		assertEquals(0, image2[1][0]);

		int[][] image3 = this.imageViewer.convertImage(this.image1, 0.25);
		assertEquals(32, image3.length);
		assertEquals(40, image3[0].length);
		// assert "1" are in the correct places
	}
}