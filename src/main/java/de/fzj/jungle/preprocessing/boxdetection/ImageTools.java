package de.fzj.jungle.preprocessing.boxdetection;

/**
 * Utility class for common processing routines.
 * <p>
 * Code adapted from the Template Matching plugin of Walter O'Dell. See
 * <a href="https://imagej.nih.gov/ij/plugins/template-matching.html">https://
 * imagej.nih.gov/ij/plugins/template-matching.html</a> for more details.
 * </p>
 */
public class ImageTools
{
	public static float sqr( float x )
	{
		return ( x * x );
	}

	public static double sqrt( double x )
	{
		return ( Math.sqrt( x ) );
	}

	public static float sqrt( float x )
	{
		return ( float ) ( Math.sqrt( x ) );
	}

	// the statistical correlation coefficient is similar to image correlation,
	// but
	// uses a few more comptuations and is better at finding image features
	// returns correlation image as 2D array
	// indeices arrays as [depth][width][height]
	public static float[][] statsCorrelation( float[][] pixels, float[][] kernel )
	{
		int kw = kernel.length;
		int kh = kernel[ 0 ].length;
		float[][][][] kernels3D = new float[ 1 ][ 1 ][ kw ][ kh ];
		kernels3D[ 0 ][ 0 ] = kernel;
		int width = pixels.length;
		int height = pixels[ 0 ].length;
		float[][][] pixels3D = new float[ 1 ][ width ][ height ];
		pixels3D[ 0 ] = pixels;
		float[][][][] corr3D = statsCorrelation( pixels3D, kernels3D );
		return corr3D[ 0 ][ 0 ];
	}

	// PW, 04-01-2002 3D statistical correlation
	// WO 4/24/02 added ability to do multiple 3D kernels at once
	// also, knock off the blank edge image elements (slices, columns and rows),
	// thus
	// resulting corrImage is smaller than original image
	public static float[][][][] statsCorrelation( float[][][] pixels, float[][][][] kernels )
	{
		int nkernels = kernels.length;
		int kd = kernels[ 0 ].length; // kernel dims
		int kw = kernels[ 0 ][ 0 ].length;
		int kh = kernels[ 0 ][ 0 ][ 0 ].length;
		int depth = pixels.length; // image dims
		int width = pixels[ 0 ].length;
		int height = pixels[ 0 ][ 0 ].length;
		int ew = kw / 2; // width of edge (no corr value)
		int eh = kh / 2;
		int ed = kd / 2;
		int cd = depth - 2 * ed; // corr image dims (removing edges)
		int cw = width - 2 * ew;
		int ch = height - 2 * eh;
		float[][][][] corrImg = new float[ nkernels ][ cd ][ cw ][ ch ];
		float Sxx, localAvg, corr;
		float Sxy[] = new float[ nkernels ]; // cross correlation
		float Syy[] = new float[ nkernels ]; // self corr of each kernel
		float kernelAvg[] = new float[ nkernels ];

		for ( int n = 0; n < nkernels; n++ )
		{
			kernelAvg[ n ] = Syy[ n ] = 0f; // self correlation of kernel
			for ( int i = 0; i < kw; i++ )
				for ( int j = 0; j < kh; j++ )
					for ( int k = 0; k < kd; k++ )
						kernelAvg[ n ] += kernels[ n ][ k ][ i ][ j ];
			kernelAvg[ n ] /= kw * kh * kd;
			for ( int i = 0; i < kw; i++ )
				for ( int j = 0; j < kh; j++ )
					for ( int k = 0; k < kd; k++ )
						Syy[ n ] += sqr( kernels[ n ][ k ][ i ][ j ] - kernelAvg[ n ] );
		}
		for ( int x = ew; x < width - ew; x++ )
			for ( int y = eh; y < height - eh; y++ )
				for ( int z = ed; z < depth - ed; z++ )
				{ // loop over all pixels in image
					localAvg = 0f; // average of image pixels covered by kernel
					for ( int dd = -ed; dd <= ed; dd++ )
						for ( int ww = -ew; ww <= ew; ww++ )
							for ( int hh = -eh; hh <= eh; hh++ )
								localAvg += ( pixels[ z + dd ][ x + ww ][ y + hh ] );
					localAvg /= kd * kw * kh;
					Sxx = 0f; // self corr of image
					for ( int dd = -ed; dd <= ed; dd++ )
						for ( int ww = -ew; ww <= ew; ww++ )
							for ( int hh = -eh; hh <= eh; hh++ )
								Sxx += sqr( ( pixels[ z + dd ][ x + ww ][ y + hh ] ) - localAvg );

					for ( int n = 0; n < nkernels; n++ )
					{
						Sxy[ n ] = 0f; // cross corr of image with kernel
						for ( int dd = -ed; dd <= ed; dd++ )
							for ( int ww = -ew; ww <= ew; ww++ )
								for ( int hh = -eh; hh <= eh; hh++ )
									Sxy[ n ] += ( ( pixels[ z + dd ][ x + ww ][ y + hh ] ) - localAvg ) * ( kernels[ n ][ dd + ed ][ ww + ew ][ hh + eh ] - kernelAvg[ n ] );
						if ( Sxy[ n ] == 0f )
							corr = 0f; // if both Sxy and Sxx == 0, then gives
										// NAN
						else
							corr = Sxy[ n ] / sqrt( Sxx * Syy[ n ] );
						// correlation values range from -1 ... +1
						corrImg[ n ][ z - ed ][ x - ew ][ y - eh ] = corr;
					} // end nkernel loop
				} // end x,y loops
		return corrImg;
	} // end 3D byte statsCorrelation of multiple kernels

}
