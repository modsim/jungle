package de.fzj.jungle.profile;

import org.scijava.plugin.Plugin;

/**
 * @author Stefan Helfrich
 */
@Plugin( type = Profile.class, name = "DFG" )
public class DFGProfile extends AbstractProfile
{

	static private DFGProfile instance = null;

	/**
	 * 
	 */
	public DFGProfile()
	{
		this.profileName = "DFG";

		/*
		 * Shape Index Map
		 */
		this.simLowerThreshold = -1.0d;
		this.simUpperThreshold = -0.35d;
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
		if ( DFGProfile.instance == null )
		{
			DFGProfile.instance = new DFGProfile();
		}

		return DFGProfile.instance;
	}

}
