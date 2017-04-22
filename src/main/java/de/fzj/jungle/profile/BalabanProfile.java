/**
 * 
 */
package de.fzj.jungle.profile;

import org.scijava.plugin.Plugin;

/**
 * @author Stefan Helfrich <s.helfrich@fz-juelich.de>
 * @version 0.1
 *
 */
@Plugin(type = Profile.class, name = "Balaban")
public class BalabanProfile extends AbstractProfile {
	
	static private BalabanProfile instance = null;
	
	/**
	 * 
	 */
	public BalabanProfile() {
		this.profileName = "Balaban";

		/*
		 * Shape Index Map
		 */
		this.simLowerThreshold = -1.0d;
		this.simUpperThreshold = -0.2d;
		this.simGaussRadius = 5.0d;
		
		/*
		 * Thresholding
		 */
		this.thresholdingMethod = "net.imagej.ops.threshold.localBernsen.LocalBernsenThreshold";
		this.thresholdingRadius = 50;
		this.thresholdingK1 = 0.35d;
		this.thresholdingK2 = 0.45d;
		this.thresholdingR = 128;
		
		/*
		 * Morphological operations
		 */
		this.numberOfDilations = 10;
		this.numberOfErosions = 5;
		this.medianRadius = 10d;
	}
	
	public static Profile getInstance() {
		if (BalabanProfile.instance == null) {
			BalabanProfile.instance = new BalabanProfile();
		}
		
		return BalabanProfile.instance;
	}

}
