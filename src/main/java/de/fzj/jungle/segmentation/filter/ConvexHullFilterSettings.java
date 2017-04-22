package de.fzj.jungle.segmentation.filter;

/**
 * {@lin FilterSettings} for a {@link ConvexHullFilter}.
 * 
 * @author Stefan Helfrich
 */
public class ConvexHullFilterSettings implements FilterSettings
{

	/*
	 * Private fields
	 */
	private final double deviation;

	/**
	 * 
	 */
	public ConvexHullFilterSettings( double deviation )
	{
		this.deviation = deviation;
	}

	/**
	 * @return the deviation
	 */
	public double getDeviation()
	{
		return deviation;
	}

}
