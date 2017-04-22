package de.fzj.jungle.profile;

import net.imagej.ops.threshold.LocalThresholdMethod;

/**
 * Abstract base class for {@link Profile}s.
 * 
 * @author Stefan Helfrich
 */
public abstract class AbstractProfile implements Profile {
	
	protected String profileName;
	
	/*
	 * Shape Index Map
	 */
	protected double simLowerThreshold;
	protected double simUpperThreshold;
	protected double simGaussRadius;
	
	/*
	 * Thresholding
	 */
	protected String thresholdingMethod;
	protected int thresholdingRadius;
	protected double thresholdingK1;
	protected double thresholdingK2;
	protected double thresholdingR;
	
	/*
	 * Morphological operations
	 */
	protected int numberOfDilations;
	protected int numberOfErosions;
	protected double medianRadius;
	
	@Override
	public String getProfileName() {
		return profileName;
	}
	
	@Override
	public double getSimLowerThreshold() {
		return simLowerThreshold;
	}
	
	@Override
	public double getSimUpperThreshold() {
		return simUpperThreshold;
	}
	
	@Override
	public double getSimGaussRadius() {
		return simGaussRadius;
	}
	
	@Override
	public String getThresholdingMethod() {
		return thresholdingMethod;
	}
	
	@Override
	public int getThresholdingRadius() {
		return thresholdingRadius;
	}
	
	@Override
	public double getThresholdingK1() {
		return thresholdingK1;
	}
	
	@Override
	public double getThresholdingK2() {
		return thresholdingK2;
	}
	
	@Override
	public double getThresholdingR() {
		return thresholdingR;
	}
	
	@Override
	public int getNumberOfDilations() {
		return numberOfDilations;
	}
	
	@Override
	public int getNumberOfErosions() {
		return numberOfErosions;
	}
	
	@Override
	public double getMedianRadius() {
		return medianRadius;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T extends LocalThresholdMethod<?>> Class<T> getThresholdingMethodClass() {
		Class<T> cls = null;
		try {
			cls = (Class<T>) Class.forName(thresholdingMethod);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return cls;
	}
}
