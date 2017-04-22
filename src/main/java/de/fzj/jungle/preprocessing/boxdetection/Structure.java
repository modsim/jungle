package de.fzj.jungle.preprocessing.boxdetection;

/**
 * Represents predefined microfluidic growth chambers, either by ratio or
 * absolute dimensions.
 * 
 * @author Stefan Helfrich
 *
 */
public class Structure
{

	private final int width;

	private final int height;

	private final double ratio;

	public Structure( int width, int height )
	{
		this.width = width;
		this.height = height;
		this.ratio = 0;
	}

	public Structure( double ratio )
	{
		this.width = 0;
		this.height = 0;
		this.ratio = ratio;
	}

	/**
	 * @return the width
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * @return the height
	 */
	public int getHeight()
	{
		return height;
	}

	public double getRatio()
	{
		return ratio;
	}
}
