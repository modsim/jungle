package de.fzj.jungle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.fzj.jungle.gui.MasterPluginUI;
import de.fzj.jungle.gui.RoiSplittingTool;
import de.fzj.jungle.preprocessing.PreprocessorPlugin;
import de.fzj.jungle.profile.Profile;
import de.fzj.jungle.profile.ProfileService;
import de.fzj.jungle.segmentation.Cell;
import de.fzj.jungle.segmentation.SegmentationPlugin;
import de.fzj.jungle.segmentation.filter.Filter;
import de.fzj.jungle.segmentation.filter.FilterFactory;
import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

/**
 * Central plugin of the JuNGLE image analysis pipeline.
 * 
 * It handles the creation of the user interface for composing the pipeline and
 * uses the input to steer the subplugins and handles the data flow.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = Command.class, headless = true, menu = { @Menu( label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC ), @Menu( label = "JuNGLE" ), @Menu( label = "JuNGLE Main" ) } )
public class MasterPlugin implements Command
{

	@Parameter
	private Dataset dataset;

	@Parameter
	private LogService logService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private ProfileService profileService;

	/**
	 * The image sequence that is processed by the pipeline. Kept for backwards
	 * compatibility.
	 */
	private ImagePlus imgPlus;

	/**
	 * The user interface that is responsible for gathering input from the the
	 * user.
	 */
	private MasterPluginUI masterPluginUI;

	/**
	 * Show results in a table after completing the computation. The table
	 * includes per frame information about the number of cells and the overall
	 * cell area.
	 */
	private boolean showGeneralResultsTable = true;

	/**
	 * Show results in a table after completing the computation. The table
	 * includes for each single cell in a frame the cell area.
	 */
	private boolean showDetailedResultsTable = false;

	/**
	 * The factor that determines how images are scaled before further
	 * processing.
	 */
	public final static double SCALING_FACTOR = 2.0;

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public void run()
	{
		// Keep imgPlus for backwards compatibility
		ImgPlus< ? extends RealType > imgPlus2 = dataset.getImgPlus();
		this.imgPlus = ImageJFunctions.wrap( imgPlus2, imgPlus2.getName() );

		// Keep ip for backwards compatibility
		ImageProcessor ip = imgPlus.getProcessor();

		Stream< String > stream = profileService.getAvailableProfiles().stream().map( profile -> profile.getProfileName() );
		String[] profiles = stream.toArray( String[]::new );
		masterPluginUI = new MasterPluginUI( profiles );

		// Run garbage collection manually
		System.gc();

		Map< String, Object > settings;

		if ( IJ.isMacro() && ( Macro.getOptions() != null ) )
		{
			// This is macro mode
			settings = parseMacroOptions( Macro.getOptions() );
		}
		else
		{
			// Running UI mode
			if ( showDialog() == false ) { return; }

			settings = masterPluginUI.getSettings();

			// Install ROI Splitting Tool into toolbar
			RoiSplittingTool roiSplittingTool = new RoiSplittingTool();
			// TODO Do we need to explicitly call the run method?
			roiSplittingTool.run( "" );
		}

		// TODO Define following strings in enum
		boolean executePreprocessing = ( Boolean ) settings.get( "executePreprocessing" );
		boolean executeSegmentation = ( Boolean ) settings.get( "executeSegmentation" );
		boolean executeLengthCalculator = ( Boolean ) settings.get( "executeLengthCalculation" );
		boolean executeTracking = ( Boolean ) settings.get( "executeTracking" );

		String filterMethod = ( String ) settings.get( "filterMethod" );

		double minimalCellSize = ( Double ) settings.get( "minimalSize" );
		double maximalCellSize = ( Double ) settings.get( "maximalSize" );
		double maximalClusterSize = ( Double ) settings.get( "backgroundSize" );

		double deviationConvexHull = ( Double ) settings.get( "deviationConvexHull" );

		String profileName = ( String ) settings.get( "segmentationMethod" );
		Stream< Profile > profileStream = profileService.getAvailableProfiles().stream();
		Profile selectedProfile = ( Profile ) profileStream.filter( p -> p.getProfileName().equals( profileName ) ).toArray()[ 0 ];

		/*
		 * Resize the input sizes by SCALING_FACTOR^2 because the parameters are
		 * supposed to be regions/areas.
		 */
		minimalCellSize *= Math.pow( SCALING_FACTOR, 2 );
		maximalCellSize *= Math.pow( SCALING_FACTOR, 2 );
		maximalClusterSize *= Math.pow( SCALING_FACTOR, 2 );

		/*
		 * Preprocessing
		 */
		if ( executePreprocessing )
		{
			PreprocessorPlugin preprocessor = new PreprocessorPlugin( imgPlus );
			preprocessor.run( ip );
		}

		/*
		 * Create Filter using factory
		 */
		Filter< Cell > filter = FilterFactory.createFilter( filterMethod, minimalCellSize, maximalCellSize, maximalClusterSize, deviationConvexHull );

		if ( executeSegmentation )
		{
			HashMap< String, Object > segmentationParameters = new HashMap<>();
			// segmentationParameters.put("dataset", dataset);
			segmentationParameters.put( "filter", filter );
			segmentationParameters.put( "profile", selectedProfile );

			Future< CommandModule > future = commandService.run( SegmentationPlugin.class, true, segmentationParameters );
			final Module module = moduleService.waitFor( future );

			Map< Integer, List< Cell > > segmentationResults = ( Map< Integer, List< Cell > > ) module.getOutput( "finalResults" );

			/*
			 * Results and tables
			 */
			if ( showGeneralResultsTable )
			{
				generateAndShowGeneralResultsTable( segmentationResults );
				// IJ.runMacro("run(\"Generate Results Table\")");
			}

			if ( showDetailedResultsTable )
			{
				generateAndShowDetailedResultsTable( segmentationResults );
			}
		}

		/*
		 * Perform garbage collection to clean unnecessary data from watershed.
		 */
		System.gc();

		if ( executeLengthCalculator )
		{
			IJ.run( "Skeletonize (2D/3D)" );
		}

		if ( executeTracking )
		{

		}
	}

	public boolean showDialog()
	{
		masterPluginUI.showDialog();

		if ( masterPluginUI.wasCanceled() ) { return false; }

		return true;
	}

	/**
	 * Generate and show table with general results.
	 * 
	 * @param results
	 *            A map that contains the detected {@link Cell}s for each frame
	 *            of the input image sequence.
	 */
	private void generateAndShowGeneralResultsTable( Map< Integer, List< Cell > > results )
	{
		ResultsTable resultsTable = new ResultsTable();

		// Get the calibration of the image only once
		Calibration calibration = imgPlus.getCalibration();

		// finalResults is a TreeMap and thus is ordered
		for ( Map.Entry< Integer, ? > entry : results.entrySet() )
		{
			int frame = entry.getKey();

			// Create new row in table
			resultsTable.incrementCounter();

			// Add frame number in first column
			resultsTable.addValue( "Frame", frame );

			// Add number of cells in second column
			resultsTable.addValue( "Cell number", results.get( frame ).size() );

			// Compute sum of cell areas
			List< Cell > cellList = results.get( frame );
			double overallSize = 0.0;
			for ( Cell cell : cellList )
			{
				overallSize += cell.getEnclosedArea( calibration );
			}

			// Add sum of cell areas in third column and denote the unit in the
			// header
			resultsTable.addValue( "Population area [" + calibration.getUnit() + "^2]", overallSize );

			// If the image does not contain calibration information the overall
			// size will be an integer value of pixels. Thus we can set the
			// precision such that integers instead of doubles are shown.
			if ( !this.imgPlus.getCalibration().scaled() )
			{
				resultsTable.setPrecision( 0 );
			}
		}

		resultsTable.showRowNumbers( false );
		resultsTable.show( "Results for " + imgPlus.getTitle() );
	}

	/**
	 * Generate and show table with detailed results.
	 * 
	 * @param results
	 *            A map that contains the detected {@link Cell}s for each frame
	 *            of the input image sequence.
	 */
	private void generateAndShowDetailedResultsTable( Map< Integer, List< Cell > > results )
	{
		ResultsTable resultsTable = new ResultsTable();

		// Get the calibration of the image only once
		Calibration calibration = imgPlus.getCalibration();

		// finalResults is a TreeMap and thus is ordered
		for ( Map.Entry< Integer, ? > entry : results.entrySet() )
		{
			int frame = entry.getKey();

			// Create new row in table
			resultsTable.incrementCounter();

			// Set column header
			resultsTable.setValue( "Frame " + frame, 0, 0.0 );

			List< Cell > cellList = results.get( frame );

			// Fill column with information
			int rowCounter = 0;
			for ( Cell cell : cellList )
			{
				if ( rowCounter >= resultsTable.getCounter() )
				{
					// Append new row in table
					resultsTable.incrementCounter();
				}

				// Add value
				resultsTable.setValue( "Frame " + frame, rowCounter, cell.getEnclosedArea( calibration ) );

				rowCounter++;
			}

		}

		resultsTable.showRowNumbers( false );
		resultsTable.show( "Detailed Results" );
	}

	/**
	 * This method is called to parse the macro options when run in batch mode.
	 * 
	 * @param macroOptions
	 *            String containing the options
	 * @return Map containing the settings
	 */
	protected Map< String, Object > parseMacroOptions( String macroOptions )
	{
		// TODO Robustify!
		Map< String, Object > optionsMap = new HashMap<>();

		Scanner scanner = new Scanner( macroOptions );
		while ( scanner.hasNext() )
		{
			String s = scanner.next();

			try
			{
				// Check for boolean parameters
				clParameter param = clParameter.valueOf( s.toUpperCase() );

				switch ( param )
				{
				case PREPROCESSING: // fall-through
				case SEGMENTATION: // fall-through
				case LENGTH_CALCULATION: // fall-through
				case TRACKING: // fall-through
				case THRESHOLDINGDEBUG: // fall-through
				case SEGMENTATIONDEBUG:
					optionsMap.put( param.optionName, true );
					break;
				default:
					break;
				}
			}
			catch ( IllegalArgumentException e )
			{
				// s does not contain a matching boolean
				String[] keyValueArray = s.split( "=" );
				String keyString = keyValueArray[ 0 ].toUpperCase();
				clParameter keyParameter = clParameter.valueOf( keyString );
				String valueString = keyValueArray[ 1 ].toUpperCase();
				try
				{
					FilterTypes valueParameter = FilterTypes.valueOf( valueString );
					optionsMap.put( keyParameter.optionName, valueParameter.optionName );
				}
				catch ( IllegalArgumentException e2 )
				{
					try
					{
						ProfileTypes valueParameter = ProfileTypes.valueOf( valueString );
						optionsMap.put( keyParameter.optionName, valueParameter.optionName );
					}
					catch ( IllegalArgumentException e3 )
					{
						try
						{
							optionsMap.put( keyParameter.optionName, Integer.parseInt( valueString ) );
						}
						catch ( NumberFormatException nfe )
						{
							optionsMap.put( keyParameter.optionName, Double.parseDouble( valueString ) );
						}
					}
				}
			}
			finally
			{
				scanner.close();
			}
		}

		// Handle default values
		for ( clParameter p : clParameter.values() )
		{
			if ( !optionsMap.containsKey( p.optionName ) )
			{
				optionsMap.put( p.optionName, p.defaultValue );
			}
		}

		return optionsMap;
	}

	/**
	 * This enumeration encodes the possible parameters to the macro interface
	 * of the pipeline.
	 * 
	 * @author Stefan Helfrich
	 */
	enum clParameter
	{
		PREPROCESSING( "executePreprocessing", false ), SEGMENTATION( "executeSegmentation", false ), LENGTH_CALCULATION( "executeLengthCalculation", false ), TRACKING( "executeTracking", false ), AUTO_LOCAL_THRESHOLDING( "autoLocalThresholding", true ), THRESHOLDINGDEBUG( "thresholdingDebug", false ), PROFILE( "segmentationMethod", ProfileTypes.DEFAULT.optionName ), SEGMENTATIONDEBUG( "segmentationDebug", false ), FILTER( "filterMethod", FilterTypes.SIZE.optionName ), MINIMALSIZE( "minimalSize", 200d ), MAXIMALSIZE( "maximalSize", 600d ), BACKGROUNDSIZE( "backgroundSize", 5000d ), DEVIATION( "deviationConvexHull", 0.25d );

		public String optionName;

		public Object defaultValue;

		clParameter( String optionName )
		{
			this.optionName = optionName;
		}

		clParameter()
		{
			this.optionName = this.toString();
		}

		clParameter( String optionName, Object defaultValue )
		{
			this.optionName = optionName;
			this.defaultValue = defaultValue;
		}
	}

	enum FilterTypes
	{
		SIZE( "Size Filter" ), CONVEXHULL( "Convex Hull Filter" ), SIZEANDCONVEXHULL( "Size + Convex Hull Filter" );

		public String optionName;

		FilterTypes( String optionName )
		{
			this.optionName = optionName;
		}
	}

	enum ProfileTypes
	{
		DEFAULT( "Default" ), BALABAN( "Balaban" ), DFG( "DFG" ), RAPHAEL( "Raphael" ), SOPHIE( "Sophie" ), BALABANBHI( "BalabanBHI" );

		public String optionName;

		ProfileTypes( String optionName )
		{
			this.optionName = optionName;
		}
	}

}
