package de.fzj.jungle.profile;

import org.scijava.plugin.Plugin;

/**
 * Default {@link Profile}.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = Profile.class, name = "Default" )
public class DefaultProfile extends AbstractProfile
{

	static private AbstractProfile instance = null;

	/**
	 * 
	 */
	public DefaultProfile()
	{
		this.profileName = "Default";

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
		this.numberOfDilations = 30;
		this.numberOfErosions = 2;
		this.medianRadius = 10d;
	}

	public static Profile getInstance()
	{
		if ( DefaultProfile.instance == null )
		{
			DefaultProfile.instance = new DefaultProfile();
		}

		return DefaultProfile.instance;
	}

}
