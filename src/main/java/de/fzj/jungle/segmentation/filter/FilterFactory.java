package de.fzj.jungle.segmentation.filter;

import java.util.HashMap;
import java.util.Map;

import de.fzj.jungle.segmentation.Cell;

/**
 * Provides easy access to {@link Filter}s.
 * 
 * @author Stefan Helfrich
 */
public class FilterFactory
{

	static final Map< String, Class< ? extends Filter< Cell > > > internalMap;

	static
	{
		internalMap = new HashMap<>();
		internalMap.put( "Size Filter", SizeFilter.class );
		internalMap.put( "Convex Hull Filter", ConvexHullFilter.class );
	}

	@Deprecated
	public static Filter< Cell > createFilter( String s, int... sizes )
	{
		if ( s.equals( "Size Filter" ) )
		{
			SizeFilterSettings settings = new SizeFilterSettings( sizes[ 0 ], sizes[ 1 ], sizes[ 2 ] );
			return new SizeFilter( settings );
		}
		else if ( s.equals( "Convex Hull Filter" ) )
		{
			// parameters don't fit the string
		}

		return null;
	}

	@Deprecated
	public static Filter< Cell > createFilter( String s, double deviation )
	{
		if ( s.equals( "Size Filter" ) )
		{
			return new SizeFilter( null );
		}
		else if ( s.equals( "Convex Hull Filter" ) )
		{
			ConvexHullFilterSettings settings = new ConvexHullFilterSettings( deviation );
			return new ConvexHullFilter( settings );
		}

		return null;
	}

	public static Filter< Cell > createFilter( String s, Object... objects )
	{
		if ( s.equals( "Size Filter" ) )
		{
			return new SizeFilter( ( Double ) objects[ 0 ], ( Double ) objects[ 1 ], ( Double ) objects[ 2 ] );
		}
		else if ( s.equals( "Convex Hull Filter" ) )
		{
			ConvexHullFilterSettings settings = new ConvexHullFilterSettings( ( Double ) objects[ 3 ] );
			return new ConvexHullFilter( settings );
		}
		else if ( s.equals( "Size + Convex Hull Filter" ) )
		{
			// We leave objects[1] out because only use the sizefilter for
			// preprocessing
			SizeFilterSettings sizeSettings = new SizeFilterSettings( ( Double ) objects[ 0 ], ( Double ) objects[ 2 ], ( Double ) objects[ 2 ] );
			ConvexHullFilterSettings convexSettings = new ConvexHullFilterSettings( ( Double ) objects[ 3 ] );
			return new SizeConvexHullFilter( sizeSettings, convexSettings );
		}

		return null;
	}

}
