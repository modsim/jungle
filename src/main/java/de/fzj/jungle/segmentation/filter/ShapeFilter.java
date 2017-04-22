/**
 * 
 */
package de.fzj.jungle.segmentation.filter;

import ij.process.ImageProcessor;

import java.util.Collection;

import de.fzj.jungle.segmentation.Cell;

/**
 * This filter works by assuming or deriving a standard cell shape to which
 * other cells are compared. This basically is a project for itself since it
 * needs a whole lot of prerequisites to be successful at all. There is
 * currently no release planned for the future.
 * 
 * @author Stefan Helfrich
 */
public class ShapeFilter implements Filter<Cell> {

	@Override
	public Filter.Result<Cell> filter(Collection<Cell> list, ImageProcessor ip) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

}
