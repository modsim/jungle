package de.fzj.jungle.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * {@link SpotAnalyzerFactory} for a {@link SpotFluorescenceAnalyzer}.
 * 
 * @author Stefan Helfrich
 *
 * @param <T>
 *            type of input
 */
@Plugin( type = SpotAnalyzerFactory.class )
public class SpotFluorescenceAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements SpotAnalyzerFactory< T >
{

	/*
	 * Constants
	 */
	public static final String KEY = "SPOT_FLUORESCENCE_ANALYZER";

	public static final String PIXELS = "PIXELS";

	public static final String YFP_FLUORESCENCE_TOTAL = "YFP_FLUORESCENCE_TOTAL";

	public static final String YFP_FLUORESCENCE_MEAN = "YFP_FLUORESCENCE_MEAN";

	public static final String YFP_FLUORESCENCE_STDDEV = "YFP_FLUORESCENCE_STDDEV";

	public static final String CRIMSON_FLUORESCENCE_TOTAL = "CRIMSON_FLUORESCENCE_TOTAL";

	public static final String CRIMSON_FLUORESCENCE_MEAN = "CRIMSON_FLUORESCENCE_MEAN";

	public static final String CRIMSON_FLUORESCENCE_STDDEV = "CRIMSON_FLUORESCENCE_STDDEV";

	public static final ArrayList< String > FEATURES = new ArrayList< >( 7 );

	public static final HashMap< String, String > FEATURE_NAMES = new HashMap< >( 7 );

	public static final HashMap< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 7 );

	public static final HashMap< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 7 );

	private static final Map< String, Boolean > IS_INT = new HashMap< >( 7 );

	static
	{
		FEATURES.add( PIXELS );
		FEATURES.add( YFP_FLUORESCENCE_TOTAL );
		FEATURES.add( YFP_FLUORESCENCE_MEAN );
		FEATURES.add( YFP_FLUORESCENCE_STDDEV );
		FEATURES.add( CRIMSON_FLUORESCENCE_TOTAL );
		FEATURES.add( CRIMSON_FLUORESCENCE_MEAN );
		FEATURES.add( CRIMSON_FLUORESCENCE_STDDEV );

		FEATURE_NAMES.put( PIXELS, "Pixels" );
		FEATURE_NAMES.put( YFP_FLUORESCENCE_TOTAL, "YFP Total" );
		FEATURE_NAMES.put( YFP_FLUORESCENCE_MEAN, "YFP Mean" );
		FEATURE_NAMES.put( YFP_FLUORESCENCE_STDDEV, "YFP StdDev" );
		FEATURE_NAMES.put( CRIMSON_FLUORESCENCE_TOTAL, "Crimson Total" );
		FEATURE_NAMES.put( CRIMSON_FLUORESCENCE_MEAN, "Crimson Mean" );
		FEATURE_NAMES.put( CRIMSON_FLUORESCENCE_STDDEV, "Crimson StdDev" );

		FEATURE_SHORT_NAMES.put( PIXELS, "Pixels" );
		FEATURE_SHORT_NAMES.put( YFP_FLUORESCENCE_TOTAL, "YFPTot" );
		FEATURE_SHORT_NAMES.put( YFP_FLUORESCENCE_MEAN, "YFP" );
		FEATURE_SHORT_NAMES.put( YFP_FLUORESCENCE_STDDEV, "YFP SD" );
		FEATURE_SHORT_NAMES.put( CRIMSON_FLUORESCENCE_TOTAL, "CrimsonTot" );
		FEATURE_SHORT_NAMES.put( CRIMSON_FLUORESCENCE_MEAN, "Crimson" );
		FEATURE_SHORT_NAMES.put( CRIMSON_FLUORESCENCE_STDDEV, "Crimson SD" );

		FEATURE_DIMENSIONS.put( PIXELS, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( YFP_FLUORESCENCE_TOTAL, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( YFP_FLUORESCENCE_MEAN, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( YFP_FLUORESCENCE_STDDEV, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( CRIMSON_FLUORESCENCE_TOTAL, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( CRIMSON_FLUORESCENCE_MEAN, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( CRIMSON_FLUORESCENCE_STDDEV, Dimension.INTENSITY );

		IS_INT.put( PIXELS, Boolean.FALSE );
		IS_INT.put( YFP_FLUORESCENCE_TOTAL, Boolean.FALSE );
		IS_INT.put( YFP_FLUORESCENCE_MEAN, Boolean.FALSE );
		IS_INT.put( YFP_FLUORESCENCE_STDDEV, Boolean.FALSE );
		IS_INT.put( CRIMSON_FLUORESCENCE_TOTAL, Boolean.FALSE );
		IS_INT.put( CRIMSON_FLUORESCENCE_MEAN, Boolean.FALSE );
		IS_INT.put( CRIMSON_FLUORESCENCE_STDDEV, Boolean.FALSE );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public SpotAnalyzer< T > getAnalyzer( Model model, ImgPlus< T > img, int frame, int channel )
	{
		/*
		 * Since we want to compute fluorescence values for all channels, we
		 * drop the frame argument.
		 */
		final ImgPlus< T > imgT = HyperSliceImgPlus.fixTimeAxis( img, frame );
		final Iterator< Spot > spots = model.getSpots().iterator( frame, false );

		return new SpotFluorescenceAnalyzer< >( imgT, spots );
	}

	@Override
	public String getInfoText()
	{
		// TODO Add info text 
		return null;
	}

	@Override
	public ImageIcon getIcon()
	{
		// TODO Add icon
		return null;
	}

	@Override
	public String getName()
	{
		return "Spot Fluorescence Analyzer";
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

}
