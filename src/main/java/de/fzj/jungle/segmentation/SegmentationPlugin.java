/**
 * 
 */
package de.fzj.jungle.segmentation;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imagej.plugins.commands.assign.noisereduce.NoiseReductionMedian;
import net.imagej.plugins.commands.assign.noisereduce.RadialNeigh;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.binary.Thresholder;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.ops.operation.randomaccessible.unary.FillHoles;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Dilate;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.morph.Erode;
import net.imglib2.ops.types.ConnectedType;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.fzj.jungle.MasterPlugin;
import de.fzj.jungle.profile.Profile;
import de.fzj.jungle.profile.Profiles;
import de.fzj.jungle.segmentation.filter.Filter;
import de.fzj.jungle.segmentation.filter.FilterFactory;
import de.fzj.jungle.segmentation.splitting.BottleneckDetector;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

/**
 * Implements a segmentation pipeline for bacteria in microfluidic growth
 * chambers that can efficiently be parallelized on image sequences.
 * <p>
 * It starts by combining a local thresholded version with a thresholded version
 * of the shape index map. Subsequently, ImageJ's Analyze Particle is executed
 * to detect individual {@link Roi}s from the binary mask. The resulting
 * {@link Cell}s are filtered and split up if they fulfill certain criteria
 * established by {@link Filter}s.
 * </p>
 * <p>
 * Splitting is delegated to a {@link BottleneckDetector} instance. This
 * instance returns a {@code Cell[]} which is subsequently filtered, if cells
 * fulfill the splitting criteria. This iterative process is continued until no
 * more cells are filtered or 10 rounds of iterations (after which the quality
 * of the splitting results is questionable).
 * </p>
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = Command.class, headless = true, menu = { @Menu( label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC ), @Menu( label = "JuNGLE" ), @Menu( label = "SegmentationPlugin" ) } )
public class SegmentationPlugin extends ContextCommand
{

	private Dataset dataset;

	@Parameter
	private OpService opService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private LogService logService;

	// TODO ImageJ2: convert to ImgLabeling for visualization
	@Parameter( type = ItemIO.OUTPUT )
	private Map< Integer, List< Cell > > finalResults = new TreeMap<>();

	/* Private fields */
	@Parameter( type = ItemIO.BOTH )
	private ImagePlus imp;

	@Parameter
	private Filter< Cell > filter = FilterFactory.createFilter( "Size Filter", 800.0d, 2400.0d, 20000.0d, 0.25d );

	@Parameter
	private Profile profile = Profiles.create( "Default" );

	/*
	 * PARAMETERS
	 */
	/**
	 * Debug flag
	 * 
	 * Making it final will make the compiler skip bytecode compilation for
	 * if(DEBUG) statements
	 */
	public static final boolean DEBUG = false;

	/**
	 * Default constructor.
	 */
	public SegmentationPlugin()
	{
		this( null );
	}

	/**
	 * Constructs a {@link SegmentationPlugin} instance from an
	 * {@link ImagePlus}.
	 * 
	 * @param imp
	 */
	public SegmentationPlugin( ImagePlus imp )
	{
		this( imp, null, null );
	}

	/**
	 * Constructs a {@link SegmentationPlugin} instance from an
	 * {@link ImagePlus}, {@link CellDetectorSettings}, and a {@link Filter}.
	 * 
	 * @param imp
	 * @param filter
	 */
	public SegmentationPlugin( ImagePlus imp, Profile profile, Filter< Cell > filter )
	{
		this.imp = imp;
		this.finalResults = new TreeMap<>();

		this.profile = profile;
		this.filter = filter;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param dataset
	 * @param profile
	 * @param filter
	 */
	public SegmentationPlugin( Dataset dataset, Profile profile, Filter< Cell > filter )
	{
		this( ( ImagePlus ) null, profile, filter );

		this.dataset = dataset;
	}

	@Override
	public void run()
	{
		// Keep imgPlus for backwards compatibility
		ImagePlusImg< ? extends RealType, ? > wrapped = ImagePlusAdapter.wrap( this.imp );
		this.dataset = datasetService.create( wrapped );

		// Benchmarking
		long startTime = System.nanoTime();

		// Execute algorithm for each frame in a stack
		final int stackSize = imp.getStackSize();

		final AtomicInteger ai = new AtomicInteger( 1 );

		final Thread[] threads = SimpleMultiThreading.newThreads();

		logService.info( String.format( "Using %d thread(s) for segmentation.", threads.length ) );

		for ( int ithread = 0; ithread < threads.length; ithread++ )
		{
			// Concurrently run in as many threads as CPUs
			threads[ ithread ] = new Thread()
			{

				{
					setPriority( Thread.NORM_PRIORITY );
				}

				@Override
				public void run()
				{
					// Each thread processes a few items in the total list
					// Each loop iteration within the run method
					// has a unique 'i' number to work with
					// and to use as index in the results array:
					int channelIncrease = 1;

					if ( imp.isHyperStack() )
					{
						channelIncrease = imp.getNChannels();
					}

					for ( int i = ai.getAndAdd( channelIncrease ); i <= stackSize; i = ai.getAndAdd( channelIncrease ) )
					{
						logService.info( String.format( "Started processing of image %d.", i ) );

						// Create a new ImagePlus for each thread that has the
						// same meta information, but no image data
						ImagePlus threadImagePlus = imp.createImagePlus();

						// Get the ImageProcessor from the original ImagePlus
						ImageProcessor originalProcessor = imp.getStack().getProcessor( i );

						// Create a duplicate
						ImageProcessor duplicateOfOriginalProcessor = originalProcessor.duplicate();

						// Use that ImageProcessor for the processing
						threadImagePlus.setProcessor( duplicateOfOriginalProcessor );

						// Per construction, this Img will only contain one
						// slice
						Img< ? extends RealType > original = ImagePlusAdapter.wrap( threadImagePlus );

						// Scale up in the spatial domain
						double[] dims = scaleSpatialDimensions( original, dataset, 2.0d );
						RandomAccessibleInterval< ? extends RealType > imgScaled = opService.transform().scale( original, dims, new NLinearInterpolatorFactory() );

						// Obtain a binary image
						Img< BitType > out = segment( imgScaled );

						ImgPlus< BitType > thresholded = new ImgPlus<>( out );
						ImagePlus thresholdedImp = ImageJFunctions.wrap( thresholded, "Thresholded" );
						ImageProcessor outputIp = thresholdedImp.getProcessor();

						// outputIp will be 16-bit when wrapped into an
						// ImagePlus
						outputIp = outputIp.convertToByte( false );

						// Set the correct slice number for the output stack
						outputIp.setSliceNumber( i );
						threadImagePlus.setProcessor( outputIp );

						/*
						 * Img<BitType> will be converted to ImageJ1 images with
						 * values of 0 and 1. Analyze Particles however requires
						 * a binary 8-bit image with a set threshold. Hence, we
						 * are using ContrastEnhancer to convert the image from
						 * [0,1] to [0,255] and subsequently set the threshold.
						 */
						ContrastEnhancer ce = new ContrastEnhancer();
						ce.setNormalize( true );
						ce.stretchHistogram( threadImagePlus, 0.0 );
						outputIp.setThreshold( 254, 255, ImageProcessor.NO_LUT_UPDATE );

						// ParticleAnalyzer writes overlays to an ImagePlus when
						// the option SHOW_OVERLAY_OUTLINES is active. Hence, we
						// can circumvent using the RoiManager by extracting
						// ROIs from the overlay.
						ParticleAnalyzer pa = new ParticleAnalyzer( ParticleAnalyzer.SHOW_OVERLAY_OUTLINES, 0, null, 0, Double.POSITIVE_INFINITY, 0.0, 1.0 );
						pa.analyze( threadImagePlus, outputIp );

						if ( IJ.debugMode )
						{
							threadImagePlus.show();
						}

						Overlay o = threadImagePlus.getOverlay();

						List< Roi > rois = new LinkedList<>();
						for ( int j = 0; j < o.size(); j++ )
						{
							Roi r = o.get( j );

							if ( IJ.debugMode )
							{
								RoiManager.getInstance().addRoi( r );
							}

							rois.add( r );
						}

						// Start the postprocessing
						List< Cell > result = new LinkedList<>();

						for ( Roi r : rois )
						{
							Cell c = new Cell( r.getPolygon(), r, outputIp, imp.getCalibration() );
							result.add( c );
						}

						// Filter cell lists according to the the filter set in
						// the constructor
						List< Cell > filteredResult = new LinkedList<>();
						List< Cell > cellList = result;

						Filter.Result< Cell > filterResult = filter.filter( cellList, outputIp );

						filteredResult.addAll( filterResult.singleCellList );

						int MAX_ITER = 5;
						int iter = 0;
						while ( !filterResult.multipleCellList.isEmpty() && ( iter < MAX_ITER ) )
						{
							List< Cell > tempList = new LinkedList<>();

							for ( Cell c : filterResult.multipleCellList )
							{
								BottleneckDetector bnd = new BottleneckDetector( c );

								try
								{
									Cell[] cellArray = bnd.execute();

									// Might result in a single cell when an
									// inner
									// contour is available
									for ( Cell splitCell : cellArray )
									{
										// Cancel if a split did not result in a
										// change
										if ( splitCell.getCellRoi().equals( c.getCellRoi() ) )
										{
											continue;
										}
										tempList.add( splitCell );
									}
								}
								catch ( UnexpectedException e )
								{
									// Catch exception and add problematic Roi
									// to Manager
									RoiManager manager = RoiManager.getInstance();
									if ( manager == null )
									{
										manager = new RoiManager( true );
									}

									Roi roi = c.getCellRoi();
									roi.setName( "Debug A==B[" + c.getCellId() + "]" );
									manager.addRoi( roi );
								}
							}

							// break;
							filterResult = filter.filter( tempList, outputIp );

							filteredResult.addAll( filterResult.singleCellList );

							iter++;
						}

						finalResults.put( i, filteredResult );

						logService.info( String.format( "Finished processing image %d.", i ) );
					}
				}
			};
		}

		logService.info( "Starting segmentation." );
		SimpleMultiThreading.startAndJoin( threads );
		logService.info( "Finished segmentation." );

		// Benchmarking
		long endTime = System.nanoTime();
		long duration = endTime - startTime;

		logService.info( String.format( "Finished segmentation in %d ns.", duration ) );

		/*
		 * Gather the final segmentation results from the SegmentationPlugin and
		 * create ROIs for each cell. Since those ROIs have to be down-scaled we
		 * do not add them directly to the RoiManager but to a temporary list.
		 */
		Collection< Roi > tempRois = new ArrayList<>();

		for ( Map.Entry< Integer, List< Cell > > entry : getFinalResults().entrySet() )
		{
			int frame = entry.getKey();
			List< Cell > cellList = entry.getValue();

			for ( Cell cell : cellList )
			{
				// Get resized cellRoi
				Roi cellRoi = resizeRoi( cell.getCellRoi() );
				if ( imp.isHyperStack() )
				{
					cellRoi.setPosition( 1, 1, ( frame / imp.getNChannels() ) + 1 );
					cellRoi.setName( getRoiNameFromCell( frame, cell ) );
				}
				else
				{
					cellRoi.setPosition( frame );
					cellRoi.setName( getRoiNameFromCell( frame, cell ) );
				}
				cell.setCellRoi( cellRoi );

				tempRois.add( cellRoi );
			}
		}

		/*
		 * ... we add all containing ROIs to an overlay
		 */
		for ( Roi r : tempRois )
		{
			// Set the current stack position according to the stack type
			// if (imp.isHyperStack()) {
			// imp.setPosition(r.getCPosition(), r.getZPosition(),
			// r.getTPosition());
			// } else {
			// imp.setPosition(r.getPosition());
			//// imp.setSlice(r.getPosition());
			// }

			// Add ROIs to overlay
			Overlay overlay = imp.getOverlay();
			if ( overlay == null )
			{
				overlay = new Overlay();
				imp.setOverlay( overlay );
			}

			overlay.add( r );
		}
	}

	/**
	 * Creates an array that can be used for scaling, where the scaleFactor is
	 * only set for the spatial axes.
	 * 
	 * @param img
	 *            {@link Img} for which the scale array is supposed (dimensions
	 *            are extracted from this image)
	 * @param dataset
	 *            {@link Dataset} from the spatial axes positions are extracted
	 * @param scaleFactor
	 * @return An array where for the spatial dimensions scaleFactor is set and
	 *         1.0 for the remaining dimensions
	 */
	public static double[] scaleSpatialDimensions( Img< ? extends RealType > img, Dataset dataset, double scaleFactor )
	{
		final int xAxisDimension = dataset.dimensionIndex( Axes.X );
		final int yAxisDimension = dataset.dimensionIndex( Axes.Y );

		double[] dims = new double[ img.numDimensions() ];
		for ( int j = 0; j < dims.length; j++ )
		{
			if ( j == xAxisDimension || j == yAxisDimension )
			{
				dims[ j ] = scaleFactor;
			}
			else
			{
				dims[ j ] = 1d;
			}
		}

		return dims;
	}

	/**
	 * Get final segmentation results.
	 * 
	 * @return a mapping from frame number to a list of cell objects.
	 */
	public Map< Integer, List< Cell > > getFinalResults()
	{
		return finalResults;
	}

	/**
	 * Implements the segmentation procedure as described in Stefan Helfrich's
	 * PhD thesis.
	 * 
	 * @param input
	 *            image that is to be segmented.
	 * @return an {@link Img<BitType>} where cells have value TRUE and
	 *         background value FALSE.
	 */
	public < T extends RealType< T > > Img< BitType > segment( RandomAccessibleInterval< T > input )
	{
		Img< BitType > sim = createThresholdedShapeIndexMap( input, profile.getSimLowerThreshold(), profile.getSimUpperThreshold(), profile.getSimGaussRadius() );
		this.debugOut( sim, "sim" );

		Img< BitType > thresholded = createLocalThresholded( input, profile.getThresholdingRadius(), profile.getThresholdingK1(), profile.getThresholdingR() );
		this.debugOut( thresholded, "thresholded" );

		Img< BitType > resultData = thresholded.copy();
		Img< BitType > inputData = thresholded.copy();

		// Invert data for Erode to work properly
		invert( inputData );

		/* Erode image */
		// TODO ImageJ2: use Ops (when they are available)
		OutOfBoundsMirrorFactory< BitType, RandomAccessibleInterval< BitType > > factory = new OutOfBoundsMirrorFactory<>( OutOfBoundsMirrorFactory.Boundary.DOUBLE );
		@SuppressWarnings( "deprecation" )
		Erode erosion = new Erode( ConnectedType.EIGHT_CONNECTED, factory, 1 );
		for ( int i = 0; i < profile.getNumberOfErosions(); i++ )
		{
			erosion.compute( inputData, resultData );
			inputData = resultData.copy();
			this.debugOut( resultData, "erosion " + i );
		}
		this.debugOut( resultData, "thresholded eroded" );

		resultData = resultData.copy();
		inputData = resultData.copy();

		/* Dilate image */
		// TODO ImageJ2: use Ops (when they are available)
		Dilate dilation = new Dilate( ConnectedType.EIGHT_CONNECTED, factory, 1 );
		for ( int i = 0; i < profile.getNumberOfDilations(); i++ )
		{
			dilation.compute( inputData, resultData );
			inputData = resultData.copy();
			this.debugOut( resultData, "dilation " + i );
		}
		this.debugOut( resultData, "thresholded dilated" );

		/* Apply median filter */
		Img< BitType > simMediaFiltered = null;
		if ( profile.getMedianRadius() > 0 )
		{
			// TODO ImageJ2: use Ops (when they are available)
			NoiseReductionMedian< T > medianCommand = new NoiseReductionMedian<>();
			medianCommand.setContext( this.getContext() );
			medianCommand.setInput( datasetService.create( sim ) );
			medianCommand.setNeighborhood( new RadialNeigh( 2, 5 ) );
			medianCommand.run();
			simMediaFiltered = ( Img< BitType > ) medianCommand.getOutput().getImgPlus();

			this.debugOut( simMediaFiltered, "sim median filtered" );
		}

		// Combine both branches
		Img< BitType > output = sim.factory().create( sim, sim.firstElement() );
		combineImages( simMediaFiltered, resultData, output );
		this.debugOut( output, "output" );

		// Fill holes in output
		Img< BitType > filledHoles = sim.factory().create( sim, sim.firstElement() );
		FillHoles fillHolesOperation = new FillHoles( ConnectedType.EIGHT_CONNECTED );
		fillHolesOperation.compute( output, filledHoles );
		this.debugOut( filledHoles, "filled holes" );

		return output;
	}

	/**
	 * Inverts two input images individually and ORs them pixel-wise. Finally,
	 * the output is inverted for further processing.
	 * 
	 * @param simMedianFiltered
	 *            A median-filtered shape index map ({@code Img<BitType>})
	 * @param resultData
	 *            A local thresholded version of the input image
	 *            ({@code Img<BitType>})
	 * @param output
	 *            a binary mask ({@code Img<BitType>})
	 */
	@SuppressWarnings( "unchecked" )
	private void combineImages( Img< BitType > simMedianFiltered, Img< BitType > resultData, Img< BitType > output )
	{
		Map< String, Object > map = new HashMap<>();
		map.put( "sim", simMedianFiltered );
		map.put( "thresholded", resultData );

		Img< BitType > result = ( Img< BitType > ) opService.eval( "!(!sim || !thresholded)", map );
		opService.copy().img( output, result );
	}

	/**
	 * Inplace inversion of an image of BitType. TODO ImageJ2: use Ops
	 * 
	 * @param inputData
	 */
	private static void invert( Img< BitType > inputData )
	{
		Cursor< BitType > cursor = inputData.cursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			BitType value = cursor.get();
			value.not();
		}
	}

	/**
	 * Shows interim results (images) when ImageJ's debug mode is enabled.
	 * 
	 * @param img
	 * @param name
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void debugOut( Img< ? extends RealType > img, String name )
	{
		if ( IJ.debugMode )
		{
			ImageJFunctions.show( img, name );
		}
	}

	/**
	 * Computes the shape index map (SIM) for an input images an applies the
	 * (manual) thresholds to create a binary mask that can be used for further
	 * processing.
	 * 
	 * @param input
	 *            Input image
	 * @param lt
	 *            lower threshold to be set on shape index map
	 * @param ut
	 *            upper threshold to be set on shape index map
	 * @param gaussradius
	 *            radius of the Gaussian used for smoothing the input (before
	 *            computing the shape index map)
	 * @return a binary mask (i.e. {@code Img<BitType>}
	 */
	public < T extends RealType< T > > Img< BitType > createThresholdedShapeIndexMap( RandomAccessibleInterval< T > input, final double lt, final double ut, final double gaussradius )
	{
		/* Compute shape index map */
		Shape_Index_Map_imglib2 simCommand = new Shape_Index_Map_imglib2();
		simCommand.setContext( this.getContext() );
		simCommand.setDataset( datasetService.create( input ) );
		simCommand.setBlurRadius( gaussradius );
		simCommand.run();

		Img< FloatType > sim = simCommand.getShapeIndexMap();

		/*
		 * Do a manual thresholding to create a new ByteProcessor (instead of
		 * just applying a LUT)
		 */
		int threads = 1;
		Img< BitType > simLowerThreshold = Thresholder.threshold( sim, new FloatType( ( float ) lt ), true, threads );
		Img< BitType > simUpperThreshold = Thresholder.threshold( sim, new FloatType( ( float ) ut ), false, threads );

		// Combine both images with AND
		// TODO ImageJ2: use Ops
		Img< BitType > thresholdedSim = simLowerThreshold.factory().create( simLowerThreshold, new BitType() );

		Cursor< BitType > simThresholdedCursor = thresholdedSim.cursor();
		Cursor< BitType > simLowerCursor = simLowerThreshold.cursor();
		Cursor< BitType > simUpperCursor = simUpperThreshold.cursor();

		while ( simThresholdedCursor.hasNext() )
		{
			// Move all cursors forward by one pixel
			simThresholdedCursor.fwd();
			simLowerCursor.fwd();
			simUpperCursor.fwd();

			BitType lowerBoolean = simLowerCursor.get();
			BitType upperBoolean = simUpperCursor.get();
			lowerBoolean.and( upperBoolean );

			simThresholdedCursor.get().set( lowerBoolean );
		}

		return thresholdedSim;
	}

	/**
	 * Applies a local thresholding technique to an image with a circular
	 * windows of a given radius and parameters.
	 * 
	 * @param input
	 *            {@link RandomAccessibleInterval} to be thresholded
	 * @param radius
	 *            radius of the windows that is used for the computation of a
	 *            local threshold
	 * @param k
	 *            First parameter for local threshold
	 * @param r
	 *            Second parameter for local threshold
	 * @return a local thresholded version of {@code input} as
	 *         {@code Img<BitType>}.
	 */
	private < T extends RealType< T > > Img< BitType > createLocalThresholded( RandomAccessibleInterval< T > input, int radius, double k, double r )
	{
		Img< BitType > out = new ArrayImgFactory< BitType >().create( input, new BitType() );
		Img< DoubleType > convertedInput = opService.convert().float64( Views.iterable( input ) );

		/*
		 * Since we are applying local thresholding algorithms that operate on
		 * normalized images, they require the minimum and maximum values as
		 * input.
		 */
		DoubleType max = ( DoubleType ) opService.stats().max( convertedInput );
		DoubleType min = ( DoubleType ) opService.stats().min( convertedInput );

		Img< DoubleType > normalizedInput = new ArrayImgFactory< DoubleType >().create( input, new DoubleType() );
		opService.image().normalize( normalizedInput, convertedInput, min, max, new DoubleType( 0.0 ), new DoubleType( 1.0 ) );

		// Apply threshold via IJ-Ops
		// FIXME Add thresholding method as method parameter
		opService.run( profile.getThresholdingMethodClass(), out, normalizedInput, new RectangleShape( radius, false ), new OutOfBoundsMirrorFactory< T, Img< T > >( Boundary.SINGLE ), k, r );

		return out;
	}

	/**
	 * Resizes a ROI according to {@link MasterPlugin.SCALING_FACTOR}. Does not
	 * work in-place but rather returns a new instance of Roi.
	 * 
	 * @param r
	 * @return new instance of downscaled Roi.
	 */
	private Roi resizeRoi( Roi r )
	{
		FloatPolygon fp = r.getFloatPolygon();
		for ( int j = 0; j < fp.npoints; j++ )
		{
			fp.xpoints[ j ] /= MasterPlugin.SCALING_FACTOR;
			fp.ypoints[ j ] /= MasterPlugin.SCALING_FACTOR;
		}

		Roi newRoi = new PolygonRoi( fp, Roi.POLYGON );
		newRoi.setName( r.getName() );
		newRoi.setImage( r.getImage() );
		newRoi.setPosition( r.getPosition() );

		return newRoi;
	}

	/**
	 * TODO Documentation
	 * 
	 * @param frame
	 * @param c
	 * @return
	 */
	private String getRoiNameFromCell( int frame, Cell c )
	{
		int digits = 4;

		if ( imp.isHyperStack() )
		{
			digits = String.valueOf( imp.getNFrames() ).length();
		}
		else
		{
			digits = String.valueOf( imp.getStackSize() ).length();
		}

		String formatString = "F%0" + digits + "d-";

		return String.format( formatString, frame ) + c.getName();
	}

}
