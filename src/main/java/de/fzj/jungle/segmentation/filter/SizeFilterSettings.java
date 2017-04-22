package de.fzj.jungle.segmentation.filter;

/**
 * {@link FilterSettings} for a {@link SizeFilter}.
 * 
 * @author Stefan Helfrich
 */
public class SizeFilterSettings implements FilterSettings {

	/**
	 * How the SizeFilter works:
	 * 
	 * min (1)     (2)     (3)     (4) max
	 *  |-------|-------|-------|-------|
	 *         (x)     (y)     (z) 
	 *  
	 *  (1) too small - noise
	 *  (2) single cell
	 *  (3) multiple cells
	 *  (4) too large - background
	 *  
	 *  (x) singleCellThreshold
	 *  (y) multipleCellThreshold
	 *  (z) backgroundThreshold
	 */
	double singleCellThreshold;
	double multipleCellsThreshold;
	double backgroundThreshold;

	/**
	 * Standard constructor.
	 * 
	 * @param singleCellThreshold
	 * @param multipleCellsThreshold
	 * @param backgroundThreshold
	 */
	public SizeFilterSettings( double singleCellThreshold, double multipleCellsThreshold, double backgroundThreshold )
	{
		super();
		this.singleCellThreshold = singleCellThreshold;
		this.multipleCellsThreshold = multipleCellsThreshold;
		this.backgroundThreshold = backgroundThreshold;
	}

	/**
	 * @return the singleCellThreshold
	 */
	public double getSingleCellThreshold()
	{
		return singleCellThreshold;
	}

	/**
	 * @param singleCellThreshold
	 *            the singleCellThreshold to set
	 */
	public void setSingleCellThreshold( double singleCellThreshold )
	{
		this.singleCellThreshold = singleCellThreshold;
	}

	/**
	 * @return the multipleCellsThreshold
	 */
	public double getMultipleCellsThreshold()
	{
		return multipleCellsThreshold;
	}

	/**
	 * @param multipleCellsThreshold
	 *            the multipleCellsThreshold to set
	 */
	public void setMultipleCellsThreshold( double multipleCellsThreshold )
	{
		this.multipleCellsThreshold = multipleCellsThreshold;
	}

	/**
	 * @return the backgroundThreshold
	 */
	public double getBackgroundThreshold()
	{
		return backgroundThreshold;
	}

	/**
	 * @param backgroundThreshold
	 *            the backgroundThreshold to set
	 */
	public void setBackgroundThreshold( double backgroundThreshold )
	{
		this.backgroundThreshold = backgroundThreshold;
	}

}
