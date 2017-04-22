package de.fzj.jungle.segmentation.splitting;

import java.awt.Point;

import de.fzj.jungle.segmentation.Cell;

/**
 * Abstract base class for splitting energies that can compute an energy value
 * for two points on a {@link Cell} contour.
 * 
 * @author Stefan Helfrich
 */
public abstract class SplittingEnergy
{

	/* Private fields */
	private Point a;

	private Point b;

	/**
	 * Constructor.
	 * 
	 * @param a
	 *            Point A
	 * @param b
	 *            Point B
	 */
	public SplittingEnergy( Point a, Point b )
	{
		this.a = a;
		this.b = b;
	}

	public SplittingEnergy()
	{
		// NB: default constructor
	}

	/**
	 * Computes an energy for two splitting points.
	 * 
	 * @return an energy value.
	 */
	public abstract double computeEnergy( Point a, Point b );

	public abstract double computeEnergy( Point a, Point prevA, Point b, Point prevB );

	/**
	 * @return the a
	 */
	public Point getA()
	{
		return a;
	}

	/**
	 * @param a
	 *            the a to set
	 */
	public void setA( Point a )
	{
		this.a = a;
	}

	/**
	 * @return the b
	 */
	public Point getB()
	{
		return b;
	}

	/**
	 * @param b
	 *            the b to set
	 */
	public void setB( Point b )
	{
		this.b = b;
	}

}
