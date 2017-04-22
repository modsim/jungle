package de.fzj.jungle.preprocessing.boxdetection;

import java.util.List;
import java.util.TreeSet;

import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLine;

public interface BoxClassifier {
	
	/**
	 * The maximal possible deviation from width and height of a structure.
	 */
	final static double DIMENSION_EPSILON = 5;
	final static double RATIO_EPSILON = 0.01;
	
	public TreeSet<HoughRectangle> classify(List<HoughLine> lines);
}
