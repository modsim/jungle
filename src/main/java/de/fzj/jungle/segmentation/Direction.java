/**
 * 
 */
package de.fzj.jungle.segmentation;

/**
 * Encodes directions in two-dimensional space.
 * 
 * @author Stefan Helfrich
 */
public enum Direction
{
	RIGHT( 1, 0 ), BOTTOM_RIGHT( 1, 1 ), BOTTOM( 0, 1 ), BOTTOM_LEFT( -1, 1 ), LEFT( -1, 0 ), TOP_LEFT( -1, -1 ), TOP( 0, -1 ), TOP_RIGHT( 1, -1 );

	private int dx;

	private int dy;

	Direction( int dx, int dy )
	{
		this.dx = dx;
		this.dy = dy;
	}

	public static Direction getDirection( int dx, int dy )
	{
		if ( dy == -1 )
		{
			if ( dx == -1 )
				return TOP_LEFT;
			if ( dx == 0 )
				return TOP;
			if ( dx == 1 )
				return TOP_RIGHT;
		}
		else
		{
			if ( dy == 0 )
			{
				if ( dx == -1 )
					return LEFT;
				if ( dx == 0 )
					return null;
				if ( dx == 1 )
					return RIGHT;
			}
			else
			{
				if ( dy == 1 )
				{
					if ( dx == -1 )
						return BOTTOM_LEFT;
					if ( dx == 0 )
						return BOTTOM;
					if ( dx == 1 )
						return BOTTOM_RIGHT;
				}
			}
		}

		return null;
	}

	public Direction getOpposite()
	{
		/*
		 * Since the directions are initialized clockwise, adding 4 to the
		 * ordinal-value will result in the opposite direction (modulo 8).
		 */
		return Direction.values()[ ( ( this.ordinal() + 4 ) % 8 ) ];

		/* The more understandable implementation */
		// switch(this){
		// case TOP: return BOTTOM;
		// case TOP_RIGHT: return BOTTOM_LEFT;
		// case RIGHT: return LEFT;
		// case BOTTOM_RIGHT: return TOP_LEFT;
		// case BOTTOM: return TOP;
		// case BOTTOM_LEFT: return TOP_RIGHT;
		// case LEFT: return RIGHT;
		// case TOP_LEFT: return BOTTOM_RIGHT;
		// default: return null;
		// }
	}

	public Direction getNormalToOutside()
	{
		/*
		 * Since the directions are initialized clockwise, adding 6 to the
		 * ordinal-value will result in the direction point to the outside of
		 * the contour.
		 */
		return Direction.values()[ ( ( this.ordinal() + 6 ) % 8 ) ];
	}

	/**
	 * @return the dx
	 */
	public int getDx()
	{
		return dx;
	}

	/**
	 * @return the dy
	 */
	public int getDy()
	{
		return dy;
	}

}
