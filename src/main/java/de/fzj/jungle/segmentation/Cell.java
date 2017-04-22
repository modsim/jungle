/**
 * $LICENSE
 */
package de.fzj.jungle.segmentation;

import java.awt.Polygon;
import java.io.Serializable;
import java.util.Set;

import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * This class implements a cell that consists mainly of a contour and an inside
 * area.
 * <p>
 * Additional characteristics of a cell are stored as fields. Those fields are
 * used to compare two cells in the tracking process.
 * <p>
 * Each <code>Cell</code> instance has a unique identifier.
 * 
 * @author Stefan Helfrich
 */
public class Cell extends Spot implements Serializable
{

	/*
	 * STATIC FIELDS
	 */
	private static final long serialVersionUID = 4033611990090130567L;

	/**
	 * Counter for unique identifiers of cells. Increased at creation of a
	 * <code>Cell</code> object.
	 */
	private static int cellIdCounter = 0;

	/*
	 * PRIVATE FIELDS
	 */

	/** Unique (integer) identifier for a cell. */
	private int cellId;

	/** The cell contour. */
	private Polygon contour;

	private Roi cellRoi;

	private ImageProcessor ip;

	/**
	 * An unsorted set containing all pixels declared as inside of the cell.
	 * 
	 * TODO do we really need this?!
	 */
	private Set< Coordinate > inside;

	private Calibration calibration;

	/**
	 * Constructs a {@link Cell}.
	 */
	public Cell()
	{
		super( 0d, 0d, 0d, 0d, 0d );
		this.cellId = Cell.cellIdCounter++;
	}

	/**
	 * Constructs a {@link Cell} from a {@link Roi}, an {@link ImageProcessor},
	 * and a {@link Calibration} taken from an {@link ImagePlus}.
	 * 
	 * @param cellRoi
	 *            {@link Roi} to associate with the cell.
	 * @param ip
	 *            {@link ImageProcessor} with which {@code this} is associated.
	 * @param calibration
	 *            {@link Calibration} information that is used for extracting
	 *            measurements about {@code this}.
	 */
	public Cell( Roi cellRoi, ImageProcessor ip, Calibration calibration )
	{
		// This will also call a constructor of Spot (with default values)
		this();

		// Set the Cell variables
		this.cellRoi = cellRoi;
		this.ip = ip;
		this.calibration = calibration;

		// Set the Spot variables
		this.setName( cellRoi.getName() );

		double[] centroid = this.computeCentroidArray( calibration );
		this.putFeature( POSITION_X, Double.valueOf( centroid[ 0 ] ) );
		this.putFeature( POSITION_Y, Double.valueOf( centroid[ 1 ] ) );
		this.putFeature( POSITION_Z, Double.valueOf( centroid[ 2 ] ) );

		this.putFeature( RADIUS, Double.valueOf( 0.25d ) );
		this.putFeature( QUALITY, Double.valueOf( 1d ) );
	}

	/**
	 * Constructs a {@link Cell} that has a {@link Contour} associated with it.
	 * 
	 * @param contour
	 *            {@link Contour} associated with {@code this}
	 * @param cellRoi
	 *            {@link Roi} to associate with the cell.
	 * @param ip
	 *            {@link ImageProcessor} with which {@code this} is associated.
	 * @param calibration
	 *            {@link Calibration} information that is used for extracting
	 *            measurements about {@code this}.
	 */
	@Deprecated
	public Cell( Polygon contour, Roi cellRoi, ImageProcessor ip, Calibration calibration )
	{
		this( cellRoi, ip, calibration );

		this.contour = contour;
	}

	/**
	 * @return the contour
	 */
	@Deprecated
	public Polygon getContour()
	{
		return contour;
	}

	/**
	 * @param contour
	 *            the contour to set
	 */
	@Deprecated
	public void setContour( Polygon contour )
	{
		this.contour = contour;
	}

	/**
	 * @return the contour
	 */
	public Polygon getPolygon()
	{
		return contour;
	}

	/**
	 * @param contour
	 *            the contour to set
	 */
	public void setPolygon( Polygon contour )
	{
		this.contour = contour;
	}

	/**
	 * TODO description
	 * 
	 * @return the inside
	 */
	@Deprecated
	public Set< Coordinate > getInside()
	{
		return inside;
	}

	/**
	 * TODO description
	 * 
	 * @param inside
	 *            the inside to set
	 */
	@Deprecated
	public void setInside( Set< Coordinate > inside )
	{
		this.inside = inside;
	}

	/**
	 * TODO description
	 * 
	 * @return the cellId
	 */
	public int getCellId()
	{
		return cellId;
	}

	/**
	 * TODO description
	 * 
	 * @param cellId
	 *            the cellId to set
	 */
	public void setCellId( int cellId )
	{
		this.cellId = cellId;
	}

	/**
	 * @return the ip
	 */
	public ImageProcessor getImageProcessor()
	{
		return ip;
	}

	/**
	 * @param ip
	 *            the ip to set
	 */
	public void setImageProcessor( ImageProcessor ip )
	{
		this.ip = ip;
	}

	public double getEnclosedArea()
	{
		return getEnclosedArea( calibration );
	}

	/**
	 * Computes the size of the enclosed area by {@code this}.
	 * 
	 * @param calibration
	 *            {@link Calibration} used for computing in real world units
	 * @return The area that is covered by {@code this}.
	 */
	@SuppressWarnings( "hiding" )
	public double getEnclosedArea( Calibration calibration )
	{
		try
		{
			ip.setRoi( cellRoi );
		}
		catch ( IllegalArgumentException e )
		{
			System.err.println( "Blaaaaa" );
		}

		ImageStatistics stats = ImageStatistics.getStatistics( ip, Measurements.AREA | Measurements.CENTROID, calibration );

		return stats.area;
	}

	/**
	 * An update of featureVector can be triggered manually upon changing.
	 * 
	 * @deprecated Future versions, however, will realise an observer-based
	 *             solution that makes this method unnecessary.
	 */
	@Deprecated
	public void updateFeatureVector()
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "updateFeatureVector() is not implemented yet." );
	}

	public Coordinate computeCentroid()
	{
		return computeCentroid( null );
	}

	public double[] computeCentroidArray()
	{
		return computeCentroidArray( null );
	}

	/**
	 * The method computes the center of gravity according to
	 * 
	 * x_ = (1 / cell_area) * \sum_{cell_area} x and y_ = (1 / cell_area) *
	 * \sum_{cell_area} y.
	 * 
	 * @return the centroid in form of a Coordinate
	 */
	@SuppressWarnings( "hiding" )
	public Coordinate computeCentroid( Calibration calibration )
	{
		try
		{
			ip.setRoi( cellRoi );
		}
		catch ( IllegalArgumentException e )
		{
			System.err.println( "Blaaaaa" );
		}

		ImageStatistics stats = ImageStatistics.getStatistics( ip, Measurements.CENTROID, calibration );

		return new Coordinate( ( short ) stats.xCentroid, ( short ) stats.yCentroid, ( short ) 0 );
	}

	@SuppressWarnings( "hiding" )
	public double[] computeCentroidArray( Calibration calibration )
	{
		try
		{
			ip.setRoi( cellRoi );
		}
		catch ( IllegalArgumentException e )
		{
			System.err.println( "Blaaaaa" );
		}

		ImageStatistics stats = ImageStatistics.getStatistics( ip, Measurements.CENTROID, calibration );

		return new double[] { stats.xCentroid, stats.yCentroid, 0d };
	}

	/**
	 * @return the cellRoi
	 */
	public Roi getCellRoi()
	{
		return cellRoi;
	}

	/**
	 * @param cellRoi
	 *            the cellRoi to set
	 */
	public void setCellRoi( Roi cellRoi )
	{
		this.cellRoi = cellRoi;
	}

	public Calibration getCalibration()
	{
		return calibration;
	}

	public String getNameFromId()
	{
		return String.format( "C%d", getCellId() );
	}

	public static int getAndIncreaseCellIdCounter()
	{
		return Cell.cellIdCounter++;
	}

	public static void initCellIdCounter( int id )
	{
		Cell.cellIdCounter = id;
	}

	@Override
	public String getName()
	{
		return getNameFromId();
	}
}
