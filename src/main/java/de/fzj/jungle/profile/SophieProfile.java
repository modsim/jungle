package de.fzj.jungle.profile;

import org.scijava.plugin.Plugin;

/**
 * @author Stefan Helfrich
 */
@Plugin( type = Profile.class, name = "Sophie" )
public class SophieProfile extends AbstractProfile
{

	static private SophieProfile instance = null;

	/**
	 * 
	 */
	public SophieProfile()
	{
		this.profileName = "Sophie";

		/*
		 * Shape Index Map
		 */
		this.simLowerThreshold = -1.0d;
		this.simUpperThreshold = -0.20d;
		this.simGaussRadius = 5.0d;

		/*
		 * Thresholding
		 */
		this.thresholdingMethod = "net.imagej.ops.threshold.localSauvola.LocalSauvolaThresholdIntegral";
		this.thresholdingRadius = 20;
		this.thresholdingK1 = 0.25d;
		this.thresholdingK2 = 0.35d;
		this.thresholdingR = 128;

		/*
		 * Morphological operations
		 */
		this.numberOfDilations = 10;
		this.numberOfErosions = 2;
		this.medianRadius = 10d;
	}

	public static Profile getInstance()
	{
		if ( SophieProfile.instance == null )
		{
			SophieProfile.instance = new SophieProfile();
		}

		return SophieProfile.instance;
	}

}
