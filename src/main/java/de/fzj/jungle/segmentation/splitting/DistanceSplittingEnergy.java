package de.fzj.jungle.segmentation.splitting;

import java.awt.Point;

/**
 * {@link SplittingEnergy} that based solely on the distance between points on
 * the cell contour.
 * 
 * @author Stefan Helfrich
 */
public class DistanceSplittingEnergy extends SplittingEnergy
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.fzj.helfrich.segmentation.SplittingEnergy#computeEnergy()
	 */
	@Override
	public double computeEnergy( Point a, Point b )
	{
		return a.distance( b );
	}

	@Override
	public double computeEnergy( Point a, Point prevA, Point b, Point prevB )
	{
		return computeEnergy( a, b );
	}

}
