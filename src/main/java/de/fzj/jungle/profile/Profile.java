package de.fzj.jungle.profile;

import net.imagej.ImageJPlugin;
import net.imagej.ops.threshold.LocalThresholdMethod;

/**
 * Interface for profiles that are used for storing parameters for different
 * studies.
 * 
 * @author Stefan Helfrich
 */
public interface Profile extends ImageJPlugin
{
	/**
	 * @return the thresholdingMethod
	 */
	public String getProfileName();

	/**
	 * @return the simLowerThreshold
	 */
	public double getSimLowerThreshold();

	/**
	 * @return the simUpperThreshold
	 */
	public double getSimUpperThreshold();

	/**
	 * @return the simGaussRadius
	 */
	public double getSimGaussRadius();

	/**
	 * @return the thresholdingMethod
	 */
	public String getThresholdingMethod();

	/**
	 * @return the thresholdingRadius
	 */
	public int getThresholdingRadius();

	/**
	 * @return the thresholdingK1
	 */
	public double getThresholdingK1();

	/**
	 * @return the thresholdingK2
	 */
	public double getThresholdingK2();

	/**
	 * @return the thresholdingR
	 */
	public double getThresholdingR();

	/**
	 * @return the numberOfDilations
	 */
	public int getNumberOfDilations();

	/**
	 * @return the numberOfErosions
	 */
	public int getNumberOfErosions();

	/**
	 * @return the medianRadius
	 */
	public double getMedianRadius();

	public < T extends LocalThresholdMethod< ? > > Class< T > getThresholdingMethodClass();
}
