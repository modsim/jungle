package de.fzj.jungle.trackmate.features.spot;

import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.CRIMSON_FLUORESCENCE_MEAN;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.CRIMSON_FLUORESCENCE_STDDEV;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.CRIMSON_FLUORESCENCE_TOTAL;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.PIXELS;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.YFP_FLUORESCENCE_MEAN;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.YFP_FLUORESCENCE_STDDEV;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.YFP_FLUORESCENCE_TOTAL;

import java.awt.geom.GeneralPath;
import java.util.Iterator;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.roi.GeneralPathRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.RealSum;

import de.fzj.jungle.segmentation.Cell;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.IndependentSpotFeatureAnalyzer;
import ij.gui.Roi;

/**
 * Extracts fluorescence intensities from the {@link Roi} that is associated
 * with a {@link Spot}.
 * 
 * @author Stefan Helfrich
 *
 * @param <T>
 *            type of input
 */
public class SpotFluorescenceAnalyzer< T extends RealType< T > > extends IndependentSpotFeatureAnalyzer< T >
{

	/*
	 * Fields
	 */
	public SpotFluorescenceAnalyzer( ImgPlus< T > img, Iterator< Spot > spots )
	{
		super( img, spots );
	}

	@Override
	public void process( Spot spot )
	{
		// If we don't have fluorescence channels, we don't need to process the
		// spot
		// If spot is not a Cell we don't process it
		try
		{
			if ( img.dimension( 2 ) < 2 || !( spot instanceof Cell ) )
			{
				spot.putFeature( PIXELS, 0.0d );
				spot.putFeature( YFP_FLUORESCENCE_TOTAL, 0.0d );
				spot.putFeature( YFP_FLUORESCENCE_MEAN, 0.0d );
				spot.putFeature( YFP_FLUORESCENCE_STDDEV, 0.0d );
				spot.putFeature( CRIMSON_FLUORESCENCE_TOTAL, 0.0d );
				spot.putFeature( CRIMSON_FLUORESCENCE_MEAN, 0.0d );
				spot.putFeature( CRIMSON_FLUORESCENCE_STDDEV, 0.0d );
				return;
			}
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			// There seems to be problem with some inputs where
			// ImgPlus.dimension() throws an exception for single channel
			// stacks.
			spot.putFeature( PIXELS, 0.0d );
			spot.putFeature( YFP_FLUORESCENCE_TOTAL, 0.0d );
			spot.putFeature( YFP_FLUORESCENCE_MEAN, 0.0d );
			spot.putFeature( YFP_FLUORESCENCE_STDDEV, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_TOTAL, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_MEAN, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_STDDEV, 0.0d );
			return;
		}

		/*
		 * for each pixel in roi: for each channel: get value and add to
		 * intensity sum divide intensitie sums by number of pixels (size of
		 * roi) for average
		 */
		Roi roi = ( ( Cell ) spot ).getCellRoi();

		if ( roi.getBounds().width <= 1 || roi.getBounds().height <= 1 )
		{
			spot.putFeature( PIXELS, 0.0d );
			spot.putFeature( YFP_FLUORESCENCE_TOTAL, 0.0d );
			spot.putFeature( YFP_FLUORESCENCE_MEAN, 0.0d );
			spot.putFeature( YFP_FLUORESCENCE_STDDEV, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_TOTAL, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_MEAN, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_STDDEV, 0.0d );
			return;
		}

		// Convert Roi (IJ1) to RegionOfInterest (IJ2)
		GeneralPathRegionOfInterest roi_ = new GeneralPathRegionOfInterest();
		roi_.setGeneralPath( new GeneralPath( roi.getPolygon() ) );

		/*
		 * img.dimension(2) == channels img.dimension(3) == frames
		 */
		// Array to store information for n channels
		int numberOfChannels = ( int ) img.dimension( 2 );

		RealSum[] intensitySums = new RealSum[ numberOfChannels ];
		RealSum[] helperSums = new RealSum[ numberOfChannels ];

		// Initialize intensitySums
		for ( int channelNumber = 0; channelNumber < numberOfChannels; channelNumber++ )
		{
			intensitySums[ channelNumber ] = new RealSum();
			helperSums[ channelNumber ] = new RealSum();
		}

		int[] pixelNumbers = new int[ numberOfChannels ];

		for ( int channelNumber = 0; channelNumber < numberOfChannels; channelNumber++ )
		{
			IterableInterval< T > channel = roi_.getIterableIntervalOverROI( HyperSliceImgPlus.fixChannelAxis( img, channelNumber ) );
			Cursor< T > channelCursor = channel.localizingCursor();

			while ( channelCursor.hasNext() )
			{
				channelCursor.fwd();
				T pixelValue = channelCursor.get();
				intensitySums[ channelNumber ].add( pixelValue.getRealDouble() );
				helperSums[ channelNumber ].add( Math.pow( pixelValue.getRealDouble(), 2 ) );
				pixelNumbers[ channelNumber ]++;
			}
		}

		/*
		 * Compute mean and standard deviation
		 */
		// http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
		// N = pixelNumbers[i]
		// s_2 = helperSums[i].getSum()
		// s_1 = intensitySums[i].getSum()
		double[] intensityAverages = new double[ numberOfChannels ];
		double[] standardDeviations = new double[ numberOfChannels ];

		for ( int i = 0; i < intensitySums.length; i++ )
		{
			intensityAverages[ i ] = intensitySums[ i ].getSum() / pixelNumbers[ i ];
			standardDeviations[ i ] = Math.sqrt( pixelNumbers[ i ] * helperSums[ i ].getSum() - Math.pow( intensitySums[ i ].getSum(), 2 ) ) / ( pixelNumbers[ i ] * ( pixelNumbers[ i ] - 1 ) );
		}

		/*
		 * Write computed features to the spot that is being processed
		 */
		// TODO Remove hardcoded assignment
		spot.putFeature( PIXELS, Double.valueOf( pixelNumbers[ 1 ] ) );
		spot.putFeature( YFP_FLUORESCENCE_TOTAL, intensitySums[ 1 ].getSum() );
		spot.putFeature( YFP_FLUORESCENCE_MEAN, intensityAverages[ 1 ] );
		spot.putFeature( YFP_FLUORESCENCE_STDDEV, standardDeviations[ 1 ] );
		try
		{
			spot.putFeature( CRIMSON_FLUORESCENCE_TOTAL, intensitySums[ 2 ].getSum() );
			spot.putFeature( CRIMSON_FLUORESCENCE_MEAN, intensityAverages[ 2 ] );
			spot.putFeature( CRIMSON_FLUORESCENCE_STDDEV, standardDeviations[ 2 ] );
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			spot.putFeature( CRIMSON_FLUORESCENCE_TOTAL, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_MEAN, 0.0d );
			spot.putFeature( CRIMSON_FLUORESCENCE_STDDEV, 0.0d );
		}
	}

}
