package de.fzj.jungle.preprocessing.boxdetection;

import de.fzj.jungle.preprocessing.hough.LinearHT.HoughLinePair;

/**
 * Implements a rectangle defined by two {@link HoughLinePair}s.
 * 
 * @author Stefan Helfrich
 */
public class HoughRectangle implements Comparable< HoughRectangle >
{

	public final HoughLinePair houghPair1;

	public final HoughLinePair houghPair2;

	/**
	 * @param houghPair1
	 * @param houghPair2
	 */
	public HoughRectangle( HoughLinePair houghPair1, HoughLinePair houghPair2 )
	{
		this.houghPair1 = houghPair1;
		this.houghPair2 = houghPair2;
	}

	public int getSumOfScores()
	{
		return houghPair1.getSumOfScores() + houghPair2.getSumOfScores();
	}

	@Override
	public int compareTo( HoughRectangle rect )
	{
		Integer score = this.getSumOfScores();
		return score.compareTo( rect.getSumOfScores() );
	}

}
