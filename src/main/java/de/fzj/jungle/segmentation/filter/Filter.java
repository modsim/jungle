/**
 * 
 */
package de.fzj.jungle.segmentation.filter;

import ij.process.ImageProcessor;

import java.util.Collection;

/**
 * This interface imposes the implementation of a filter method for objects for
 * each class that implements it. Such a filter method is supposed to return an
 * object of type {@code Result} that contains a list of elements that are
 * single objects and a second list of objects that need further processing.
 * 
 * @author Stefan Helfrich
 */
public interface Filter<T> {
	
	/**
	 * Filters the parameter list and sorts them into lists of finished objects
	 * and objects that need further processing, which are both encapsulated in
	 * a Result object.
	 * 
	 * @param list
	 *            a collection of objects that is to be filtered
	 * @param ip
	 *            an ImageProcessor that might be needed for some filters
	 * @return a result object containing two lists for further processing
	 */
	public Result<T> filter(Collection<T> list, ImageProcessor ip);
	
	class Result<T> {
		
		/* Public fields - for convenience */
		public Collection<T> singleCellList;
		public Collection<T> multipleCellList;
		
		public Result(Collection<T> singleCellList, Collection<T> multipleCellList) {
			this.singleCellList = singleCellList;
			this.multipleCellList = multipleCellList;
		}
		
	}
}
