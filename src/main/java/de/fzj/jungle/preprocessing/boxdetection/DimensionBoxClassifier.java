package de.fzj.jungle.preprocessing.boxdetection;

import java.util.List;
import java.util.TreeSet;

import de.fzj.jungle.preprocessing.hough.LinearHT;
import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLine;
import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLinePair;

/**
 * Classifies a growth chamber according to absolute dimensions.
 * 
 * @author Stefan Helfrich
 */
public class DimensionBoxClassifier implements BoxClassifier
{

	@Override
	public TreeSet< HoughRectangle > classify( List< HoughLine > lines )
	{
		/*
		 * 1. Check if distances matches one of the widths 2. Check if other
		 * distances match the associated height 3. If that's the case store
		 * both it in boxHoughLines as a pair
		 */

		TreeSet< HoughLinePair > linePairsWidth = new TreeSet<>();
		TreeSet< HoughLinePair > linePairsHeight = new TreeSet<>();

		for ( HoughLine h1 : lines )
		{
			secondLineLoop: for ( HoughLine h2 : lines )
			{
				// compare lines with similar angle
				if ( h1.equals( h2 ) )
				{
					continue;
				}

				/*
				 * Only look at lines with same orientation, i.e., same angle,
				 * and opposite sides of the center. We need a little hack here,
				 * since angles around pi are close to angle near 0 ..
				 */
				double angle1 = ( h1.getAngle() > ( 3 * Math.PI / 4 ) ) ? ( Math.PI - h1.getAngle() ) : h1.getAngle();
				double angle2 = ( h2.getAngle() > ( 3 * Math.PI / 4 ) ) ? ( Math.PI - h2.getAngle() ) : h2.getAngle();

				if ( ( Math.abs( angle1 - angle2 ) < 0.05 ) && ( Math.signum( h1.getRadius() ) != Math.signum( h2.getRadius() ) ) )
				{
					// Get distance from the center of the image ("radius")
					double r1 = Math.abs( h1.getRadius() );
					double r2 = Math.abs( h2.getRadius() );

					// check for box width
					for ( Structure s : StructureStorage.getStructures() )
					{
						int width = s.getWidth();

						if ( Math.abs( r1 + r2 - width ) < BoxClassifier.DIMENSION_EPSILON )
						{
							HoughLinePair pair = new LinearHT.HoughLinePair( h1, h2, h1.getCount() + h2.getCount() );
							linePairsWidth.add( pair );
							continue secondLineLoop;
						}
					}

					// check for box height
					for ( Structure s : StructureStorage.getStructures() )
					{
						int height = s.getHeight();

						if ( Math.abs( r1 + r2 - height ) < BoxClassifier.DIMENSION_EPSILON )
						{
							HoughLinePair pair = new LinearHT.HoughLinePair( h1, h2, h1.getCount() + h2.getCount() );
							linePairsHeight.add( pair );
						}
					}
				}
			}
		}

		TreeSet< HoughRectangle > houghRectangles = new TreeSet<>();

		for ( HoughLinePair pair1 : linePairsWidth )
		{
			for ( Structure s : StructureStorage.getStructures() )
			{
				if ( Math.abs( pair1.getDistance() - s.getWidth() ) < BoxClassifier.DIMENSION_EPSILON )
				{
					for ( HoughLinePair pair2 : linePairsHeight )
					{
						if ( Math.abs( pair2.getDistance() - s.getHeight() ) < BoxClassifier.DIMENSION_EPSILON )
						{
							HoughRectangle rect = new HoughRectangle( pair1, pair2 );
							houghRectangles.add( rect );
						}
					}
				}
			}
		}

		return houghRectangles;
	}

}
