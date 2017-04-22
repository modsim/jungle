package de.fzj.jungle.util;

import de.fzj.jungle.segmentation.Cell;
import de.fzj.jungle.segmentation.Coordinate;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Draws a {@link Cell} on a new {@link ImageProcessor}.
 * 
 * @author Stefan Helfrich
 */
public class CustomImageConverter
{

	static int FOREGROUND = 0;

	static int BACKGROUND = 255;

	/**
	 * Draws a {@link Cell} on a new {@link ImageProcessor}.
	 * 
	 * @param cell
	 *            {@link Cell} to be drawn
	 * @param width
	 *            width of the {@link ImageProcessor}
	 * @param height
	 *            width of the {@link ImageProcessor}
	 * @return an {@link ImageProcessor} instance that has the {@link Cell}'s
	 *         content drawn in {@link #FOREGROUND} color.
	 */
	@SuppressWarnings( "deprecation" )
	public static ImageProcessor cellToImageProcessor( Cell cell, int width, int height )
	{
		ByteProcessor imgProc = new ByteProcessor( width, height );

		imgProc.setColor( CustomImageConverter.BACKGROUND );
		imgProc.fill();

		for ( Coordinate c : cell.getInside() )
		{
			imgProc.putPixel( c.getX(), c.getY(), CustomImageConverter.FOREGROUND );
		}

		return imgProc;
	}
}
