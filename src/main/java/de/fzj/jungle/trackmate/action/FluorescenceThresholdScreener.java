package de.fzj.jungle.trackmate.action;

import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.YFP_FLUORESCENCE_MEAN;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import ij.IJ;
import ij.gui.GenericDialog;

/**
 * A {@link TrackMateAction} that checks if at least one spot has exceeded the
 * provided threshold.
 * 
 * @author Stefan Helfrich
 */
public class FluorescenceThresholdScreener extends AbstractTMAction
{

	private double[] thresholds = new double[ 3 ];

	private boolean thresholdExceeded = false;

	private GenericDialog gd;

	/*
	 * CONSTRUCTOR
	 */
	public FluorescenceThresholdScreener()
	{
		this.gd = new GenericDialog( "Thresholds" );

		// TODO Extend to n fluorescences
		gd.addNumericField( "yfp Threshold", 500d, 1 );
		// gd.addNumericField("crimson Threshold", 500d, 4);
	}

	public void setThresholds( double... thresholds )
	{
		this.thresholds = thresholds;
	}

	@Override
	public void execute( TrackMate trackmate )
	{
		// If the thresholds have not already been set, show a GenericDialog
		if ( thresholds[ 0 ] == 0 )
		{
			gd.showDialog();

			if ( gd.wasCanceled() ) { return; }

			thresholds[ 0 ] = gd.getNextNumber();
			// thresholds[1] = gd.getNextNumber();
		}

		double yfpThreshold = thresholds[ 0 ];
		// double crimsonTreshold = thresholds[1];

		final Model model = trackmate.getModel();

		SpotCollection spots = model.getSpots();
		double[] yfpValues = spots.collectValues( YFP_FLUORESCENCE_MEAN, false );

		for ( Double value : yfpValues )
		{
			if ( value > yfpThreshold )
			{
				IJ.showMessage( "yfp Threshold exceeded" );
				// IJ.log("yfp Threshold exceeded");
				this.thresholdExceeded = true;
				return;
			}
		}
	}

	public boolean isThresholdExceeded()
	{
		return this.thresholdExceeded;
	}

}
