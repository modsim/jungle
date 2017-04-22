package de.fzj.jungle.preprocessing.boxdetection;

import java.util.List;
import java.util.TreeSet;

import de.fzj.jungle.preprocessing.hough.LinearHT;
import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLine;
import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLinePair;

/**
 * Classifies a growth chamber according to some predefined ratios.
 * 
 * @author Stefan Helfrich
 */
public class RatioBoxClassifier implements BoxClassifier
{

	@Override
	public TreeSet< HoughRectangle > classify( List< HoughLine > lines )
	{
		/*
		 * 1. Check if distances matches one of the widths 2. Check if other
		 * distances match the associated height 3. If that's the case store
		 * both it in boxHoughLines as a pair
		 */

		TreeSet< HoughLinePair > linePairs = new TreeSet<>();

		for ( HoughLine h1 : lines )
		{
			for ( HoughLine h2 : lines )
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
					HoughLinePair pair = new LinearHT.HoughLinePair( h1, h2, h1.getCount() + h2.getCount() );
					linePairs.add( pair );
				}
			}
		}

		TreeSet< HoughRectangle > houghRectangles = new TreeSet<>();

		for ( HoughLinePair pair1 : linePairs )
		{
			for ( HoughLinePair pair2 : linePairs )
			{
				if ( pair1 == pair2 )
				{
					continue;
				}

				double distance1 = pair1.getDistance();
				double distance2 = pair2.getDistance();

				double angle1 = ( pair1.getH1().getAngle() > ( 3 * Math.PI / 4 ) ) ? ( Math.PI - pair1.getH1().getAngle() ) : pair1.getH1().getAngle();
				double angle2 = ( pair2.getH1().getAngle() > ( 3 * Math.PI / 4 ) ) ? ( Math.PI - pair2.getH1().getAngle() ) : pair2.getH1().getAngle();

				if ( Math.abs( angle1 - angle2 ) < Math.PI / 4 )
				{
					continue;
				}

				// For each structure check if the ratio fits
				for ( Structure s : StructureStorage.getStructures() )
				{
					double deviation = Math.abs( ( distance1 / distance2 ) - s.getRatio() );
					double deviationInverse = Math.abs( ( distance2 / distance1 ) - s.getRatio() );

					if ( deviation < BoxClassifier.RATIO_EPSILON || deviationInverse < BoxClassifier.RATIO_EPSILON )
					{
						HoughRectangle rect = new HoughRectangle( pair1, pair2 );
						houghRectangles.add( rect );
						break;
					}
				}
			}
		}

		return houghRectangles;
	}

}
