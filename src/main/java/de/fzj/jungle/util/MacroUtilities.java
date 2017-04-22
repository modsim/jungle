package de.fzj.jungle.util;

import de.fzj.jungle.segmentation.Cell;
import ij.IJ;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

/**
 * Provides utility functions for macros.
 * 
 * @author Stefan Helfrich
 */
public class MacroUtilities {

	/**
	 * Can be called from an ImageJ macro to compute the name of the ROI that is
	 * associated with a {@link Cell}.
	 * 
	 * @param frame
	 *            Frame in which a cell has been detected
	 * @param frames
	 *            The overall number of frames in the processed image sequence
	 * @return String representation of the name for the currently processed
	 *         {@link Cell}.
	 */
	public static String getFrameString(int frame, int frames) {	
		int digits = String.valueOf(frames).length();
		
		String formatString = "F%0"+digits+"d-C%d";
		
		int id = Cell.getAndIncreaseCellIdCounter();
		if (id == 0) {
			// We are running in postprocessing mode
			RoiManager manager = RoiManager.getInstance();
			if (manager != null && manager.getRoisAsArray().length != 0) {
				// If the RoiManager is open we are using the count for generating new IDs
				Roi[] rois = manager.getRoisAsArray();
				id = Integer.parseInt(rois[rois.length-1].getName().split("-C")[1]);
				Cell.initCellIdCounter(id+1);
			} else {
				// If no RoiManager is found, try the overlay
				Overlay overlay = IJ.getImage().getOverlay();
				if (overlay != null) {
					Roi[] rois = overlay.toArray();
					/*
					 * The problem here is, that the last Roi in the overlay is
					 * very likely to be one that has been added manually.
					 * Hence, this cell can not be split with the regexp.
					 */
					try {
						id = Integer.parseInt(rois[rois.length-1].getName().split("-C")[1]);
					} catch (ArrayIndexOutOfBoundsException e) {
						/*
						 * Thus, let's try another educated guess: take the Roi
						 * in the middle and double the id.
						 */
						id = Integer.parseInt(rois[rois.length/2].getName().split("-C")[1]);
						id *= 2;
					}
					
					Cell.initCellIdCounter(id+1);
				}
			}
			
		}
		
		return String.format(formatString, frame, id);
	}
}
