package de.fzj.jungle.profile;

import org.scijava.plugin.Plugin;

/**
 * {@link Profile} used for analyzing a specific kind of experiment.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = Profile.class, name = "BalabanBHI" )
public class BalabanBHIProfile extends AbstractProfile
{

	static private BalabanBHIProfile instance = null;

	/**
	 * 
	 */
	public BalabanBHIProfile()
	{
		this.profileName = "BalabanBHI";

		/*
		 * Shape Index Map
		 */
		this.simLowerThreshold = -1.0d;
		this.simUpperThreshold = -0.25d;
		this.simGaussRadius = 5.0d;

		/*
		 * Thresholding
		 */
		this.thresholdingMethod = "net.imagej.ops.threshold.localPhansalkar.LocalPhansalkarThresholdIntegral";
		this.thresholdingRadius = 40;
		this.thresholdingK1 = 0.25d;
		this.thresholdingK2 = 0.35d;
		this.thresholdingR = 0.5;

		/*
		 * Morphological operations
		 */
		this.numberOfDilations = 20;
		this.numberOfErosions = 2;
		this.medianRadius = 10d;
	}

	public static Profile getInstance()
	{
		if ( BalabanBHIProfile.instance == null )
		{
			BalabanBHIProfile.instance = new BalabanBHIProfile();
		}

		return BalabanBHIProfile.instance;
	}

}
