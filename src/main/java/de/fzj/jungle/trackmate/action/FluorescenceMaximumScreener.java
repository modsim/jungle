package de.fzj.jungle.trackmate.action;

import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.CRIMSON_FLUORESCENCE_MEAN;
import static de.fzj.jungle.trackmate.features.spot.SpotFluorescenceAnalyzerFactory.YFP_FLUORESCENCE_MEAN;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.ArrayUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import ij.ImagePlus;

/**
 * {@link TrackMateAction} that computes the maximum fluorescence intensity for
 * all detected spots.
 * 
 * @author Stefan Helfrich
 */
public class FluorescenceMaximumScreener extends AbstractTMAction {

	private double[] maximumValues;
	
	/*
	 * CONSTRUCTOR
	 */
	public FluorescenceMaximumScreener() {
		// NB: initialization not possible
	}
	
	@Override
	public void execute(TrackMate trackmate) {		
		final Model model = trackmate.getModel();		
		
		/*
		 * Had to move that part to the execution since the ImagePlus is not
		 * available in either of the constructors.
		 */
		// Elicit dimensions
		ImagePlus imp = trackmate.getSettings().imp;
		int channels;
		if (imp.isHyperStack()) {
			channels = imp.getNChannels();
		} else {
			channels = 1;
		}
		
		// Initialize array
		this.maximumValues = new double[channels];		
		
		// Iterate over all channels to determine the maximum values
		SpotCollection spots = model.getSpots();
		double[] yfpValues = spots.collectValues(YFP_FLUORESCENCE_MEAN, false);
		double[] crimsonValues = spots.collectValues(CRIMSON_FLUORESCENCE_MEAN, false);
		
		maximumValues[0] = 0d;
		maximumValues[1] = Collections.max(Arrays.asList(ArrayUtils.toObject(yfpValues)));
		maximumValues[2] = Collections.max(Arrays.asList(ArrayUtils.toObject(crimsonValues)));
	}

	/**
	 * @return the maximumValues
	 */
	public double[] getMaximumValues() {
		return maximumValues;
	}
	
}
