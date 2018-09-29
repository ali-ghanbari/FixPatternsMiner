/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Lee Kamentsky
 *
 */
package net.imglib2.algorithm.labeling;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingOutOfBoundsRandomAccessFactory;
import net.imglib2.labeling.LabelingType;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsRandomAccess;
import net.imglib2.type.logic.BitType;

/**
 * Label all 8-connected components of a binary image
 *
 */
public class AllConnectedComponents {
	protected static class PositionStack {
		private final int dimensions;
		private long [] storage;
		private int position = 0;
		public PositionStack(int dimensions) {
			this.dimensions = dimensions;
			storage = new long [100 * dimensions];
		}
		public void push(long [] position) {
			int insertPoint = this.position * dimensions;
			if (storage.length == insertPoint) {
				long [] newStorage = new long [storage.length * 3 / 2];
				System.arraycopy(storage, 0, newStorage, 0, storage.length);
				storage = newStorage;
			}
			System.arraycopy(position, 0, storage, insertPoint, dimensions);
			this.position++;
		}
		public void pop(long [] position) {
			this.position--;
			System.arraycopy(storage, this.position * dimensions, 
					position, 0, dimensions);
		}
		public boolean isEmpty() {
			return position == 0;
		}
	}
	/**
	 * Label all connected components in the given image using an 8-connected
	 * structuring element or it's N-dimensional analog (connect if touching
	 * along diagonals as well as +/- one element in any direction).
	 * @param <T> the type of the labels to apply
	 * @param labeling Assign labels to this labeling space
	 * @param img a binary image where true indicates parts of components
	 * @param names supplies names for the different components as needed
	 * @throws NoSuchElementException if there are not enough names
	 */
	public static <T extends Comparable<T>> void labelAllConnectedComponents(
			Labeling<T> labeling, Img<BitType> img, Iterator<T> names)
	throws NoSuchElementException
	{
		long [][] offsets = getStructuringElement(img.numDimensions());
		labelAllConnectedComponents(labeling, img, names, offsets);
	}
	/**
	 * Label all connected components in the given image using an arbitrary
	 * structuring element.
	 * @param <T> the type of the labels to apply
	 * @param labeling Assign labels to this labeling space 
	 * @param img a binary image where true indicates parts of components
	 * @param names supplies names for the different components as needed
	 * @param structuringElement an array of offsets to a pixel of the
	 * pixels which are considered connected. For instance, a 4-connected
	 * structuring element would be "new int [][] {{-1,0},{1,0},{0,-1},{0,1}}".
	 * @throws NoSuchElementException if there are not enough names
	 */
	public static <T extends Comparable<T>> void labelAllConnectedComponents(
			Labeling<T> labeling, Img<BitType> img,
			Iterator<T> names, long [][] structuringElement)
	throws NoSuchElementException
	{
		Cursor<BitType> c = img.localizingCursor();
		RandomAccess<BitType> raSrc = img.randomAccess();
		OutOfBoundsFactory<LabelingType<T>, Img<LabelingType<T>>> factory =
			new LabelingOutOfBoundsRandomAccessFactory<T, Img<LabelingType<T>>>();
		OutOfBounds<LabelingType<T>> oob = factory.create(labeling);
		OutOfBoundsRandomAccess<LabelingType<T>> raDest = 
			new OutOfBoundsRandomAccess<LabelingType<T>>(labeling.numDimensions(), oob);
		long [] srcPosition = new long [img.numDimensions()];
		long [] destPosition = new long [labeling.numDimensions()];
		long [] dimensions = new long [labeling.numDimensions()];
		labeling.dimensions(dimensions);
		PositionStack toDoList = new PositionStack(img.numDimensions()); 
		while(c.hasNext()) {
			BitType t = c.next();
			if (t.get()) {
				c.localize(srcPosition);
				boolean outOfBounds = false;
				for (int i=0; i<dimensions.length; i++) {
					if (srcPosition[i] >= dimensions[i]) {
						outOfBounds = true;
						break;
					}
				}
				if (outOfBounds) continue;
				
				raDest.setPosition(srcPosition);
				/*
				 * Assign a label if no label has yet been assigned.
				 */
				LabelingType<T> label = raDest.get();
				if (label.getLabeling().isEmpty()) {
					List<T> currentLabel = label.intern(names.next());
					label.setLabeling(currentLabel);
					toDoList.push(srcPosition);
					while (! toDoList.isEmpty()) {
						/*
						 * Find neighbors at the position
						 */
						toDoList.pop(srcPosition);
						for (long [] offset:structuringElement) {
							outOfBounds = false;
							for (int i=0; i<offset.length; i++) {
								destPosition[i] = srcPosition[i] + offset[i];
								if ((destPosition[i] < 0) || (destPosition[i] >= dimensions[i])){
									outOfBounds = true;
									break;
								}
							}
							if (outOfBounds) continue;
							raSrc.setPosition(destPosition);
							if (raSrc.get().get()) { 
								raDest.setPosition(destPosition);
								label = raDest.get(); 
								if (label.getLabeling().isEmpty()) {
									label.setLabeling(currentLabel);
									toDoList.push(destPosition);
								}
							}
						}
					}
				}
			}
		}
	}
	/**
	 * Return an array of offsets to the 8-connected (or N-d equivalent)
	 * structuring element for the dimension space. The structuring element
	 * is the list of offsets from the center to the pixels to be examined.
	 * @param dimensions
	 * @return the structuring element.
	 */
	static public long [][] getStructuringElement(int dimensions) {
		int nElements = 1;
		for (int i=0; i<dimensions; i++) nElements *= 3;
		nElements--;
		long [][] result = new long [nElements][dimensions];
		long [] position = new long [dimensions];
		Arrays.fill(position, -1);
		for (int i=0; i<nElements; i++) {
			System.arraycopy(position, 0, result[i], 0, dimensions);
			/*
			 * Special case - skip the center element.
			 */
			if (i == nElements / 2 - 1) {
				position[0] += 2;
			} else {
				for (int j=0;j<dimensions;j++) {
					if (position[j] == 1) {
						position[j] = -1;
					} else {
						position[j]++;
						break;
					}
				}
			}
		}
		return result;
	}
	/**
	 * Return an iterator that (endlessly) dispenses increasing integer
	 * values for labeling components.
	 * 
	 * @param start
	 * @return an iterator dispensing Integers
	 */
	static public Iterator<Integer> getIntegerNames(final int start) {
		return new Iterator<Integer>() {
			int current = start;
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public Integer next() {
				return current++;
			}

			@Override
			public void remove() {
				
			}
		};
	}
}
