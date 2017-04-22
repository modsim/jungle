package de.fzj.jungle.preprocessing;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import net.imagej.ops.OpService;

import org.scijava.command.Command;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.fzj.jungle.preprocessing.boxdetection.BoxClassifier;
import de.fzj.jungle.preprocessing.boxdetection.EdgeDetector;
import de.fzj.jungle.preprocessing.boxdetection.HoughRectangle;
import de.fzj.jungle.preprocessing.boxdetection.RatioBoxClassifier;
import de.fzj.jungle.preprocessing.hough.LinearHT;
import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLine;
import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLinePair;
import de.fzj.jungle.preprocessing.registration.MultiChannelStackReg_;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.plugin.RoiRotator;
import ij.plugin.RoiScaler;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Implements the pre-processing tasks, i.e. registration of multi-channel time
 * series, identification of boxes (and cropping).
 * 
 * For legacy reasons, {@code PreprocessorPlugin} implements
 * {@link PlugInFilter}.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = Command.class, headless = true, menu = { @Menu( label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC ), @Menu( label = "JuNGLE" ), @Menu( label = "Preprocessor Plugin" ) } )
public class PreprocessorPlugin implements PlugInFilter, Command
{

	/**
	 * FIELDS
	 */
	@Parameter
	private ImagePlus imgPlus;

	@Parameter
	private OpService opService;

	public PreprocessorPlugin()
	{
		this( null );
	}

	public PreprocessorPlugin( ImagePlus imgPlus )
	{
		setup( "", imgPlus );
	}

	@Override
	public int setup( String arg, ImagePlus imgPlus )
	{
		this.imgPlus = imgPlus;

		return DOES_ALL;
	}

	@Override
	public void run( ImageProcessor ip )
	{
		/*
		 * Align images
		 */
		// Since we are using IJ.run in a PlugInFilter we have to unlock the
		// image ..
		MultiChannelStackReg_ multiChannelStackReg = new MultiChannelStackReg_();
		ImagePlus alignedImp = multiChannelStackReg.process( imgPlus );
		imgPlus.setStack( alignedImp.getStack(), alignedImp.getNChannels(), alignedImp.getNSlices(), alignedImp.getNFrames() );

		/*
		 * Identify boxes
		 */
		// Check if a ROI is active and use it for rotating / cropping
		boolean imgPlusHasActiveRoi = ( imgPlus.getRoi() != null );
		Roi roi = null;

		if ( !imgPlusHasActiveRoi )
		{
			/*
			 * Apply custom edge detection
			 */
			imgPlus.setProcessor( ip );
			EdgeDetector edgeDetector = new EdgeDetector();
			edgeDetector.setup( "", imgPlus );
			ImageProcessor test = edgeDetector.process( ip );
			ImagePlus edgesImgPlus = new ImagePlus( "", test );

			if ( IJ.debugMode )
			{
				edgesImgPlus.show();
			}

			/*
			 * Apply Hough transform
			 */
			// TODO ImageJ2: use Ops for Hough transform when available
			LinearHT houghTransform = new LinearHT( edgesImgPlus.getProcessor(), 1440, 256 );
			List< HoughLine > houghLines = houghTransform.getMaxLines( 100, 50 );

			if ( IJ.debugMode )
			{
				drawHoughLines( edgesImgPlus.getProcessor(), houghLines );
				edgesImgPlus.show();
			}

			BoxClassifier boxClassifier = new RatioBoxClassifier();
			TreeSet< HoughRectangle > boxRectangles = boxClassifier.classify( houghLines );

			Iterator< HoughRectangle > iter = boxRectangles.descendingIterator();
			HoughRectangle rect = iter.hasNext() ? iter.next() : null;

			// A box could not be detected: ask user for input
			if ( boxRectangles.size() < 1 || rect == null )
			{
				/*
				 * Insert modeless dialog
				 */
				new WaitForUserDialog( "Mark cultivation chamber with a rectangular ROI, then click OK" ).show();

				roi = imgPlus.getRoi();
			}
			else
			{
				if ( IJ.debugMode )
				{
					ImageProcessor ipHoughDebug = edgesImgPlus.getProcessor();

					// Horizontal 1
					drawHoughLine( ipHoughDebug, rect.houghPair1.getH1() );

					// Horizontal 2
					drawHoughLine( ipHoughDebug, rect.houghPair1.getH2() );

					// Vertical 1
					drawHoughLine( ipHoughDebug, rect.houghPair2.getH1() );

					// Vertical 2
					drawHoughLine( ipHoughDebug, rect.houghPair2.getH2() );

					edgesImgPlus.show();
				}

				HoughLinePair horizontalPair = rect.houghPair1;
				if ( horizontalPair.getH1().getRadius() < horizontalPair.getH2().getRadius() )
				{
					// Switch
					horizontalPair = new HoughLinePair( horizontalPair.getH2(), horizontalPair.getH1(), horizontalPair.getSumOfScores() );
				}

				HoughLinePair verticalPair = rect.houghPair2;
				if ( verticalPair.getH1().getRadius() > verticalPair.getH2().getRadius() )
				{
					// Switch
					verticalPair = new HoughLinePair( verticalPair.getH2(), verticalPair.getH1(), verticalPair.getSumOfScores() );
				}
				/*
				 * Rotate whole image
				 */
				double rotationAngleRadians = verticalPair.getH1().getAngle() > ( 3 * Math.PI / 4 ) ? Math.PI - verticalPair.getH1().getAngle() : verticalPair.getH1().getAngle();
				double rotationAngleDegrees = rotationAngleRadians / Math.PI * 180;
				for ( int i = 1; i <= imgPlus.getStack().getSize() && rotationAngleRadians > 0.0; i++ )
				{
					ImageProcessor ip2 = imgPlus.getStack().getProcessor( i );
					ip2.setInterpolationMethod( ImageProcessor.BICUBIC );
					// TODO ImageJ2: use Views/Ops for rotating
					ip2.rotate( -rotationAngleDegrees );
				}

				/*
				 * Create ROI from selected box
				 */
				Point2D.Double p1 = horizontalPair.getH1().intersectWith( verticalPair.getH1() );
				Point2D.Double p2 = horizontalPair.getH1().intersectWith( verticalPair.getH2() );
				Point2D.Double p3 = horizontalPair.getH2().intersectWith( verticalPair.getH2() );
				Point2D.Double p4 = horizontalPair.getH2().intersectWith( verticalPair.getH1() );

				int[] xPoints = { ( int ) p1.x, ( int ) p2.x, ( int ) p3.x, ( int ) p4.x };
				int[] yPoints = { ( int ) p1.y, ( int ) p2.y, ( int ) p3.y, ( int ) p4.y };

				roi = new PolygonRoi( xPoints, yPoints, 4, Roi.POLYGON );

				// Scale
				// TODO Make scale factor a parameter
				// TODO ImageJ2: use Views/Ops for scaling
				roi = RoiScaler.scale( roi, 0.95, 0.95, true );

				// Rotate the ROI (for cropping)
				// TODO ImageJ2: use Views/Ops for rotating
				roi = RoiRotator.rotate( roi, -rotationAngleDegrees );
			}
		}
		else
		{
			roi = imgPlus.getRoi();
		}

		/*
		 * Crop image
		 */
		// TODO ImageJ2: use Views/Ops for cropping
		ImageStack stack = imgPlus.getStack();
		ImageStack stack2 = null;
		for ( int i = 1; i <= stack.getSize(); i++ )
		{
			ImageProcessor ip2 = stack.getProcessor( i );
			ip2.setRoi( roi );
			ip2 = ip2.crop();
			if ( stack2 == null )
				stack2 = new ImageStack( ip2.getWidth(), ip2.getHeight() );
			stack2.addSlice( stack.getSliceLabel( i ), ip2 );
		}

		imgPlus.setStack( stack2 );
	}

	/**
	 * Draw a set of {@link HoughLine}s as red lines into the provided image.
	 * <p>
	 * <b>Changes pixel intensities of {@code ip}!</b>
	 * </p>
	 * 
	 * @param ip
	 *            {@link ImageProcessor} to draw into
	 * @param houghLines
	 *            a set of {@link HoughLine}s
	 */
	@Deprecated
	private void drawHoughLines( ImageProcessor ip, Collection< HoughLine > houghLines )
	{
		for ( HoughLine hl : houghLines )
		{
			Line2D.Double lin = hl.makeLine2D();
			int u1 = ( int ) Math.rint( lin.x1 );
			int v1 = ( int ) Math.rint( lin.y1 );
			int u2 = ( int ) Math.rint( lin.x2 );
			int v2 = ( int ) Math.rint( lin.y2 );
			ip.setColor( Color.RED );
			ip.drawLine( u1, v1, u2, v2 );
		}
	}

	/**
	 * Draws a {@link HoughLine} as red line into the provided image.
	 * <p>
	 * <b>Changes pixel intensities of {@code ip}!</b>
	 * </p>
	 * 
	 * @param ip
	 *            {@link ImageProcessor} to draw into
	 * @param houghLine
	 *            the {@link HoughLine} to draw.
	 */
	@Deprecated
	private void drawHoughLine( ImageProcessor ip, HoughLine houghLine )
	{
		Line2D.Double lin = houghLine.makeLine2D();
		int u1 = ( int ) Math.rint( lin.x1 );
		int v1 = ( int ) Math.rint( lin.y1 );
		int u2 = ( int ) Math.rint( lin.x2 );
		int v2 = ( int ) Math.rint( lin.y2 );
		ip.setColor( Color.RED );
		ip.drawLine( u1, v1, u2, v2 );
	}

	@Override
	public void run()
	{
		this.run( this.imgPlus.getProcessor() );
	}
}
