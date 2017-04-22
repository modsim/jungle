package de.fzj.jungle.segmentation;

import java.io.Serializable;

/**
 * Encapsulates a three-dimensional coordinate.
 * 
 * @author Stefan Helfrich
 */
public class Coordinate implements Serializable
{

	private static final long serialVersionUID = -5544385786586770383L;

	// The coordinates
	public short x, y, z;

	/**
	 * Creates a new instance of Coordinate
	 * 
	 * @param x
	 *            The X coordinate
	 * @param y
	 *            The Y coordinate
	 * @param z
	 *            The Z coordinate
	 */
	public Coordinate( short x, short y, short z )
	{
		this.setX( x );
		this.setY( y );
		this.setZ( z );
	}

	/**
	 * Creates a new instance of Coordinate
	 * 
	 * @param x
	 *            The X coordinate
	 * @param y
	 *            The Y coordinate
	 */
	public Coordinate( short x, short y )
	{
		this.setX( x );
		this.setY( y );
	}

	/**
	 * Returns the X coordinate
	 * 
	 * @return The X coordinate
	 */
	public int getX()
	{
		return x;
	}

	/**
	 * Sets the X coordinate
	 * 
	 * @param x
	 *            The X coordinate to be set
	 */
	public void setX( short x )
	{
		this.x = x;
	}

	/**
	 * Returns the Y coordinate
	 * 
	 * @return The Y coordinate
	 */
	public int getY()
	{
		return y;
	}

	/**
	 * Sets the Y coordinate
	 * 
	 * @param y
	 *            The Y coordinate to be set
	 */
	public void setY( short y )
	{
		this.y = y;
	}

	/**
	 * Returns the X coordinate
	 * 
	 * @return The X coordinate
	 */
	public int getZ()
	{
		return z;
	}

	@Override
	public String toString()
	{
		return "(" + this.getX() + "," + this.getY() + ")";

	}

	/**
	 * Sets the Z coordinate
	 * 
	 * @param z
	 *            The Z coordinate to be set
	 */
	public void setZ( short z )
	{
		this.z = z;
	}

	/**
	 * Computes the euclidean distance between this point and the give point.
	 * 
	 * @param c
	 *            the second point to which the distance is to be computed
	 * @return the euclidean distance between to Coordinates.
	 */
	public double computeEuclideanDistance( Coordinate c )
	{
		double squaredDifferenceX = Math.pow( this.getX() - c.getX(), 2 );
		double squaredDifferenceY = Math.pow( this.getY() - c.getY(), 2 );

		return Math.sqrt( squaredDifferenceX + squaredDifferenceY );
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		Coordinate other = ( Coordinate ) obj;
		if ( x != other.x )
			return false;
		if ( y != other.y )
			return false;
		if ( z != other.z )
			return false;
		return true;
	}
}
