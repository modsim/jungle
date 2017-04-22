package de.fzj.jungle.segmentation;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.LoggerFactory;

/**
 * WIP: Calculate the shape index as defined in J Koenderink and A van Doorn,
 * "Surface shape and curvature scales", Image Vision Comput, vol. 10, no. 8,
 * pp. 557–565, 1992
 * 
 * @author Stefan Helfrich
 */
@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Shape Index Map (new)")
public class Shape_Index_Map_imglib2 extends ContextCommand {
	
	@Parameter
	private Img<? extends RealType> dataset;
	
	@Parameter
	private double blurRadius;

	@Parameter(type = ItemIO.OUTPUT)
	private Img<FloatType> shapeIndexMap;

	/**
	 * The formula is:
	 *
	 *                                 dnx_x + dny_y
	 * s = 2 / PI * arctan ---------------------------------------
	 *                     sqrt((dnx_x - dny_y)^2 + 4 dny_x dnx_y)
	 *
	 * where _x and _y are the x and y components of the
	 * partial derivatives of the normal vector of the surface
	 * defined by the intensities of the image.
	 *
	 * n_x and n_y are the negative partial derivatives of the
	 * intensity, approximated by simple differences.
	 */
	public <T extends RealType<T>> Img<FloatType> getShapeIndex(RandomAccessibleInterval<T> image) {
		// Convert input image to FloatType
		Converter<T, FloatType> converter = new RealFloatConverter<T>();
		RandomAccessibleInterval<FloatType> floatView = Converters.convert(image, converter, new FloatType());
		
		// Factory for creating FloatType images (SIM and derivatives)
		final ArrayImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
		
		final Img<FloatType> blurred = factory.create(floatView, new FloatType());
		blur(floatView, blurred, blurRadius);
//		ImageJFunctions.show(blurred, "Blurred");	
		
		final Img<FloatType> dx = factory.create(floatView, new FloatType());
		gradient(Views.extendBorder(blurred), dx, 0);
//		ImageJFunctions.show(dx, "dx");
		
		final Img<FloatType> dy = factory.create(floatView, new FloatType());
		gradient(Views.extendBorder(blurred), dy, 1);
//		ImageJFunctions.show(dy, "dy");
		
		final Img<FloatType> dxx = factory.create(dx, new FloatType());
		gradient(Views.extendBorder(dx), dxx, 0);
//		ImageJFunctions.show(dxx, "dxx");
		
		final Img<FloatType> dxy = factory.create(dx, new FloatType());
		gradient(Views.extendBorder(dx), dxy, 1);
//		ImageJFunctions.show(dxy, "dxy");
		
		final Img<FloatType> dyx = factory.create(dy, new FloatType());
		gradient(Views.extendBorder(dy), dyx, 0);
//		ImageJFunctions.show(dyx, "dyx");
		
		final Img<FloatType> dyy = factory.create(dy, new FloatType());
		gradient(Views.extendBorder(dy), dyy, 1);
//		ImageJFunctions.show(dyy, "dyy");
		
		float factor = 2 / (float)Math.PI;
		
		// Create a new image of the same type with same dimensions as the input
		Img<FloatType> shapeIndexMap = factory.create(floatView, new FloatType());
		Cursor<FloatType> shapeIndexCursor = shapeIndexMap.cursor();
		
		// Create cursors for all images
        Cursor<FloatType> cursorDxx = dxx.cursor();
        Cursor<FloatType> cursorDxy = dxy.cursor();
        Cursor<FloatType> cursorDyx = dyx.cursor();
        Cursor<FloatType> cursorDyy = dyy.cursor();
        
        // Iterate
        while (shapeIndexCursor.hasNext()) {
            // Move all cursors forward by one pixel
        	shapeIndexCursor.fwd();
        	cursorDxx.fwd();
            cursorDxy.fwd();
            cursorDyx.fwd();
            cursorDyy.fwd();
            
            float dnx_x = -cursorDxx.get().getRealFloat();
            float dnx_y = -cursorDxy.get().getRealFloat();
            float dny_x = -cursorDyx.get().getRealFloat();
            float dny_y = -cursorDyy.get().getRealFloat();
            
			double D = Math.sqrt((dnx_x - dny_y) * (dnx_x - dny_y) + 4 * dnx_y * dny_x);
			float s = factor * (float)Math.atan((dnx_x + dny_y) / D);
            
			FloatType sFloatType = new FloatType(s);		
			FloatType zeroFloatType = new FloatType(0f);
			
            // set the value of this pixel of the output image to the same as the input,
            // every Type supports T.set( T type )
            shapeIndexCursor.get().set(Float.isNaN(s) ? zeroFloatType : sFloatType);
        }
        
        return shapeIndexMap;
	}
	
	@Override
	@SuppressWarnings({"rawtypes", "unchecked" })
	public void run() {
		shapeIndexMap = getShapeIndex(dataset);
	}
	
	public static void main(String[] args) {
		// Launch ImageJ as usual.
		final ImageJ ij = net.imagej.Main.launch(args);
		
		try {			
//			UnsignedByteType type = new UnsignedByteType();
//			ArrayImgFactory<UnsignedByteType> factory = new ArrayImgFactory<UnsignedByteType>();
//			RandomAccessibleInterval<UnsignedByteType> img = new ImgOpener().openImg("/home/helfrich/Images/benchmark/4cells.tif", factory, type);
//			
//			ImageJFunctions.show(img, "Input");
			Dataset dataset1 = ij.dataset().open("/home/helfrich/Images/benchmark/4cells_multiT.tif");
			
			ij.display().createDisplay(dataset1.getName(), dataset1);
			
			// Launch the "Gradient Image" command right away.
			ij.command().run(Shape_Index_Map_imglib2.class, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Blurs a {@link RandomAccessibleInterval} with a Gaussian kernel.
	 * 
	 * @param source
	 *            source image, has to provide valid data in the interval of the
	 *            target image.
	 * @param target
	 *            output image, blurred version of source in the.
	 * @param gaussRadius
	 *            the (calibrated) radius of the Gaussian kernel.
	 */
	private static <T extends NumericType<T>> void blur(
			RandomAccessibleInterval<T> source,
			RandomAccessibleInterval<T> target,
			double gaussRadius)
	{
		RandomAccessible<T> infiniteImg = Views.extendValue(source, source.randomAccess().get());
//		infiniteImg = Views.hyperSlice(infiniteImg, Axes.TIME, 0);
		
		// Perform Gaussian convolution with float precision
		double[] sigma = new double[infiniteImg.numDimensions()];
		for (int d = 0; d < infiniteImg.numDimensions(); ++d) {
			// TODO Should sigma be != 0 only in spatial domain (x,y)?
			if (d < 2) { // assuming that dim0 and dim1 are x and y
				sigma[d] = gaussRadius;
			} else {
				sigma[d] = 0d;
			}
		}
	
		try {
			Gauss3.gauss(sigma, infiniteImg, target);
		} catch (IncompatibleTypeException e) {
			LoggerFactory.getLogger(Shape_Index_Map_imglib2.class).error("Could not convert images (internal error)", e);
		}
	}
	
	/**
	 * Compute the partial derivative of source in a particular dimension.
	 *
	 * @param source
	 *            source image, has to provide valid data in the interval of the
	 *            gradient image plus a one pixel border in dimension.
	 * @param target
	 *            output image, the partial derivative of source in the
	 *            specified dimension.
	 * @param dimension
	 *            along which dimension the partial derivatives are computed
	 */
	public static < T extends NumericType< T > > void gradient(
			final RandomAccessible< T > source,
			final RandomAccessibleInterval< T > target,
			final int dimension )
	{
		final Cursor< T > front = Views.flatIterable(
				Views.interval( source,
						Intervals.translate( target, 1, dimension ) ) ).cursor();
		final Cursor< T > back = Views.flatIterable(
				Views.interval( source,
						Intervals.translate( target, -1, dimension ) ) ).cursor();
		for( final T t : Views.flatIterable( target ) )
		{
			t.set( front.next() );
			t.sub( back.next() );
			t.mul( 0.5 );
		}
	}
	
	/**
	 * @return the dataset
	 */
	public Img<? extends RealType> getDataset() {
		return dataset;
	}

	/**
	 * @param dataset the dataset to set
	 */
	public void setDataset(Img<? extends RealType> dataset) {
		this.dataset = dataset;
	}

	/**
	 * @return the blurRadius
	 */
	public double getBlurRadius() {
		return blurRadius;
	}

	/**
	 * @param blurRadius the blurRadius to set
	 */
	public void setBlurRadius(double blurRadius) {
		this.blurRadius = blurRadius;
	}

	/**
	 * @return the shapeIndexMap
	 */
	public Img<FloatType> getShapeIndexMap() {
		return shapeIndexMap;
	}
}
