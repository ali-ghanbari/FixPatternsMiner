package model.util;

import model.MARK_II.Cell;
import model.MARK_II.Column;
import model.MARK_II.Region;
import model.MARK_II.Synapse;

import java.util.Set;
import java.awt.Dimension;

/**
 * @author Quinn Liu (quinnliu@vt.edu)
 * @version June 18, 2013
 */
public class RegionConsoleViewer {
    /**
     * Returns a 2-D array of chars representing each Column's activeState
     * within a Region inside of a SpatialPooler object. 'a' represents an
     * active Column while 'i' represents an inactive Column for the current
     * time step.
     *
     * @param region
     * @return A 2-D char array of Columns' overlapScores.
     */
    public static char[][] getColumnActiveStatesCharArray(Region region) {
	char[][] columnActiveStates = new char[region.getXAxisLength()][region
		.getYAxisLength()];
	Column[][] columns = region.getColumns();
	for (int x = 0; x < columnActiveStates.length; x++) {
	    for (int y = 0; y < columnActiveStates[x].length; y++) {
		if (columns[x][y].getActiveState()) {
		    // 'a' represents an active Column at a specific time step
		    columnActiveStates[x][y] = 'a';
		} else {
		    // 'i' represents an inactive Column at a specific time step
		    columnActiveStates[x][y] = 'i';
		}
	    }
	}
	return columnActiveStates;
    }

    /**
     * Returns a 2-D array of integers representing each Column's overlapScore
     * within a Region inside of a SpatialPooler object.
     *
     * @param region
     * @return A 2-D integer array of Columns' overlapScores.
     */
    public static int[][] getColumnOverlapScoresIntArray(Region region) {
	int[][] columnOverlapScores = new int[region.getXAxisLength()][region
		.getYAxisLength()];
	Column[][] columns = region.getColumns();
	for (int x = 0; x < columnOverlapScores.length; x++) {
	    for (int y = 0; y < columnOverlapScores[x].length; y++) {
		columnOverlapScores[x][y] = columns[x][y].getOverlapScore();
	    }
	}
	return columnOverlapScores;
    }

    /**
     * 0 means the SensorCell is disconnected from the Synapse at that location.
     * Any number 2-9 represents the permanenceValue of the Synapse at that
     * location rounded down in the tenth decimal place.
     *
     * For example, a Synapse with a permanenceValue of 0.36 will be represented
     * as a 3 in the int[][] array being returned.
     */
    public static int[][] getSynapsePermanencesIntArray(Region region) {
	Dimension bottomLayerDimensions = region.getBottomLayerXYAxisLength();
	int[][] synapsePermanences = new int[bottomLayerDimensions.width][bottomLayerDimensions.height];

	Column[][] columns = region.getColumns();
	for (int x = 0; x < columns.length; x++) {
	    for (int y = 0; y < columns[x].length; y++) {
		Set<Synapse<Cell>> synapses = columns[x][y]
			.getProximalSegment().getSynapses();

		for (Synapse<Cell> synapse : synapses) {
		    if (synapse.getPermanenceValue() < 0.2) {
			synapsePermanences[synapse.getCellXPosition()][synapse
				.getCellYPosition()] = 0;
		    } else {
			// permanenceTimesTen = round((0.9999 - 0.05555) * 10)
			int permanenceTimesTen = (int) Math.round(((synapse
				.getPermanenceValue() - 0.055555) * 10));
			synapsePermanences[synapse.getCellXPosition()][synapse
				.getCellYPosition()] = permanenceTimesTen;
		    }
		}
	    }
	}
	return synapsePermanences;
    }

    /**
     * Prints a byte 2-D array to the console.
     *
     * @param doubleByteArray
     *            The 2-D byte array to be printed.
     */
    public static void printDoubleByteArray(byte[][] doubleByteArray) {
	for (int x = 0; x < doubleByteArray.length; x++) {
	    System.out.println();
	    for (int y = 0; y < doubleByteArray[x].length; y++) {
		System.out.print(doubleByteArray[x][y]);
	    }
	}
    }

    /**
     * Prints a int 2-D array to the console.
     *
     * @param doubleIntArray
     *            The 2-D int array to be printed.
     */
    public static void printDoubleIntArray(int[][] doubleIntArray) {
	for (int x = 0; x < doubleIntArray.length; x++) {
	    System.out.println();
	    for (int y = 0; y < doubleIntArray[x].length; y++) {
		System.out.printf("%4d", doubleIntArray[x][y]);
	    }
	}
    }

    /**
     * Prints a char 2-D array to the console.
     *
     * @param doubleCharArray
     *            The 2-D char array to be printed.
     * @return The printed 2-D char array as a String.
     *
     */
    public static String doubleCharArrayAsString(char[][] doubleCharArray) {
	String doubleCharArrayAsString = "";
	for (int x = 0; x < doubleCharArray.length; x++) {
	    boolean isAtBeginning = (x == 0);
	    boolean isAtEnd = (x == doubleCharArray.length);
	    if (isAtBeginning || isAtEnd) {
		// don't add anything
	    } else {
		doubleCharArrayAsString += "\n";
	    }

	    for (int y = 0; y < doubleCharArray[x].length; y++) {
		doubleCharArrayAsString += doubleCharArray[x][y];
	    }
	}
	return doubleCharArrayAsString;
    }
}
