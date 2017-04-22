package de.fzj.jungle.preprocessing.boxdetection;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.io.InputStream;

/**
 * Detects edges in an image of rectangular microfluidic growth chamber.
 * <p>
 * Uses manually predefined templates (find them in
 * {@code src/main/resources/kernels} that are matched by computing and
 * thresholding a cross-correlation image of the input.
 * </p>
 * 
 * @author Stefan Helfrich
 */
public class EdgeDetector implements PlugInFilter
{

	@Override
	public int setup( String arg, ImagePlus imp )
	{
		// NB: not setup required
		return 0;
	}

	@Override
	public void run( ImageProcessor ip )
	{
		// Delegate to process()
		ImageProcessor boxEdges = process( ip );

		ip = boxEdges;
	}

	public ImageProcessor process( ImageProcessor ip )
	{
		int imgWidth = ip.getWidth();
		int imgHeight = ip.getHeight();

		float[][] origimg = new float[ imgWidth ][ imgHeight ];
		for ( int w = 0; w < imgWidth; w++ )
		{
			for ( int h = 0; h < imgHeight; h++ )
			{
				origimg[ w ][ h ] = ip.getPixel( w, h );
			}
		}

		// Rotate kernel and repeat for each direction
		// Load top kernel from file
		InputStream in = this.getClass().getResourceAsStream( "/kernels/top.tif" );
		Opener o = new Opener();
		ImagePlus kernelImp = o.openTiff( in, "kernel" );
		ImageProcessor kernelIp = kernelImp.getProcessor();

		// TOP
		ByteProcessor verticalIp = crossCorrelate( origimg, kernelIp );
		ByteBlitter verticalBlitter = new ByteBlitter( verticalIp );

		{
			// BOTTOM
			kernelIp.flipVertical();
			ByteProcessor correlatedIp180 = crossCorrelate( origimg, kernelIp );
			verticalBlitter.copyBits( correlatedIp180, 0, 0, Blitter.OR );
		}

		// Load left kernel from file
		in = this.getClass().getResourceAsStream( "/kernels/left.tif" );
		o = new Opener();
		kernelImp = o.openTiff( in, "kernel" );
		kernelIp = kernelImp.getProcessor();

		ByteProcessor horizontalIp = crossCorrelate( origimg, kernelIp );
		ByteBlitter horizontalBlitter = new ByteBlitter( horizontalIp );

		{
			// RIGHT
			kernelIp.flipHorizontal();
			ByteProcessor correlatedIp180 = crossCorrelate( origimg, kernelIp );
			horizontalBlitter.copyBits( correlatedIp180, 0, 0, Blitter.OR );
		}

		ByteProcessor finalIp = new ByteProcessor( imgWidth, imgHeight );
		ByteBlitter finalBlitter = new ByteBlitter( finalIp );
		int xloc = ( int ) Math.floor( ( imgWidth - verticalIp.getWidth() ) / 2 );
		int yloc = ( int ) Math.floor( ( imgHeight - verticalIp.getHeight() ) / 2 );
		finalBlitter.copyBits( verticalIp, xloc, yloc, Blitter.OR );

		xloc = ( int ) Math.floor( ( imgWidth - horizontalIp.getWidth() ) / 2 );
		yloc = ( int ) Math.floor( ( imgHeight - horizontalIp.getHeight() ) / 2 );
		finalBlitter.copyBits( horizontalIp, xloc, yloc, Blitter.OR );

		if ( IJ.debugMode )
		{
			new ImagePlus( "Cross-Correlation Image", finalIp ).show();
		}

		return finalIp;
	}

	private ByteProcessor crossCorrelate( float[][] originalImage, ImageProcessor kernelIp )
	{
		int kernelWidth = kernelIp.getWidth();
		int kernelHeight = kernelIp.getHeight();

		if ( kernelIp.isInvertedLut() )
		{
			kernelIp.invert();
		}

		float[][] kernelimg = new float[ kernelWidth ][ kernelHeight ];
		for ( int w = 0; w < kernelWidth; w++ )
		{
			for ( int h = 0; h < kernelHeight; h++ )
			{
				kernelimg[ w ][ h ] = kernelIp.getPixel( w, h );
			}
		}

		// Compute normalized cross correlation
		float[][] corrimg = ImageTools.statsCorrelation( originalImage, kernelimg );
		float thresh = 0.90f;
		int corrWidth = corrimg.length;
		int corrHeight = corrimg[ 0 ].length;

		byte[] corrPixels = new byte[ corrWidth * corrHeight ];
		for ( int h = 0; h < corrHeight; h++ )
		{
			for ( int w = 0; w < corrWidth; w++ )
			{
				float value = corrimg[ w ][ h ];
				corrPixels[ h * corrWidth + w ] = ( byte ) ( ( value > thresh ) ? 0xff : 0x00 );
			}
		}

		return new ByteProcessor( corrWidth, corrHeight, corrPixels );
	}
}
