/**
 * 
 */
package de.fzj.jungle.segmentation.filter;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;

import de.fzj.jungle.segmentation.Cell;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * {@link Filter} that splits a {@link Cell} when the area deviation from the
 * convex hull reaches a given threshold.
 * 
 * @author Stefan Helfrich
 */
public class ConvexHullFilter implements Filter< Cell >
{

	private ConvexHullFilterSettings settings;

	public ConvexHullFilter( ConvexHullFilterSettings settings )
	{
		this.settings = settings;
	}

	@Override
	public Result< Cell > filter( Collection< Cell > collection, ImageProcessor ip )
	{
		Collection< Cell > singleCellList = new ArrayList<>();
		Collection< Cell > multipleCellList = new ArrayList<>();

		for ( Cell c : collection )
		{
			// Compute convex hull of cell
			Polygon convexHullPolygon = c.getCellRoi().getConvexHull();
			Roi convexHullRoi = new PolygonRoi( convexHullPolygon, Roi.POLYGON );

			// Obtain area of convex hull
			ip.setRoi( convexHullRoi );
			ImageStatistics stats = ImageStatistics.getStatistics( ip, Measurements.AREA | Measurements.CENTROID, c.getCalibration() );

			double convexHullArea = stats.area;

			// Obtain area of cell
			double cellArea = c.getEnclosedArea();

			// Compute deviation
			double deviation = Math.abs( 1 - ( cellArea / convexHullArea ) );

			if ( deviation < settings.getDeviation() )
			{
				// don't split cell
				singleCellList.add( c );
			}
			else
			{
				// split cell
				multipleCellList.add( c );
			}
		}

		return new Result<>( singleCellList, multipleCellList );
	}
}
