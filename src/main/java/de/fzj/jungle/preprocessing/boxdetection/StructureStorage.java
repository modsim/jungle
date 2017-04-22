package de.fzj.jungle.preprocessing.boxdetection;

import java.util.ArrayList;
import java.util.List;

/**
 * Central storage for predefined {@link Structure}s.
 * 
 * @author Stefan Helfrich
 */
public class StructureStorage
{

	private static List< Structure > structures = new ArrayList<>( 2 );

	static
	{
		structures.add( new Structure( 0.65 ) );
		structures.add( new Structure( 0.64 ) );
		structures.add( new Structure( 0.73 ) );
		structures.add( new Structure( 0.74 ) );
		structures.add( new Structure( 1.00 ) );
	}

	public static List< Structure > getStructures()
	{
		return structures;
	}
}
