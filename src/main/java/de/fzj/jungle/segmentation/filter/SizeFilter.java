/**
 * $LICENSE
 */
package de.fzj.jungle.segmentation.filter;

import ij.process.ImageProcessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import de.fzj.jungle.segmentation.Cell;
import de.fzj.jungle.segmentation.filter.Filter;

/**
 * This class provides an implementation of the <code>Filter</code> interface.
 * 
 * The selection criterion used is the size of a <code>Cell</code>. Hence, a
 * <code>Collection</code> is filtered according to given thresholds of size.
 * 
 * @author Stefan Helfrich
 */
public class SizeFilter implements Filter<Cell> {

	/* Private fields */
	private SizeFilterSettings settings;
	
	/**
	 * The <code>SizeFilter</code> class creates a filter based on the given
	 * thresholds.
	 * 
	 * @param settings
	 */
	public SizeFilter(SizeFilterSettings settings) {
		this.settings = settings;
	}
	
	/**
	 * The <code>SizeFilter</code> class creates a filter based on the given
	 * thresholds.
	 */
	public SizeFilter(double singleCellThreshold, double multipleCellsThreshold,
			double backgroundThreshold) {
		this.settings = new SizeFilterSettings(singleCellThreshold,
				multipleCellsThreshold, backgroundThreshold);
	}
	
	/**
	 * @return the settings
	 */
	public SizeFilterSettings getSettings() {
		return settings;
	}

	/**
	 * @param settings the settings to set
	 */
	public void setSettings(SizeFilterSettings settings) {
		this.settings = settings;
	}

	/**
	 * Filters the given collection according to the thresholds. New
	 * <code>Collection</code>s are created for <code>Cell</code>s that are
	 * below, above, and within the thresholds, respectively.
	 * 
	 * @param collection
	 *            the collection from which cells are to be filtered
	 * @return the resulting lists
	 */
	@Override
	public Filter.Result<Cell> filter(Collection<Cell> collection, ImageProcessor ip) {
		Iterator<Cell> iter = collection.iterator();
		
		Collection<Cell> singleCellList = new LinkedList<>();
		Collection<Cell> multipleCellList = new LinkedList<>();
		
		while (iter.hasNext()) {
			Cell cell = iter.next();
			
			if (cell.getEnclosedArea() < this.settings.getSingleCellThreshold()) {
				continue;
			}
			if (cell.getEnclosedArea() < this.settings.getMultipleCellsThreshold()) {
				singleCellList.add(cell);
			} else {
				if (cell.getEnclosedArea() < this.settings.getBackgroundThreshold()) {
					multipleCellList.add(cell);
				}
			}
		}
		
		return new Result<>(singleCellList, multipleCellList);
	}
}
