package de.fzj.jungle.preprocessing.registration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * Registers a time series with multiple channels. In the original
 * implementation, StackReg_ execution failed for multiple channels.
 * 
 * Transformations are computed for the first channel and are applied to all
 * other channels. Hence, this will not solve issues with chromatic abberations.
 * 
 * @author Stefan Helfrich
 */
public class MultiChannelStackReg_ implements PlugIn, PlugInFilter
{

	private ImagePlus imp;

	private int tSlice;

	private Transformation transformation;

	private ImagePlus finalImp;

	@Override
	public int setup( String args, ImagePlus imp )
	{
		this.imp = imp;

		return DOES_ALL;
	}

	@Override
	public void run( String args )
	{
		Runtime.getRuntime().gc();

		imp = WindowManager.getCurrentImage();
		if ( imp == null )
		{
			IJ.error( "No image available" );
			return;
		}

		process( imp );
	}

	@Override
	public void run( ImageProcessor ip )
	{
		// NB: not used
	}

	public ImagePlus process( ImagePlus imp )
	{
		finalImp = IJ.createHyperStack( null, imp.getWidth(), imp.getHeight(), imp.getNChannels(), imp.getNSlices(), imp.getNFrames(), imp.getBitDepth() );
		finalImp.setCalibration( imp.getCalibration() );

		final int width = imp.getWidth();
		final int height = imp.getHeight();
		final int targetSlice = 1;
		tSlice = targetSlice;
		transformation = Transformation.TRANSLATION;

		// Identity matrix
		double[][] globalTransform = { { 1.0, 0.0, 0.0 }, { 0.0, 1.0, 0.0 }, { 0.0, 0.0, 1.0 } };

		double[][] anchorPoints = null;

		switch ( transformation )
		{
		case TRANSLATION:
		{
			anchorPoints = new double[ 1 ][ 3 ];
			anchorPoints[ 0 ][ 0 ] = width / 2;
			anchorPoints[ 0 ][ 1 ] = height / 2;
			anchorPoints[ 0 ][ 2 ] = 1.0;
			break;
		}
		case RIGID_BODY:
		{
			anchorPoints = new double[ 3 ][ 3 ];
			anchorPoints[ 0 ][ 0 ] = width / 2;
			anchorPoints[ 0 ][ 1 ] = height / 2;
			anchorPoints[ 0 ][ 2 ] = 1.0;
			anchorPoints[ 1 ][ 0 ] = width / 2;
			anchorPoints[ 1 ][ 1 ] = height / 4;
			anchorPoints[ 1 ][ 2 ] = 1.0;
			anchorPoints[ 2 ][ 0 ] = width / 2;
			anchorPoints[ 2 ][ 1 ] = ( 3 * height ) / 4;
			anchorPoints[ 2 ][ 2 ] = 1.0;
			break;
		}
		default:
		{
			IJ.error( "Unexpected transformation" );
			return null;
		}
		}

		// ImagePlus instances with a single ImageProcessor
		ImagePlus source = null;
		ImagePlus target = null;
		double[] colorWeights = null;

		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
		{
			target = new ImagePlus( "StackRegTarget", new ByteProcessor( width, height, new byte[ width * height ], imp.getProcessor().getColorModel() ) );
			target.getProcessor().copyBits( imp.getProcessor(), 0, 0, Blitter.COPY );
			break;
		}
		case ImagePlus.GRAY16:
		{
			target = new ImagePlus( "StackRegTarget", new ShortProcessor( width, height, new short[ width * height ], imp.getProcessor().getColorModel() ) );
			target.getProcessor().copyBits( imp.getProcessor(), 0, 0, Blitter.COPY );
			break;
		}
		default:
		{
			IJ.error( "Unexpected image type" );
			return null;
		}
		}

		// Before we start the registration process, copy the target slice (with
		// all channels) to finalImp
		for ( int i = targetSlice; i < targetSlice + imp.getNChannels(); i++ )
		{
			imp.setSlice( i );
			finalImp.setSlice( i );

			finalImp.setProcessor( imp.getProcessor().duplicate() );
			finalImp.getImageStack().setProcessor( imp.getProcessor().duplicate(), i );
		}

		/*
		 * Registration
		 */
		for ( int s = targetSlice + imp.getNChannels(); ( s <= imp.getStackSize() ); s = s + imp.getNChannels() )
		{
			source = registerSlice( source, target, imp, width, height, transformation, globalTransform, anchorPoints, colorWeights, s );

			if ( source == null ) { return null; }
		}

		imp.setSlice( targetSlice );
		imp.updateAndDraw();
		return finalImp;
	}

	/**
	 * 
	 * @param source
	 * @param target
	 * @param imp
	 * @param width
	 * @param height
	 * @param transformation
	 * @param globalTransform
	 * @param anchorPoints
	 * @param colorWeights
	 * @param s
	 * @return
	 */
	private ImagePlus registerSlice( ImagePlus source, ImagePlus target, ImagePlus imp, final int width, final int height, final Transformation transformation, final double[][] globalTransform, final double[][] anchorPoints, final double[] colorWeights, int s )
	{
		imp.setSlice( s );
		finalImp.setSlice( s );
		try
		{
			Object turboReg = null;
			Method method = null;
			double[][] sourcePoints = null;
			double[][] targetPoints = null;
			double[][] localTransform = null;
			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
			{
				source = new ImagePlus( "StackRegSource", new ByteProcessor( width, height, ( byte[] ) imp.getProcessor().getPixels(), imp.getProcessor().getColorModel() ) );
				break;
			}
			case ImagePlus.GRAY16:
			{
				source = new ImagePlus( "StackRegSource", new ShortProcessor( width, height, ( short[] ) imp.getProcessor().getPixels(), imp.getProcessor().getColorModel() ) );
				break;
			}
			default:
			{
				IJ.error( "Unexpected image type" );
				return ( null );
			}
			}

			// Write source image to temp folder
			final FileSaver sourceFile = new FileSaver( source );
			final String sourcePathAndFileName = IJ.getDirectory( "temp" ) + source.getTitle();
			sourceFile.saveAsTiff( sourcePathAndFileName );

			// Write target image to temp folder
			final FileSaver targetFile = new FileSaver( target );
			final String targetPathAndFileName = IJ.getDirectory( "temp" ) + target.getTitle();
			targetFile.saveAsTiff( targetPathAndFileName );

			switch ( transformation )
			{
			case TRANSLATION:
			{
				turboReg = IJ.runPlugIn( "TurboReg_", "-align" + " -file " + sourcePathAndFileName + " 0 0 " + ( width - 1 ) + " " + ( height - 1 ) + " -file " + targetPathAndFileName + " 0 0 " + ( width - 1 ) + " " + ( height - 1 ) + " -translation" + " " + ( width / 2 ) + " " + ( height / 2 ) + " " + ( width / 2 ) + " " + ( height / 2 ) + " -hideOutput" );
				break;
			}
			case RIGID_BODY:
			{
				turboReg = IJ.runPlugIn( "TurboReg_", "-align" + " -file " + sourcePathAndFileName + " 0 0 " + ( width - 1 ) + " " + ( height - 1 ) + " -file " + targetPathAndFileName + " 0 0 " + ( width - 1 ) + " " + ( height - 1 ) + " -rigidBody" + " " + ( width / 2 ) + " " + ( height / 2 ) + " " + ( width / 2 ) + " " + ( height / 2 ) + " " + ( width / 2 ) + " " + ( height / 4 ) + " " + ( width / 2 ) + " " + ( height / 4 ) + " " + ( width / 2 ) + " " + ( ( 3 * height ) / 4 ) + " " + ( width / 2 ) + " " + ( ( 3 * height ) / 4 ) + " -hideOutput" );
			}
			default:
			{
				IJ.error( "Unexpected transformation" );
				return ( null );
			}
			}

			if ( turboReg == null ) { throw ( new ClassNotFoundException() ); }

			target.setProcessor( null, source.getProcessor() );
			method = turboReg.getClass().getMethod( "getSourcePoints", null );
			sourcePoints = ( ( double[][] ) method.invoke( turboReg, null ) );
			method = turboReg.getClass().getMethod( "getTargetPoints", null );
			targetPoints = ( ( double[][] ) method.invoke( turboReg, null ) );

			localTransform = getTransformationMatrix( targetPoints, sourcePoints );

			double[][] rescued = { { globalTransform[ 0 ][ 0 ], globalTransform[ 0 ][ 1 ], globalTransform[ 0 ][ 2 ] }, { globalTransform[ 1 ][ 0 ], globalTransform[ 1 ][ 1 ], globalTransform[ 1 ][ 2 ] }, { globalTransform[ 2 ][ 0 ], globalTransform[ 2 ][ 1 ], globalTransform[ 2 ][ 2 ] } };

			for ( int i = 0; ( i < 3 ); i++ )
			{
				for ( int j = 0; ( j < 3 ); j++ )
				{
					globalTransform[ i ][ j ] = 0.0;
					for ( int k = 0; ( k < 3 ); k++ )
					{
						globalTransform[ i ][ j ] += localTransform[ i ][ k ] * rescued[ k ][ j ];
					}
				}
			}

			switch ( imp.getType() )
			{
			case ImagePlus.GRAY8:
			case ImagePlus.GRAY16:
			{
				switch ( transformation )
				{
				case TRANSLATION:
				{
					sourcePoints = new double[ 1 ][ 3 ];
					for ( int i = 0; ( i < 3 ); i++ )
					{
						sourcePoints[ 0 ][ i ] = 0.0;
						for ( int j = 0; ( j < 3 ); j++ )
						{
							sourcePoints[ 0 ][ i ] += globalTransform[ i ][ j ] * anchorPoints[ 0 ][ j ];
						}
					}
					turboReg = IJ.runPlugIn( "TurboReg_", "-transform" + " -file " + sourcePathAndFileName + " " + width + " " + height + " -translation" + " " + sourcePoints[ 0 ][ 0 ] + " " + sourcePoints[ 0 ][ 1 ] + " " + ( width / 2 ) + " " + ( height / 2 ) + " -hideOutput" );
					break;
				}
				case RIGID_BODY:
				{
					sourcePoints = new double[ 3 ][ 3 ];
					for ( int i = 0; ( i < 3 ); i++ )
					{
						sourcePoints[ 0 ][ i ] = 0.0;
						sourcePoints[ 1 ][ i ] = 0.0;
						sourcePoints[ 2 ][ i ] = 0.0;
						for ( int j = 0; ( j < 3 ); j++ )
						{
							sourcePoints[ 0 ][ i ] += globalTransform[ i ][ j ] * anchorPoints[ 0 ][ j ];
							sourcePoints[ 1 ][ i ] += globalTransform[ i ][ j ] * anchorPoints[ 1 ][ j ];
							sourcePoints[ 2 ][ i ] += globalTransform[ i ][ j ] * anchorPoints[ 2 ][ j ];
						}
					}
					turboReg = IJ.runPlugIn( "TurboReg_", "-transform" + " -file " + sourcePathAndFileName + " " + width + " " + height + " -rigidBody" + " " + sourcePoints[ 0 ][ 0 ] + " " + sourcePoints[ 0 ][ 1 ] + " " + ( width / 2 ) + " " + ( height / 2 ) + " " + sourcePoints[ 1 ][ 0 ] + " " + sourcePoints[ 1 ][ 1 ] + " " + ( width / 2 ) + " " + ( height / 4 ) + " " + sourcePoints[ 2 ][ 0 ] + " " + sourcePoints[ 2 ][ 1 ] + " " + ( width / 2 ) + " " + ( ( 3 * height ) / 4 ) + " -hideOutput" );
					break;
				}
				default:
				{
					IJ.error( "Unexpected transformation" );
					return ( null );
				}
				}

				if ( turboReg == null ) { throw ( new ClassNotFoundException() ); }

				method = turboReg.getClass().getMethod( "getTransformedImage", null );
				ImagePlus transformedSource = ( ImagePlus ) method.invoke( turboReg, null );
				transformedSource.getStack().deleteLastSlice();

				switch ( imp.getType() )
				{
				case ImagePlus.GRAY8:
				{
					transformedSource.getProcessor().setMinAndMax( 0.0, 255.0 );
					final ImageConverter converter = new ImageConverter( transformedSource );
					converter.convertToGray8();
					break;
				}
				case ImagePlus.GRAY16:
				{
					transformedSource.getProcessor().setMinAndMax( 0.0, 65535.0 );
					final ImageConverter converter = new ImageConverter( transformedSource );
					converter.convertToGray16();
					break;
				}
				default:
				{
					IJ.error( "Unexpected image type" );
					return ( null );
				}
				}

				finalImp.getImageStack().setProcessor( transformedSource.getProcessor().duplicate(), s );
				finalImp.setProcessor( transformedSource.getProcessor().duplicate() );

				// This we have to do that for all additional channels
				for ( int channel = 1; channel < imp.getNChannels(); channel++ )
				{
					// Get ImagePlus from each channel
					imp.setSlice( s + channel );
					finalImp.setSlice( s + channel );
					ImagePlus channelImp = new ImagePlus( "ChannelImp" + channel, new ShortProcessor( width, height, ( short[] ) imp.getProcessor().getPixels(), imp.getProcessor().getColorModel() ) );

					final FileSaver channelFile = new FileSaver( channelImp );
					final String channelPathAndFileName = IJ.getDirectory( "temp" ) + channelImp.getTitle();
					channelFile.saveAsTiff( channelPathAndFileName );

					switch ( transformation )
					{
					case TRANSLATION:
					{
						turboReg = IJ.runPlugIn( "TurboReg_", "-transform" + " -file " + channelPathAndFileName + " " + width + " " + height + " -translation" + " " + sourcePoints[ 0 ][ 0 ] + " " + sourcePoints[ 0 ][ 1 ] + " " + ( width / 2 ) + " " + ( height / 2 ) + " -hideOutput" );
						break;
					}
					case RIGID_BODY:
					{
						turboReg = IJ.runPlugIn( "TurboReg_", "-transform" + " -file " + channelPathAndFileName + " " + width + " " + height + " -rigidBody" + " " + sourcePoints[ 0 ][ 0 ] + " " + sourcePoints[ 0 ][ 1 ] + " " + ( width / 2 ) + " " + ( height / 2 ) + " " + sourcePoints[ 1 ][ 0 ] + " " + sourcePoints[ 1 ][ 1 ] + " " + ( width / 2 ) + " " + ( height / 4 ) + " " + sourcePoints[ 2 ][ 0 ] + " " + sourcePoints[ 2 ][ 1 ] + " " + ( width / 2 ) + " " + ( ( 3 * height ) / 4 ) + " -hideOutput" );
						break;
					}
					default:
					{
						IJ.error( "Unexpected transformation" );
						return ( null );
					}
					}

					if ( turboReg == null ) { throw ( new ClassNotFoundException() ); }

					method = turboReg.getClass().getMethod( "getTransformedImage", null );
					ImagePlus transformedChannelImp = ( ImagePlus ) method.invoke( turboReg, null );
					transformedChannelImp.getStack().deleteLastSlice();

					switch ( imp.getType() )
					{
					case ImagePlus.GRAY8:
					{
						transformedChannelImp.getProcessor().setMinAndMax( 0.0, 255.0 );
						final ImageConverter converter = new ImageConverter( transformedChannelImp );
						converter.convertToGray8();
						break;
					}
					case ImagePlus.GRAY16:
					{
						transformedChannelImp.getProcessor().setMinAndMax( 0.0, 65535.0 );
						final ImageConverter converter = new ImageConverter( transformedChannelImp );
						converter.convertToGray16();
						break;
					}
					default:
					{
						IJ.error( "Unexpected image type" );
						return ( null );
					}
					}

					finalImp.getImageStack().setProcessor( transformedChannelImp.getProcessor().duplicate(), channel + s );
					finalImp.setProcessor( transformedChannelImp.getProcessor().duplicate() );
				}

				break;
			}
			default:
			{
				IJ.error( "Unexpected image type" );
				return ( null );
			}
			}
		}
		catch ( NoSuchMethodException e )
		{
			IJ.error( "Unexpected NoSuchMethodException " + e );
			return ( null );
		}
		catch ( IllegalAccessException e )
		{
			IJ.error( "Unexpected IllegalAccessException " + e );
			return ( null );
		}
		catch ( InvocationTargetException e )
		{
			IJ.error( "Unexpected InvocationTargetException " + e );
			return ( null );
		}
		catch ( ClassNotFoundException e )
		{
			IJ.error( "Please download TurboReg_ from\nhttp://bigwww.epfl.ch/thevenaz/turboreg/" );
			return ( null );
		}

		return ( source );
	}

	/**
	 * 
	 * @param fromCoord
	 * @param toCoord
	 * @param transformation
	 * @return
	 */
	private double[][] getTransformationMatrix( final double[][] fromCoord, final double[][] toCoord )
	{
		double[][] matrix = new double[ 3 ][ 3 ];

		final double angle = Math.atan2( fromCoord[ 2 ][ 0 ] - fromCoord[ 1 ][ 0 ], fromCoord[ 2 ][ 1 ] - fromCoord[ 1 ][ 1 ] ) - Math.atan2( toCoord[ 2 ][ 0 ] - toCoord[ 1 ][ 0 ], toCoord[ 2 ][ 1 ] - toCoord[ 1 ][ 1 ] );
		final double c = Math.cos( angle );
		final double s = Math.sin( angle );
		matrix[ 0 ][ 0 ] = c;
		matrix[ 0 ][ 1 ] = -s;
		matrix[ 0 ][ 2 ] = toCoord[ 0 ][ 0 ] - c * fromCoord[ 0 ][ 0 ] + s * fromCoord[ 0 ][ 1 ];
		matrix[ 1 ][ 0 ] = s;
		matrix[ 1 ][ 1 ] = c;
		matrix[ 1 ][ 2 ] = toCoord[ 0 ][ 1 ] - s * fromCoord[ 0 ][ 0 ] - c * fromCoord[ 0 ][ 1 ];

		matrix[ 2 ][ 0 ] = 0.0;
		matrix[ 2 ][ 1 ] = 0.0;
		matrix[ 2 ][ 2 ] = 1.0;
		return ( matrix );
	}

	enum Transformation
	{
		TRANSLATION, RIGID_BODY, SCALED_ROTATION, AFFINE;
	}

}
