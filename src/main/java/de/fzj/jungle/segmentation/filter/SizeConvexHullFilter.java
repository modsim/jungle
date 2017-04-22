/**
 * 
 */
package de.fzj.jungle.segmentation.filter;

import ij.process.ImageProcessor;

import java.util.Collection;
import java.util.LinkedList;

import de.fzj.jungle.segmentation.Cell;

/**
 * A filter that combines a size filter with a convex hull filter. The filtering
 * is done in a two step process and only removes too small / too large
 * segments. Therefore, both output lists from the sizefilter are combined and
 * then used as input to the convex hull filter.
 * 
 * @author Stefan Helfrich
 */
public class SizeConvexHullFilter extends ConvexHullFilter
{

	private SizeFilter sizeFilter;

	public SizeConvexHullFilter( SizeFilterSettings sizeFilterSettings, ConvexHullFilterSettings convexHullFilterSettings )
	{
		super( convexHullFilterSettings );

		this.sizeFilter = new SizeFilter( sizeFilterSettings );
	}

	@Override
	public Result< Cell > filter( Collection< Cell > list, ImageProcessor ip )
	{
		Result< Cell > sizeFilterResult = this.sizeFilter.filter( list, ip );

		Collection< Cell > sizeFilterCombined = new LinkedList<>();
		sizeFilterCombined.addAll( sizeFilterResult.singleCellList );
		sizeFilterCombined.addAll( sizeFilterResult.multipleCellList );

		return super.filter( sizeFilterCombined, ip );
	}

}
