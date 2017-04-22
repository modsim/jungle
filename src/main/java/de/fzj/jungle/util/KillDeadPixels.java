package de.fzj.jungle.util;

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Removes ROIs from an image's overlay that are too small and are, thus, not
 * rendered on the image which makes them unselectable.
 * 
 * @author Stefan Helfrich
 */
public class KillDeadPixels implements PlugInFilter {

	ImagePlus imp;
	
	@Override
	public void run(ImageProcessor arg0) {
		Overlay o = imp.getOverlay();
		for (Roi r : o.toArray()) {
			Rectangle rect = r.getBounds();
			double width = rect.getWidth();
			double height = rect.getHeight();
			
			if (width == 0.0d || height == 0.0d) {
				o.remove(r);
			}
		}
	}

	@Override
	public int setup(String args, ImagePlus imp) {
		this.imp = imp;
		
		return DOES_ALL;
	}

}
