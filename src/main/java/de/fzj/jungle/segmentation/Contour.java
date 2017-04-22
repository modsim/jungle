package de.fzj.jungle.segmentation;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Contour of a {@link Cell}.
 * 
 * @author Stefan Helfrich
 */
@Deprecated
public class Contour extends LinkedList< Coordinate >
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private CustomArea enclosedArea;

	/**
	 * @param coordinateList
	 */
	public Contour( List< Coordinate > coordinateList )
	{
		super( coordinateList );

		Path2D.Double path = new Path2D.Double();

		Coordinate first = coordinateList.get( 0 );
		path.moveTo( first.getX(), first.getY() );
		for ( Coordinate c : coordinateList )
		{
			if ( c != first )
			{
				path.lineTo( c.getX(), c.getY() );
			}
		}
		path.lineTo( first.getX(), first.getY() );

		enclosedArea = new CustomArea( path );
	}

	/**
	 * 
	 */
	public Contour()
	{
		super();

		enclosedArea = new CustomArea();
	}

	public ContourIter iterator( Coordinate A )
	{
		return new ContourIter( A );
	}

	public ContourIter iterator( Coordinate A, Coordinate B )
	{
		return new ContourIter( A, B );
	}

	private class ContourIter implements Iterator< Coordinate >
	{

		Coordinate nextCoordinate;

		Coordinate endCoordinate;

		public ContourIter( Coordinate start )
		{
			this.endCoordinate = Contour.this.get( Contour.this.indexOf( start ) - 1 );
			this.nextCoordinate = start;
		}

		public ContourIter( Coordinate start, Coordinate end )
		{
			this.endCoordinate = end;
			this.nextCoordinate = start;
		}

		@Override
		public boolean hasNext()
		{
			return this.nextCoordinate != this.endCoordinate;
		}

		@Override
		public Coordinate next()
		{
			Coordinate temp = this.nextCoordinate;
			Contour contour = Contour.this;
			this.nextCoordinate = contour.get( contour.indexOf( this.nextCoordinate ) + 1 );

			return temp;
		}

		@Override
		public void remove()
		{
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException( "CountourIter.remove() not yet implemented." );
		}

	}

	@Override
	public Coordinate get( int index )
	{
		if ( index < 0 ) { return this.get( this.size() + ( index % this.size() ) ); }
		if ( index >= this.size() ) { return this.get( index % this.size() ); }

		return super.get( index );
	}

	/**
	 * @return The enclosed area
	 */
	public Area getEnclosedArea()
	{
		return enclosedArea;
	}

	/**
	 * @param enclosedArea
	 *            The enclosed area to set
	 */
	public void setEnclosedArea( CustomArea enclosedArea )
	{
		this.enclosedArea = enclosedArea;
	}

	public void updateEnclosedArea()
	{
		Path2D.Double path = new Path2D.Double();

		Coordinate first = this.get( 0 );
		path.moveTo( first.getX(), first.getY() );
		for ( Coordinate c : this )
		{
			if ( c != first )
			{
				path.lineTo( c.getX(), c.getY() );
			}
		}
		path.lineTo( first.getX(), first.getY() );

		enclosedArea = new CustomArea( path );
	}

	public double getNormalAngle( Coordinate c )
	{
		// TODO Implement boundary conditions
		int idx = this.indexOf( c );
		Coordinate previous = this.get( idx - 1 );
		Coordinate next = this.get( idx + 1 );

		double angle = calculateAngleToXAxis( previous, next );
		double normal = calculateAngleOfNormal( angle );

		return normal;
	}

	public double calculateAngleToXAxis( Coordinate c1, Coordinate c2 )
	{
		int deltaX = Math.abs( c2.getX() - c1.getX() );
		int deltaY = Math.abs( c2.getY() - c1.getY() );

		return Math.atan( deltaY / deltaX );
	}

	private double calculateAngleOfNormal( double angle )
	{
		return angle + 0.5 * Math.PI;
	}

	/**
	 * @return a list containing coordinates that have been removed from the
	 *         contour by the sorting procedure.
	 */
	public List< Coordinate > sortContour()
	{
		// Start with a more or less randomly chosen coordinate (because the
		// list is not sorted)
		Coordinate start;
		try
		{
			start = this.getFirst();
		}
		catch ( NoSuchElementException ex )
		{
			return new LinkedList<>();
		}

		// Initialize empty list that later will hold the sorted coordinates
		LinkedList< Coordinate > sortedContour = new LinkedList<>();

		// Initialization according to "Digital Image Processing"
		FindNextCoordinateResult tempResult = findNextCoordinate( start, Direction.TOP, Direction.RIGHT ); // Get
																											// coordinate
		Coordinate startPlusOne = tempResult.getCoordinate();

		Coordinate currentCoordinate = startPlusOne;

		Direction previousDirection = tempResult.getDirection();

		do
		{
			// loop over directions starting with outside pointing direction
			Direction searchDirection = previousDirection.getNormalToOutside();

			FindNextCoordinateResult result = findNextCoordinate( currentCoordinate, searchDirection, previousDirection );

			if ( result.getCoordinate() != null )
			{
				sortedContour.add( result.getCoordinate() );

				currentCoordinate = result.getCoordinate();
				previousDirection = result.getDirection();
			}
			else
			{
				currentCoordinate = start;
				previousDirection = result.getDirection();
			}
		}
		while ( currentCoordinate != startPlusOne && getNeighbour( currentCoordinate, previousDirection.getOpposite() ) != start );

		List< Coordinate > tempContour = new LinkedList<>( this );
		tempContour.removeAll( sortedContour );

		this.clear();
		this.addAll( sortedContour );

		this.updateEnclosedArea();

		return tempContour;
	}

	private boolean hasNeighbour( Coordinate coordinate, Direction direction )
	{
		int neighbourX = coordinate.getX() + direction.getDx();
		int neighbourY = coordinate.getY() + direction.getDy();

		return this.contains( new Coordinate( ( short ) neighbourX, ( short ) neighbourY ) );
	}

	private Coordinate getNeighbour( Coordinate coordinate, Direction direction )
	{
		int neighbourX = coordinate.getX() + direction.getDx();
		int neighbourY = coordinate.getY() + direction.getDy();

		int index = this.indexOf( new Coordinate( ( short ) neighbourX, ( short ) neighbourY ) );
		return this.get( index );
	}

	private FindNextCoordinateResult findNextCoordinate( Coordinate currentCoordinate, Direction searchDirection, Direction previousDirection )
	{
		for ( int i = 0; i < Direction.values().length - 1; ++i )
		{
			Direction currentDirection = Direction.values()[ ( ( searchDirection.ordinal() + i ) % 8 ) ];

			if ( hasNeighbour( currentCoordinate, currentDirection ) && ( currentDirection != previousDirection.getOpposite() ) )
			{
				Coordinate neighbour = getNeighbour( currentCoordinate, currentDirection );

				return new FindNextCoordinateResult( neighbour, currentDirection );
			}
		}

		return new FindNextCoordinateResult( null, searchDirection );
	}

	/**
	 * Composite of a {@link Coordinate} and a {@link Direction}.
	 * 
	 * @author Stefan Helfrich
	 */
	private class FindNextCoordinateResult
	{
		private Coordinate coordinate;

		private Direction direction;

		public FindNextCoordinateResult( Coordinate coordinate, Direction direction )
		{
			this.coordinate = coordinate;
			this.direction = direction;
		}

		public Coordinate getCoordinate()
		{
			return this.coordinate;
		}

		@SuppressWarnings( "unused" )
		public void setCoordinate( Coordinate coordinate )
		{
			this.coordinate = coordinate;
		}

		public Direction getDirection()
		{
			return this.direction;
		}

		@SuppressWarnings( "unused" )
		public void setDirection( Direction direction )
		{
			this.direction = direction;
		}
	}
}
