package de.fzj.jungle.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import fiji.tool.AbstractTool;
import fiji.tool.ToolToggleListener;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

/**
 * A tool that enables the convenient splitting, merging, and deleting of region
 * of interests that are located in an overlay.
 * 
 * When the tool is active, the following functionality is available:
 * <ul>
 * <li>click a ROI to select it</li>
 * <li>split it by selecting a start point outside of the active ROI and drag
 * the line to the other side of the ROI</li>
 * <li>hold the shift key down and select two ROIs after another to merge them
 * </li>
 * </ul>
 * 
 * @author Stefan Helfrich
 */
public class RoiSplittingTool extends AbstractTool implements MouseMotionListener, MouseListener, ToolToggleListener, KeyListener, FocusListener
{

	static Cursor defaultCursor = new Cursor( Cursor.DEFAULT_CURSOR );

	static Cursor handCursor = new Cursor( Cursor.HAND_CURSOR );

	static Cursor crosshairCursor = new Cursor( Cursor.CROSSHAIR_CURSOR );

	private ImagePlus imp;

	private ImageCanvas canvas;

	private boolean draggingActive = false;

	private int originX, originY;

	private boolean selectionMode = false;

	private boolean mergeMode = false;

	private boolean panMode = false;

	private LinkedList< Roi > selectedRois = new LinkedList<>();

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		// NB: not handled
	}

	@Override
	public void mouseEntered( final MouseEvent e )
	{
		// NB: not handled
	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
		// NB: not handled
	}

	@Override
	public void mousePressed( MouseEvent e )
	{
		if ( mergeMode )
		{
			e.consume();
			return;
		}

		if ( panMode )
		{
			e.consume();
			return;
		}

		if ( mouseOverRoi( e ) != null )
		{
			selectionMode = true;

			e.consume();
			return;
		}

		ImageCanvas source = ( ImageCanvas ) e.getSource();
		if ( source != canvas )
		{
			// We changed image window. Update fields accordingly
			ImageWindow window = ( ImageWindow ) source.getParent();
			imp = window.getImagePlus();
			canvas = source;
		}

		originX = e.getX();
		originY = e.getY();

		draggingActive = true;
		e.consume();
	}

	@Override
	public void mouseReleased( MouseEvent e )
	{
		if ( panMode )
		{
			e.consume();
			return;
		}

		if ( selectionMode )
		{
			/*
			 * We have to remove the ROI from the overlay and add the one we get
			 * from imp.getRoi() afterwards. This fixes a nasty issue where
			 * selecting a ROI a second time and deleting it results in a
			 * coloured/filled ROI.
			 */
			// Delete ROI from overlay
			Roi selectedRoi = mouseOverRoi( e );

			/*
			 * We need to take care of the scenario where the user starts a drag
			 * inside of a cell and releases the mouse outside of a cell. In
			 * that case selectionMode is TRUE but mouseOverRoi returns null,
			 * effectively adding null to the overlay. This will crash all
			 * subsequent calls to mouseOverRoi.
			 */
			if ( selectedRoi == null )
			{
				selectionMode = false;
				draggingActive = false;

				e.consume();
				return;
			}

			Overlay overlay = imp.getOverlay();
			if ( overlay != null )
			{
				overlay.remove( selectedRoi );
			}

			// Activate the ROI
			imp.setRoi( selectedRoi, true );
			selectionMode = false;

			// Add the pseudo-duplicate to the overlay
			if ( overlay != null )
			{
				overlay.add( imp.getRoi() );
			}

			if ( IJ.debugMode )
			{
				Roi impRoi = imp.getRoi();

				if ( overlay != null )
				{
					for ( Roi overlayRoi : overlay.toArray() )
					{
						if ( impRoi == overlayRoi )
						{
							IJ.log( "Found imp.getRoi() in overlay" );
							break;
						}
					}
				}
			}

			e.consume();
			return;
		}

		if ( mergeMode )
		{
			Roi r = mouseOverRoi( e );

			if ( r != null )
			{
				selectedRois.add( r );
			}
			e.consume();
			return;
		}

		// Compute Roi for splitting
		double x1 = canvas.offScreenX( originX );
		double x2 = canvas.offScreenX( e.getX() );
		double y1 = canvas.offScreenY( originY );
		double y2 = canvas.offScreenY( e.getY() );

		if ( x1 == x2 && y1 == y2 )
		{
			// Deselect ROI
			imp.setRoi( ( Roi ) null );

			// Deactivate dragging
			draggingActive = false;

			return;
		}

		ShapeRoi splitRoi = computeShapeRoiFromPoints( x1, y1, x2, y2 );

		// Get the ROI that will be split from somewhere
		Roi toBeSplitRoi = imp.getRoi();

		if ( toBeSplitRoi == null )
		{
			ImageWindow focusedWindow = WindowManager.getCurrentWindow();
			IJ.error( "No Roi selected." );
			canvas.paint( canvas.getGraphics() );

			WindowManager.toFront( focusedWindow );
			return;
		}

		ShapeRoi toBeSplitShapeRoi = new ShapeRoi( toBeSplitRoi );

		// Split the ROI
		Roi[] resultingRois = splitRoi( toBeSplitShapeRoi, splitRoi );

		/*
		 * Get it to where it came from
		 */
		RoiManager manager = RoiManager.getInstance();
		if ( manager == null )
		{
			manager = new RoiManager( true );
		}

		int roiIdx = manager.getRoiIndex( toBeSplitRoi );

		// Roi is from RoiManager
		if ( roiIdx != -1 )
		{
			synchronized ( manager )
			{
				manager.select( roiIdx );
				manager.runCommand( "delete" );
			}

			for ( int i = 0; i < resultingRois.length; i++ )
			{
				// Set name and position of the split result
				Roi r = resultingRois[ i ];
				r.setName( toBeSplitRoi.getName() + "-" + i );

				copyRoiPosition( toBeSplitRoi, r );

				manager.addRoi( r );
			}

			// Sort Roi list after adding
			manager.runCommand( "sort" );
		}
		else
		{
			// Roi is from Overlay
			if ( toBeSplitRoi.isActiveOverlayRoi() )
			{
				Overlay overlay = imp.getOverlay();
				overlay.remove( toBeSplitRoi );

				for ( int i = 0; i < resultingRois.length; i++ )
				{
					// Set name and position of the split result
					Roi r = resultingRois[ i ];
					r.setName( toBeSplitRoi.getName() + "-" + i );

					copyRoiPosition( toBeSplitRoi, r );

					overlay.add( r );
				}
			}
		}

		// Deselect all Rois
		imp.setRoi( ( Roi ) null );

		// Repaint the canvas
		canvas.paint( canvas.getGraphics() );

		// Disable dragging mode
		draggingActive = false;

		e.consume();
	}

	@Override
	public void mouseDragged( MouseEvent e )
	{
		if ( panMode ) { return; }

		if ( draggingActive )
		{
			canvas.paint( canvas.getGraphics() );

			Graphics2D currentGraphics = ( Graphics2D ) canvas.getGraphics();
			currentGraphics.setStroke( new BasicStroke( 3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10.0f }, 0.0f ) );
			currentGraphics.setPaint( Color.RED );
			currentGraphics.drawLine( originX, originY, e.getX(), e.getY() );
		}
	}

	@Override
	public void mouseMoved( MouseEvent e )
	{
		if ( mouseOverRoi( e ) != null || panMode )
		{
			canvas.setCursor( handCursor );
		}
		else
		{
			canvas.setCursor( crosshairCursor );
		}
	}

	private Roi[] splitRoi( ShapeRoi roi, ShapeRoi splitRoi )
	{
		roi = roi.not( splitRoi );

		Roi[] splittingResult = roi.getRois();
		List< Roi > finalResult = new ArrayList<>();

		// Remove single-pixel ROIs from splitting result
		for ( Roi r : splittingResult )
		{
			Rectangle rect = r.getBounds();
			double width = rect.getWidth();
			double heigth = rect.getHeight();

			if ( width > 0.0d && heigth > 0.0d )
			{
				finalResult.add( r );
			}
		}

		return finalResult.toArray( new Roi[ finalResult.size() ] );
	}

	private ShapeRoi computeShapeRoiFromPoints( double x1, double y1, double x2, double y2 )
	{
		// TODO other way around :P
		double dx = x2 - x1;
		double dy = y2 - y1;

		// Euclidean distance
		double distance = Math.sqrt( Math.pow( dx, 2 ) + Math.pow( dy, 2 ) );

		{
			/*
			 * We prolong the splitting line a tiny bit to improve the results
			 */
			x1 = x1 - ( 1 / distance ) * dx;
			y1 = y1 - ( 1 / distance ) * dy;

			x2 = x1 + dx + ( 1 / distance ) * dx;
			y2 = y1 + dy + ( 1 / distance ) * dy;
		}

		// Compute new points
		double x1_shift = x1 + ( 1 / distance ) * ( -1 * dy );
		double y1_shift = y1 + ( 1 / distance ) * dx;

		double x2_shift = x2 + ( 1 / distance ) * ( -1 * dy );
		double y2_shift = y2 + ( 1 / distance ) * dx;

		FloatPolygon poly = new FloatPolygon();
		poly.addPoint( x1, y1 );
		poly.addPoint( x1_shift, y1_shift );
		poly.addPoint( x2_shift, y2_shift );
		poly.addPoint( x2, y2 );

		Roi polygonRoi = new PolygonRoi( poly, Roi.POLYGON );

		return new ShapeRoi( polygonRoi );
	}

	private Roi mouseOverRoi( MouseEvent e )
	{
		int sx = e.getX();
		int sy = e.getY();
		int ox = canvas.offScreenX( sx );
		int oy = canvas.offScreenY( sy );

		return mouseOverRoi( ox, oy );
	}

	private Roi mouseOverRoi( int ox, int oy )
	{
		Overlay overlay = imp.getOverlay();
		Roi[] rois;

		// There is no overlay .. work with Rois and RoiManager
		if ( overlay == null )
		{
			RoiManager manager = RoiManager.getInstance();
			if ( manager == null )
			{
				manager = new RoiManager( true );
			}

			rois = manager.getRoisAsArray();
		}
		else
		{
			rois = overlay.toArray();
		}

		for ( Roi r : rois )
		{
			int roiPosition = r.getPosition();
			int impPosition = imp.getCurrentSlice();

			// Handle hyperstack separately, since r.getPosition()==0 for
			// hyperstacks
			if ( roiPosition <= 0 )
			{
				roiPosition = r.getTPosition();
				impPosition = imp.getT();
			}

			if ( r.contains( ox, oy ) && ( roiPosition == impPosition ) ) { return r; }
		}

		return null;
	}

	@Override
	public String getToolName()
	{
		return "ROI Splitting Tool";
	}

	@Override
	public String getToolIcon()
	{
		return "C000D00D01D02D03D04D05D06D07D08D09D0aD0bD0cD0dD0eD0fD10D11D12D13D18D19D1aD1bD1cD1fD20D21D22D25D26D29D2aD2bD2fD30D31D34D35D36D37D3aD3eD3fD40D41D43D44D45D46D47D48D4dD4eD4fD50D51D53D54D55D56D57D58D5cD5dD5eD5fD60D61D63D64D65D66D67D6dD6eD6fD70D71D74D75D76D7aD7bD7dD7eD7fD80D81D82D84D85D89D8aD8bD8eD8fD90D91D92D98D99D9aD9bD9cD9fDa0Da1Da2Da3Da7Da8Da9DaaDabDacDadDafDb0Db1Db2Db8Db9DbaDbbDbcDbdDbfDc0Dc1Dc5Dc6Dc9DcaDcbDccDcdDcfDd0Dd4Dd5Dd6Dd7DdbDdcDdfDe0De3De4De5De6De7De8De9DeeDefDf0Df1Df2Df3Df4Df5Df6Df7Df8Df9DfaDfbDfcDfdDfeDff";
	}

	@Override
	public void run( String arg )
	{
		// getToolId() returns -1 if no tool with the given name is found
		Toolbar globalToolbar = Toolbar.getInstance();
		if ( globalToolbar.getToolId( "ROI Splitting Tool" ) >= 0 ) { return; }

		// Let's add our tool as focus listener
		WindowManager.getCurrentWindow().addFocusListener( this );

		// Launch interactive mode
		super.run( arg );
	}

	@Override
	public void toolToggled( boolean enabled )
	{
		imp = WindowManager.getCurrentImage();

		if ( imp != null )
		{
			canvas = imp.getCanvas();
		}

		selectionMode = false;
		mergeMode = false;
	}

	@Override
	public void keyPressed( KeyEvent e )
	{
		final int keyCode = e.getKeyCode();

		switch ( keyCode )
		{
		case KeyEvent.VK_SHIFT:
			mergeMode = true;
			e.consume();
			break;

		case KeyEvent.VK_SPACE:
			panMode = true;
			break;
		}
	}

	@Override
	public void keyReleased( KeyEvent e )
	{
		if ( e.getKeyCode() == KeyEvent.VK_SHIFT )
		{
			ShapeRoi shape = null;
			Overlay overlay = imp.getOverlay();

			if ( selectedRois.size() < 2 )
			{
				selectedRois.clear();
				mergeMode = false;
				e.consume();
			}

			for ( Roi r : selectedRois )
			{
				ShapeRoi s = new ShapeRoi( r );
				s.setName( r.getName() );

				copyRoiPosition( r, s );

				// Remove
				if ( overlay == null )
				{
					// Handle ROI
				}
				else
				{
					overlay.remove( r );
				}

				// combine
				if ( shape == null )
				{
					shape = s;
					continue;
				}

				shape.or( s );
			}

			// add new one to overlay (what about the name?)
			if ( shape != null )
			{
				if ( overlay != null )
				{
					overlay.add( shape );
				}

				// Set the result as active ROI so the user can visually verify
				// the merge
				imp.setRoi( shape );
			}

			selectedRois.clear();
			mergeMode = false;
			e.consume();
		}

		if ( e.getKeyCode() == KeyEvent.VK_SPACE )
		{
			panMode = false;
		}
	}

	@Override
	public void focusGained( FocusEvent e )
	{
		// NB: not handled
	}

	@Override
	public void focusLost( FocusEvent e )
	{
		selectedRois.clear();
		mergeMode = false;
		selectionMode = false;
	}

	@Override
	public void keyTyped( KeyEvent e )
	{
		// NB: not handled
	}

	/**
	 * Copies the position of one Roi to another Roi. It does so by looking at
	 * which properties are set {@link Roi#getPosition()} or
	 * {@link Roi#getCPosition()}/{@link Roi#getZPosition()}/
	 * {@link Roi#getTPosition()}.
	 * 
	 * This is necessary, because the flag {@link ImagePlus#isHyperStack()} has
	 * weird behavior for edge cases.
	 * 
	 * @param from
	 *            ROI from which position is extracted
	 * @param to
	 *            ROI to which the position is transferred
	 */
	private void copyRoiPosition( Roi from, Roi to )
	{
		boolean positionIsZero = from.getPosition() == 0;
		boolean cIsZero = from.getCPosition() == 0;
		boolean zIsZero = from.getZPosition() == 0;
		boolean tIsZero = from.getTPosition() == 0;

		if ( positionIsZero )
		{
			if ( !cIsZero || !zIsZero || !tIsZero )
			{
				to.setPosition( from.getCPosition(), from.getZPosition(), from.getTPosition() );
			}
			else
			{
				// TODO ImageJ2: replace with LogService call
				IJ.log( "Cannot determine ROI position." );
			}
		}
		else
		{
			to.setPosition( from.getPosition() );
		}
	}

}
