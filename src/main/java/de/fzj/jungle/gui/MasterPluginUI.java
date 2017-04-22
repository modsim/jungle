package de.fzj.jungle.gui;

import ij.IJ;
import ij.WindowManager;
import ij.measure.Calibration;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * User interface component for the image analysis pipeline. Let's you choose
 * pipeline composition as well as parameters. Settings are stored when
 * "Remember settings" is activated.
 * 
 * @author Stefan Helfrich
 */
public class MasterPluginUI extends JDialog implements WindowListener
{

	private static final long serialVersionUID = 1L;

	/* Constants */
	private static final String EXECUTE_PREPROCESSING = "EXECUTE_PREPROCESSING";

	private static final String EXECUTE_SEGMENTATION = "EXECUTE_SEGMENTATION";

	private static final String EXECUTE_LENGTH_CALCULATION = "EXECUTE_LENGTH_CALCULATION";

	private static final String EXECUTE_TRACKING = "EXECUTE_TRACKING";

	private static final String THRESHOLDING_METHOD = "THRESHOLDING_METHOD";

	private static final String THRESHOLDING_DEBUG = "THRESHOLDING_DEBUG";

	private static final String SEGMENTATION_METHOD = "SEGMENTATION_METHOD";

	private static final String SEGMENTATION_DEBUG = "SEGMENTATION_DEBUG";

	private static final String FILTER_METHOD = "FILTER_METHOD";

	private static final String MINIMAL_SIZE = "MINIMAL_SIZE";

	private static final String MAXIMAL_SIZE = "MAXIMAL_SIZE";

	private static final String BACKGROUND_SIZE = "BACKGROUND_SIZE";

	private static final String DEVIATION = "DEVIATION";

	private static final String REMEMBER_SETTINGS = "REMEMBER_SETTINGS";

	private boolean wasCanceled;

	private Calibration calibration;

	/** Storage for the previously selected settings. */
	private Preferences prefs;

	/*
	 * UI elements
	 */
	private JPanel jPanel;

	private JPanel pipelineCompositionPanel;

	private JCheckBox preprocessingCheckBox;

	private JCheckBox segmentationCheckBox;

	private JCheckBox lengthCalculationCheckBox;

	private JCheckBox trackingCheckBox;

	private JPanel thresholdingPanel;

	private JComboBox< String > thresholdingComboBox;

	private JCheckBox thresholdingDebugCheckBox;

	private JPanel segmentationPanel;

	private JComboBox< String > segmentationComboBox;

	private JCheckBox segmentationDebugCheckBox;

	private JPanel filterPanel;

	private JComboBox< String > filterComboBox;

	/*
	 * Settings panel for SizeFilter
	 */
	private JPanel sizeFilterSettingsPanel;

	private JLabel minimalSizeLabel;

	private JTextField minimalSizeTextField;

	private JLabel minimalSizePixelLabel;

	private JLabel maximalSizeLabel;

	private JTextField maximalSizeTextField;

	private JLabel maximalSizePixelLabel;

	private JLabel backgroundSizeLabel;

	private JTextField backgroundSizeTextField;

	private JLabel backgroundSizePixelLabel;

	/*
	 * Simplified settings panel for Size and Convex Hull Filter
	 */
	private JPanel miniSizeFilterSettingsPanel;

	private JLabel miniMinimalSizeLabel;

	private JTextField miniMinimalSizeTextField;

	private JLabel miniMinimalSizePixelLabel;

	private JLabel miniBackgroundSizeLabel;

	private JTextField miniBackgroundSizeTextField;

	private JLabel miniBackgroundSizePixelLabel;

	private JPanel convexHullFilterSettingsPanel;

	private JLabel deviationLabel;

	private JTextField deviationTextField;

	private JPanel rememberSettingsPanel;

	private JCheckBox rememberSettingsCheckBox;

	private JPanel generalButtonsPanel;

	private JButton helpButton;

	private JButton cancelButton;

	private JButton startButton;

	public MasterPluginUI( final String[] profileNames )
	{
		super( WindowManager.getCurrentImage() != null ? ( Frame ) WindowManager.getCurrentImage().getWindow() : IJ.getInstance() != null ? IJ.getInstance() : new Frame(), "Master Plugin", true );

		this.calibration = WindowManager.getCurrentImage().getCalibration();
		this.prefs = Preferences.userNodeForPackage( this.getClass() );

		init( profileNames );
		disableInactive();
	}

	private void disableInactive()
	{
		lengthCalculationCheckBox.setEnabled( false );
		trackingCheckBox.setEnabled( false );
		thresholdingComboBox.setEnabled( false );
		thresholdingDebugCheckBox.setEnabled( false );
		segmentationDebugCheckBox.setEnabled( false );
		helpButton.setEnabled( false );
	}

	public void init( final String[] profileNames )
	{
		this.setTitle( "Master Plugin" );
		this.setResizable( false );
		this.addWindowListener( this );

		jPanel = new JPanel();
		add( jPanel, BorderLayout.NORTH );
		jPanel.setLayout( new BoxLayout( jPanel, BoxLayout.PAGE_AXIS ) );
		// jPanel.setSize(300, 50);
		// jPanel.setPreferredSize(new java.awt.Dimension(300, 50));

		/*
		 * Modules / pipeline composition
		 */
		pipelineCompositionPanel = new JPanel();
		pipelineCompositionPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Pipeline Composition" ), BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
		jPanel.add( pipelineCompositionPanel );
		GridLayout pipelineCompositionLayout = new GridLayout( 4, 1 );
		pipelineCompositionPanel.setLayout( pipelineCompositionLayout );

		preprocessingCheckBox = new JCheckBox( "Preprocessing", prefs.getBoolean( EXECUTE_PREPROCESSING, true ) );
		pipelineCompositionPanel.add( preprocessingCheckBox );
		segmentationCheckBox = new JCheckBox( "Segmentation", prefs.getBoolean( EXECUTE_SEGMENTATION, true ) );
		pipelineCompositionPanel.add( segmentationCheckBox );
		lengthCalculationCheckBox = new JCheckBox( "Length Calculation", prefs.getBoolean( EXECUTE_LENGTH_CALCULATION, false ) );
		pipelineCompositionPanel.add( lengthCalculationCheckBox );
		trackingCheckBox = new JCheckBox( "Tracking", prefs.getBoolean( EXECUTE_TRACKING, false ) );
		pipelineCompositionPanel.add( trackingCheckBox );

		/*
		 * Thresholding
		 */
		thresholdingPanel = new JPanel();
		thresholdingPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Thresholding" ), BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
		jPanel.add( thresholdingPanel );
		GridLayout thresholdingLayout = new GridLayout( 2, 1 );
		thresholdingPanel.setLayout( thresholdingLayout );

		thresholdingComboBox = new JComboBox<>();
		thresholdingComboBox.setModel( new DefaultComboBoxModel<>( new String[] { "Auto Local Threshold", "Auto Threshold", "Manual" } ) );
		thresholdingComboBox.setSelectedItem( prefs.get( THRESHOLDING_METHOD, "Auto Local Threshold" ) );
		thresholdingComboBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				thresholdingComboBoxActionPerformed( e );
			}
		} );
		thresholdingPanel.add( thresholdingComboBox );

		thresholdingDebugCheckBox = new JCheckBox( "Debug", prefs.getBoolean( THRESHOLDING_DEBUG, false ) );
		thresholdingPanel.add( thresholdingDebugCheckBox );

		/*
		 * Segmentation
		 */
		segmentationPanel = new JPanel();
		segmentationPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Segmentation" ), BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
		jPanel.add( segmentationPanel );
		GridLayout segmentationLayout = new GridLayout( 2, 1 );
		segmentationPanel.setLayout( segmentationLayout );

		segmentationComboBox = new JComboBox<>();
		segmentationComboBox.setModel( new DefaultComboBoxModel<>( profileNames ) );
		segmentationComboBox.setSelectedItem( prefs.get( SEGMENTATION_METHOD, "Default" ) );
		segmentationComboBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				segmentationComboBoxActionPerformed( e );
			}
		} );
		segmentationPanel.add( segmentationComboBox );

		segmentationDebugCheckBox = new JCheckBox( "Debug", prefs.getBoolean( SEGMENTATION_DEBUG, false ) );
		segmentationPanel.add( segmentationDebugCheckBox );

		/*
		 * Filter selection
		 */
		filterPanel = new JPanel();
		filterPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Filter" ), BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
		jPanel.add( filterPanel );
		GridLayout filterLayout = new GridLayout( 1, 1 );
		filterPanel.setLayout( filterLayout );

		filterComboBox = new JComboBox<>();
		filterComboBox.setModel( new DefaultComboBoxModel<>( new String[] { "Size Filter", "Convex Hull Filter", "Size + Convex Hull Filter", "No Filter" } ) );
		filterComboBox.setSelectedItem( prefs.get( FILTER_METHOD, "Size + Convex Hull Filter" ) );
		filterComboBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				filterComboBoxActionPerformed( e );
			}
		} );
		filterPanel.add( filterComboBox );

		/*
		 * Size filter settings
		 */
		// TODO Add slider with three handles
		sizeFilterSettingsPanel = new JPanel();
		sizeFilterSettingsPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Size Filter Settings" ), BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
		jPanel.add( sizeFilterSettingsPanel );
		GridLayout sizeFilterLayout = new GridLayout( 3, 3 );
		sizeFilterSettingsPanel.setLayout( sizeFilterLayout );

		{
			minimalSizeLabel = new JLabel( "Minimal cell size:" );
			sizeFilterSettingsPanel.add( minimalSizeLabel );

			// Determine pixel / microns based on the image calibration
			if ( calibration.getUnit().equals( "pixel" ) )
			{
				minimalSizeTextField = new JTextField( prefs.get( MINIMAL_SIZE, "200" ) );
			}
			else if ( calibration.getUnit().equals( "micron" ) )
			{
				minimalSizeTextField = new JTextField( prefs.get( MINIMAL_SIZE, "1.7" ) );
			}
			sizeFilterSettingsPanel.add( minimalSizeTextField );

			minimalSizePixelLabel = new JLabel( calibration.getUnit() + "^2" );
			sizeFilterSettingsPanel.add( minimalSizePixelLabel );
		}

		{
			maximalSizeLabel = new JLabel( "Maximal cell size:" );
			sizeFilterSettingsPanel.add( maximalSizeLabel );

			// Determine pixel / microns based on the image calibration
			if ( calibration.getUnit().equals( "pixel" ) )
			{
				maximalSizeTextField = new JTextField( prefs.get( MAXIMAL_SIZE, "650" ) );
			}
			else if ( calibration.getUnit().equals( "micron" ) )
			{
				maximalSizeTextField = new JTextField( prefs.get( MAXIMAL_SIZE, "5.4" ) );
			}
			sizeFilterSettingsPanel.add( maximalSizeTextField );

			maximalSizePixelLabel = new JLabel( calibration.getUnit() + "^2" );
			sizeFilterSettingsPanel.add( maximalSizePixelLabel );
		}

		{
			backgroundSizeLabel = new JLabel( "Background size:" );
			sizeFilterSettingsPanel.add( backgroundSizeLabel );

			// Determine pixel / microns based on the image calibration
			if ( calibration.getUnit().equals( "pixel" ) )
			{
				backgroundSizeTextField = new JTextField( prefs.get( BACKGROUND_SIZE, "5000" ) );
			}
			else if ( calibration.getUnit().equals( "micron" ) )
			{
				backgroundSizeTextField = new JTextField( prefs.get( BACKGROUND_SIZE, "41.3" ) );
			}
			sizeFilterSettingsPanel.add( backgroundSizeTextField );

			backgroundSizePixelLabel = new JLabel( calibration.getUnit() + "^2" );
			sizeFilterSettingsPanel.add( backgroundSizePixelLabel );
		}

		/*
		 * Mini Size filter settings
		 */
		// TODO Slider with three handles?!
		miniSizeFilterSettingsPanel = new JPanel();
		miniSizeFilterSettingsPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Size Filter Settings" ), BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
		jPanel.add( miniSizeFilterSettingsPanel );
		GridLayout miniSizeFilterLayout = new GridLayout( 2, 3 );
		miniSizeFilterSettingsPanel.setLayout( miniSizeFilterLayout );

		{
			miniMinimalSizeLabel = new JLabel( "Minimal cell size:" );
			miniSizeFilterSettingsPanel.add( miniMinimalSizeLabel );

			// Determine pixel / microns based on the image calibration
			if ( calibration.getUnit().equals( "pixel" ) )
			{
				// TODO Not sure if this is correct
				miniMinimalSizeTextField = new JTextField( prefs.get( MINIMAL_SIZE, "200" ) );
			}
			else if ( calibration.getUnit().equals( "micron" ) )
			{
				miniMinimalSizeTextField = new JTextField( prefs.get( MINIMAL_SIZE, "1.7" ) );
			}
			miniSizeFilterSettingsPanel.add( miniMinimalSizeTextField );

			miniMinimalSizePixelLabel = new JLabel( calibration.getUnit() + "^2" );
			miniSizeFilterSettingsPanel.add( miniMinimalSizePixelLabel );
		}

		{
			miniBackgroundSizeLabel = new JLabel( "Background size:" );
			miniSizeFilterSettingsPanel.add( miniBackgroundSizeLabel );

			// Determine pixel / microns based on the image calibration
			if ( calibration.getUnit().equals( "pixel" ) )
			{
				miniBackgroundSizeTextField = new JTextField( prefs.get( BACKGROUND_SIZE, "5000" ) );
			}
			else if ( calibration.getUnit().equals( "micron" ) )
			{
				miniBackgroundSizeTextField = new JTextField( prefs.get( BACKGROUND_SIZE, "41.3" ) );
			}
			miniSizeFilterSettingsPanel.add( miniBackgroundSizeTextField );

			miniBackgroundSizePixelLabel = new JLabel( calibration.getUnit() + "^2" );
			miniSizeFilterSettingsPanel.add( miniBackgroundSizePixelLabel );
		}

		/*
		 * Convex hull filter settings
		 */
		convexHullFilterSettingsPanel = new JPanel();
		convexHullFilterSettingsPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createTitledBorder( "Convex Hull Filter Settings" ), BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
		jPanel.add( convexHullFilterSettingsPanel );
		GridLayout convexHullFilterLayout = new GridLayout( 1, 2 );
		convexHullFilterSettingsPanel.setLayout( convexHullFilterLayout );

		deviationLabel = new JLabel( "Deviaton:" );
		convexHullFilterSettingsPanel.add( deviationLabel );
		deviationTextField = new JTextField( prefs.get( DEVIATION, "0.25" ) );
		convexHullFilterSettingsPanel.add( deviationTextField );

		// Invisible in the beginning
		String rememberedFilterMethod = prefs.get( FILTER_METHOD, "Size + Convex Hull Filter" );
		if ( rememberedFilterMethod.equals( "Size + Convex Hull Filter" ) )
		{
			sizeFilterSettingsPanel.setVisible( false );
			miniSizeFilterSettingsPanel.setVisible( true );
			convexHullFilterSettingsPanel.setVisible( true );
		}
		if ( rememberedFilterMethod.equals( "Size Filter" ) )
		{
			sizeFilterSettingsPanel.setVisible( true );
			miniSizeFilterSettingsPanel.setVisible( false );
			convexHullFilterSettingsPanel.setVisible( false );
		}
		if ( rememberedFilterMethod.equals( "Convex Hull Filter" ) )
		{
			sizeFilterSettingsPanel.setVisible( false );
			miniSizeFilterSettingsPanel.setVisible( false );
			convexHullFilterSettingsPanel.setVisible( true );
		}

		/*
		 * Store settings
		 */
		rememberSettingsPanel = new JPanel();
		jPanel.add( rememberSettingsPanel );
		GridLayout storeSettingsLayout = new GridLayout( 1, 1 );
		rememberSettingsPanel.setLayout( storeSettingsLayout );

		rememberSettingsCheckBox = new JCheckBox( "Remember settings?" );
		rememberSettingsPanel.add( rememberSettingsCheckBox );
		rememberSettingsCheckBox.setSelected( prefs.getBoolean( REMEMBER_SETTINGS, false ) );

		/*
		 * General controls
		 */
		generalButtonsPanel = new JPanel();
		jPanel.add( generalButtonsPanel );
		GridLayout generalButtonsLayout = new GridLayout( 1, 3 );
		generalButtonsPanel.setLayout( generalButtonsLayout );

		helpButton = new JButton( "Help" );
		generalButtonsPanel.add( helpButton );

		cancelButton = new JButton( "Cancel" );
		generalButtonsPanel.add( cancelButton );
		cancelButton.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( ActionEvent e )
			{
				wasCanceled = true;
				dispose();
			}
		} );

		startButton = new JButton( "Start" );
		generalButtonsPanel.add( startButton );
		startButton.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( ActionEvent e )
			{
				wasCanceled = false;

				if ( rememberSettingsCheckBox.isSelected() )
				{
					rememberSettings();
				}
				else
				{
					try
					{
						prefs.clear();
					}
					catch ( BackingStoreException e1 )
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

				dispose();
			}
		} );
	}

	@SuppressWarnings( "unused" )
	protected void segmentationComboBoxActionPerformed( ActionEvent e )
	{
		// NB: not handled
	}

	@SuppressWarnings( "unchecked" )
	private void filterComboBoxActionPerformed( ActionEvent e )
	{
		JComboBox< String > box = ( JComboBox< String > ) e.getSource();
		String string = ( String ) box.getSelectedItem();

		if ( string.equals( "Size Filter" ) )
		{
			/*
			 * When Size Filter is selected: - hide convex hull filter settings
			 * - show size filter settings - hide mini size filter settings
			 */
			sizeFilterSettingsPanel.setVisible( true );
			convexHullFilterSettingsPanel.setVisible( false );
			miniSizeFilterSettingsPanel.setVisible( false );
		}
		else if ( string.equals( "Convex Hull Filter" ) )
		{
			/*
			 * When Convex Hull Filter is selected: - show convex hull filter
			 * settings - hide size filter settings - hide mini size filter
			 * settings
			 */
			convexHullFilterSettingsPanel.setVisible( true );
			sizeFilterSettingsPanel.setVisible( false );
			miniSizeFilterSettingsPanel.setVisible( false );
		}
		else if ( string.equals( "Size + Convex Hull Filter" ) )
		{
			/*
			 * When Convex Hull Filter is selected: - show convex hull filter
			 * settings - show rmini size filter settings
			 */
			miniSizeFilterSettingsPanel.setVisible( true );
			convexHullFilterSettingsPanel.setVisible( true );
			sizeFilterSettingsPanel.setVisible( false );
		}

		pack();
		repaint();
		validate();
	}

	@SuppressWarnings( "unused" )
	private void thresholdingComboBoxActionPerformed( ActionEvent e )
	{
		// NB: not handled
	}

	public static void main( String[] args )
	{
		MasterPluginUI ui = new MasterPluginUI( new String[] { "Test1", "Test2" } );
		ui.showDialog();
	}

	public void showDialog()
	{
		pack();
		repaint();
		validate();
		setVisible( true );
	}

	public boolean wasCanceled()
	{
		return wasCanceled;
	}

	@Override
	public void windowActivated( WindowEvent e )
	{}

	@Override
	public void windowClosed( WindowEvent e )
	{}

	@Override
	public void windowClosing( WindowEvent e )
	{
		wasCanceled = true;
		dispose();
	}

	@Override
	public void windowDeactivated( WindowEvent e )
	{
		// NB: event handled
	}

	@Override
	public void windowDeiconified( WindowEvent e )
	{
		// NB: event handled
	}

	@Override
	public void windowIconified( WindowEvent e )
	{
		// NB: event handled
	}

	@Override
	public void windowOpened( WindowEvent e )
	{
		// NB: event handled
	}

	/**
	 * Extracts settings entered by the user in the user interface.
	 * 
	 * @return a map that contains settings associated with names
	 */
	public Map< String, Object > getSettings()
	{
		Map< String, Object > settings = new HashMap<>();

		settings.put( "executePreprocessing", preprocessingCheckBox.isSelected() );
		settings.put( "executeSegmentation", segmentationCheckBox.isSelected() );
		settings.put( "executeLengthCalculation", lengthCalculationCheckBox.isSelected() );
		settings.put( "executeTracking", trackingCheckBox.isSelected() );

		settings.put( "thresholdingMethod", thresholdingComboBox.getSelectedItem() );
		settings.put( "thresholdingDebug", thresholdingDebugCheckBox.isSelected() );

		settings.put( "segmentationMethod", segmentationComboBox.getSelectedItem() );
		settings.put( "segmentationDebug", segmentationDebugCheckBox.isSelected() );

		settings.put( "filterMethod", filterComboBox.getSelectedItem() );

		if ( sizeFilterSettingsPanel.isVisible() && !miniSizeFilterSettingsPanel.isVisible() )
		{
			settings.put( "minimalSize", parseDouble( minimalSizeTextField.getText().trim() ) );
			settings.put( "maximalSize", parseDouble( maximalSizeTextField.getText().trim() ) );
			settings.put( "backgroundSize", parseDouble( backgroundSizeTextField.getText().trim() ) );
		}

		if ( !sizeFilterSettingsPanel.isVisible() && miniSizeFilterSettingsPanel.isVisible() )
		{
			settings.put( "minimalSize", parseDouble( miniMinimalSizeTextField.getText().trim() ) );
			settings.put( "maximalSize", -1d );
			settings.put( "backgroundSize", parseDouble( miniBackgroundSizeTextField.getText().trim() ) );
		}

		if ( !sizeFilterSettingsPanel.isVisible() && !miniSizeFilterSettingsPanel.isVisible() )
		{
			settings.put( "minimalSize", -1d );
			settings.put( "maximalSize", -1d );
			settings.put( "backgroundSize", -1d );
		}

		settings.put( "deviationConvexHull", parseDouble( deviationTextField.getText().trim() ) );

		return settings;
	}

	/**
	 * Parses a double from a String using Double.parseDouble when appropriate.
	 * Does additionally check for commas in the String, tries to replace it
	 * with a dot, and calls itself with the result.
	 * 
	 * @param s
	 *            String containing a floating point number.
	 * @return the parsed number.
	 * @throws NumberFormatException
	 */
	private double parseDouble( String s ) throws NumberFormatException
	{
		double d = 0d;

		try
		{
			d = Double.parseDouble( s );
		}
		catch ( NumberFormatException e )
		{
			int idx = s.indexOf( "," );

			if ( idx != -1 )
			{
				// s contains a comma with the index idx
				String sWithDot = s.substring( 0, idx ) + "." + s.substring( idx + 1 );
				d = parseDouble( sWithDot );
			}
			else
			{
				throw e;
			}
		}

		return d;
	}

	private void rememberSettings()
	{
		try
		{
			prefs.clear();
		}
		catch ( BackingStoreException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Try to remove code duplication
		prefs.putBoolean( EXECUTE_PREPROCESSING, preprocessingCheckBox.isSelected() );
		prefs.putBoolean( EXECUTE_SEGMENTATION, segmentationCheckBox.isSelected() );
		prefs.putBoolean( EXECUTE_LENGTH_CALCULATION, lengthCalculationCheckBox.isSelected() );
		prefs.putBoolean( EXECUTE_TRACKING, trackingCheckBox.isSelected() );

		prefs.put( THRESHOLDING_METHOD, ( String ) thresholdingComboBox.getSelectedItem() );
		prefs.putBoolean( THRESHOLDING_DEBUG, thresholdingDebugCheckBox.isSelected() );
		prefs.put( SEGMENTATION_METHOD, ( String ) segmentationComboBox.getSelectedItem() );
		prefs.putBoolean( SEGMENTATION_DEBUG, segmentationDebugCheckBox.isSelected() );
		prefs.put( FILTER_METHOD, ( String ) filterComboBox.getSelectedItem() );

		if ( sizeFilterSettingsPanel.isVisible() && !miniSizeFilterSettingsPanel.isVisible() )
		{
			prefs.put( MINIMAL_SIZE, minimalSizeTextField.getText().trim() );
			prefs.put( MAXIMAL_SIZE, maximalSizeTextField.getText().trim() );
			prefs.put( BACKGROUND_SIZE, backgroundSizeTextField.getText().trim() );
		}

		if ( !sizeFilterSettingsPanel.isVisible() && miniSizeFilterSettingsPanel.isVisible() )
		{
			prefs.put( MINIMAL_SIZE, miniMinimalSizeTextField.getText().trim() );
			prefs.put( BACKGROUND_SIZE, miniBackgroundSizeTextField.getText().trim() );
		}

		prefs.put( DEVIATION, deviationTextField.getText().trim() );
		prefs.putBoolean( REMEMBER_SETTINGS, rememberSettingsCheckBox.isSelected() );
	}

}
