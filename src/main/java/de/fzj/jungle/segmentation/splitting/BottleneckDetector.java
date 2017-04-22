package de.fzj.jungle.segmentation.splitting;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import de.fzj.jungle.segmentation.Cell;
import de.fzj.jungle.segmentation.Coordinate;
import de.fzj.jungle.segmentation.Orientation;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Implements a splitting mechanism for touching cells based on finding the
 * shortest distances between any two points on the cell contour.
 * 
 * @author Stefan Helfrich
 */
public class BottleneckDetector {
	
	/* Private fields */
	private Cell cell;
	private SplittingEnergy splittingEnergy;
	
	/**
	 * Constructs a {@link BottleneckDetector} for a {@link Cell} with a default
	 * {@link SplittingEnergy}.
	 * 
	 * @param cell
	 *            {@link Cell} to split
	 */
	public BottleneckDetector(Cell cell) {
		this.cell = cell;
		this.splittingEnergy = new BasicSplittingEnergy(cell.getPolygon());
	}
	
	/**
	 * Constructs a {@link BottleneckDetector} for a {@link Cell} using a
	 * provided {@link SplittingEnergy}.
	 * 
	 * @param cell
	 *            {@link Cell} to split
	 * @param energy
	 *            {@link SplittingEnergy} used to determine the best pair of
	 *            points used for splitting
	 */
	public BottleneckDetector(Cell cell, SplittingEnergy energy) {
		this.cell = cell;
		this.splittingEnergy = energy;
	}

	public Cell[] execute() throws UnexpectedException {
		// Find optimal assignment according to the defined {@link #BottleneckDetector.splittingEnergy splitting energy}.
		try {
			Assignment optimalAssignment = findBottleneck();
			Cell[] splitResult = splitCell(optimalAssignment);
			
			return splitResult;
		} catch (NoSuchElementException e) {
			return new Cell[]{this.cell};
		}
	}
	
	public Assignment findBottleneck() {
		/*
		 * Check if the ROI is of type composite / check for inner contours.
		 * 
		 * This case has to be handled separately because splitting energies
		 * that are based on contour segments are not applicable. This is due to
		 * the fact that a split between an outer and an inner contour does not
		 * result in two segments but in a cell containing a single (outer)
		 * contour.
		 * 
		 * Thus, we use a DistanceSplittingEnergy for computation of an energy
		 * for point-pairs with one point on the outer contour and one on the
		 * inner contour.
		 */
		// Rois might be Composite although they only consist of one Roi.
		if ((this.cell.getCellRoi().getType() == Roi.COMPOSITE) && (((ShapeRoi) this.cell.getCellRoi()).getRois().length > 1)) {
			ShapeRoi roi = (ShapeRoi) this.cell.getCellRoi();
			
			/*
			 * Sort ROIs returned by getRois() according to their perimeter. The
			 * ROI with the greatest perimeter is the outer contour.
			 */
			double highestPerimeter = 0.0;
			Roi outerContourRoi = null;
			
			Roi[] rois = roi.getRois();
			
			for (Roi r : rois) {
				double perimeter = r.getLength();
				if (perimeter > highestPerimeter) {
					outerContourRoi = r;
					highestPerimeter = perimeter;
				}
			}
			
			// Iterator for outer contour
			PathIterator polygonIter = outerContourRoi.getPolygon().getPathIterator(null);
			
			// Iterator for inner contour
			PathIterator polygonIter2;
			
			// Work with DistanceSplittingEnergy (see above)
			SplittingEnergy distanceEnergy = new DistanceSplittingEnergy();
			LinkedList<Assignment> energies = new LinkedList<Assignment>();
			
			while(!polygonIter.isDone()) {				
				polygonIter.next();
				
				double[] coords = new double[6];
				
				int segmentType = polygonIter.currentSegment(coords);
				
				Point A;
				
				switch (segmentType) {
				case PathIterator.SEG_MOVETO: 
				case PathIterator.SEG_LINETO:
					A = new Point((int) coords[0], (int) coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					continue;
				default:
					throw new IllegalArgumentException("Bad path segment");
				};
				
				for (Roi r : roi.getRois()) {
					// FIXME BUG!! This check does not yield the required result
					if (r.equals(outerContourRoi)) {
						continue;
					}
					
					// Iterator for inner contour
					polygonIter2 = r.getPolygon().getPathIterator(null);
						
					while(!polygonIter2.isDone()) {							
						polygonIter2.next();
						
						double[] coords2 = new double[6];
						
						int segmentType2 = polygonIter2.currentSegment(coords2);
						
						Point B;
						
						switch (segmentType2) {
						case PathIterator.SEG_MOVETO: 
						case PathIterator.SEG_LINETO:
							B = new Point((int) coords2[0], (int) coords2[1]);
							break;
						case PathIterator.SEG_CLOSE:
							continue;
						default:
							throw new IllegalArgumentException("Bad path segment");
						};
						
						double energy = distanceEnergy.computeEnergy(A, B);
						Assignment assig = new Assignment(A, B, energy, outerContourRoi.getPolygon(), r.getPolygon());
						
						// FIXME keep one assignment per inner contour to decrease the memory consumption
						energies.add(assig);
					}
				}
			}
			
			Collections.sort(energies);
			
			// Return computed assignment
			// TODO NoSuchElementException Ursache??
			return energies.getFirst();
		}
		
		// Iterate over contour and compare each pair of points
		// Take care of not comparing both, A-B and B-A		
		PathIterator polygonIter = this.cell.getPolygon().getPathIterator(null);
		PathIterator polygonIter2;
		
		LinkedList<Assignment> energies = new LinkedList<Assignment>();
		List<Boolean> flags = new ArrayList<Boolean>(Collections.nCopies(this.cell.getPolygon().npoints+1, false));
		
		ListIterator<Boolean> flagIter = flags.listIterator();
		
		Point previousPointA = null;
		
		while(!polygonIter.isDone()) {
			flagIter.next();
			flagIter.set(true);
			
			polygonIter.next();
			
			double[] coords = new double[6];
			
			int segmentType = polygonIter.currentSegment(coords);
			
			Point A;
			
			switch (segmentType) {
			case PathIterator.SEG_MOVETO: 
			case PathIterator.SEG_LINETO:
				A = new Point((int) coords[0], (int) coords[1]);
				break;
			case PathIterator.SEG_CLOSE:
				continue;
			default:
				throw new IllegalArgumentException("Bad path segment");
			};
			
			// FIXME concurrency problem?!
			Iterator<Boolean> flagIter2 = flags.iterator();
			
			polygonIter2 = this.cell.getPolygon().getPathIterator(null);
			
			Point previousPointB = null;
			
			while(!polygonIter2.isDone()) {							
				polygonIter2.next();
				
				if (flagIter2.next()) {
					continue;
				}
				
				double[] coords2 = new double[6];
				
				int segmentType2 = polygonIter2.currentSegment(coords2);
				
				Point B;
				
				switch (segmentType2) {
				case PathIterator.SEG_MOVETO: 
				case PathIterator.SEG_LINETO:
					B = new Point((int) coords2[0], (int) coords2[1]);
					break;
				case PathIterator.SEG_CLOSE:
					continue;
				default:
					throw new IllegalArgumentException("Bad path segment");
				};
				
				/*
				 * If A.equals(B) we have a crossing on a single outer contour.
				 * Allowing this results in an energy of 0, which most likely
				 * will be the optimal assignment. This is not expected
				 * behaviour!
				 */
				if (!A.equals(B)) {
					double energy = this.splittingEnergy.computeEnergy(A, previousPointA, B, previousPointB);
					Assignment assig = new Assignment(A, B, energy);
					
					energies.add(assig);
				}
				
				previousPointB = B;
			}
			
			previousPointA = A;
		}
		
		Collections.sort(energies);
		
		return energies.getFirst();
	}
	
	private Cell[] splitCell(Assignment assig) {
		/*
		 * A and B are on different contours.
		 */
		if ((cell.getCellRoi().getType() == Roi.COMPOSITE) && (((ShapeRoi) this.cell.getCellRoi()).getRois().length > 1)) {
			// iterate over outer contour
			Point A = assig.getA();
			Point B = assig.getB();
			
			Polygon outerContour = assig.getOuterContour();
			
			PathIterator outerIter = outerContour.getPathIterator(null);
			Polygon mergedContour = new Polygon();
			
			/*
			 * Upon hitting A:
			 * 1. reverse innerContour
			 * 2. Add connection A to B
			 * 3. Add all points after B
			 * 4. Add connection B-1 to A+1
			 * 5. Add all points after A+1
			 */
			while(!outerIter.isDone()) {
				outerIter.next();
				
				double[] coords = new double[6];
				
				int segmentType = outerIter.currentSegment(coords);
				
				switch (segmentType) {
				case PathIterator.SEG_MOVETO:	
				case PathIterator.SEG_LINETO:
					mergedContour.addPoint((int) coords[0], (int) coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					break;
				default:
					throw new IllegalArgumentException("Bad path segment");
				};
				
				if (A.x == coords[0] && A.y == coords[1]) {
					Polygon innerContour = assig.getInnerContour();
					innerContour = new Polygon(innerContour.xpoints, innerContour.ypoints, innerContour.npoints);
					
					// TODO check for rotation of contour
					if (computePolygonOrientation(outerContour) == computePolygonOrientation(innerContour)) {
						reversePolygonOrder(innerContour);
					}
					
					PathIterator innerIter = innerContour.getPathIterator(null);
					
					List<Point> innerTempList = new LinkedList<Point>();
					
					boolean trigger = false;
					
					while(!innerIter.isDone()) {
						innerIter.next();
						
						double[] innerCoords = new double[6];
						
						int innerSegmentType = innerIter.currentSegment(innerCoords);
						
						if (B.x == innerCoords[0] && B.y == innerCoords[1]) {
							// start adding points to mergedContour
							trigger = !trigger;
						}
						
						switch (innerSegmentType) {
						case PathIterator.SEG_MOVETO:	
						case PathIterator.SEG_LINETO:
							if (!trigger) {
								innerTempList.add(new Point((int) innerCoords[0], (int) innerCoords[1]));
							} else {
								mergedContour.addPoint((int) innerCoords[0], (int) innerCoords[1]);
							}
							break;
						case PathIterator.SEG_CLOSE:
							break;
						default:
							throw new IllegalArgumentException("Bad path segment");
						};
						
					}
					
					// finally, add points from innerTempList to mergedContour
					for (Point p : innerTempList) {
						mergedContour.addPoint(p.x, p.y);
					}
					
				}
			}
			
			// TODO create new cell that features mergedContour and the rest of the inner contours
			ShapeRoi cellRoi = (ShapeRoi) this.cell.getCellRoi();
			ShapeRoi newCellShapeRoi = new ShapeRoi(mergedContour);
			
			ShapeRoi assigOuterShapeRoi = new ShapeRoi(assig.getOuterContour());
			ShapeRoi assigInnerShapeRoi = new ShapeRoi(assig.getInnerContour());
			
			// mergedContour - other innerContours
			for (Roi r : cellRoi.getRois()) {
				ShapeRoi rShapeRoi = new ShapeRoi(r.getPolygon());	
				
				if (rShapeRoi.equals(assigInnerShapeRoi) || rShapeRoi.equals(assigOuterShapeRoi)) {
					continue;
				}
				
				newCellShapeRoi.not(new ShapeRoi(r));
			}
			
			Roi newCellRoi = newCellShapeRoi.shapeToRoi();
			Cell c;
			
			if (newCellRoi == null) {
				c = new Cell(newCellShapeRoi.getPolygon(), newCellShapeRoi, this.cell.getImageProcessor(), this.cell.getCalibration());
			} else {
				c = new Cell(newCellRoi.getPolygon(), newCellRoi, this.cell.getImageProcessor(), this.cell.getCalibration());
			}
			
			return new Cell[]{c};
		}
		
		/*
		 * Normal procedure if A and B are on one contour.
		 */
		Point A = assig.getA();
		Point B = assig.getB();

		Polygon polygon = this.cell.getPolygon();
		
		PathIterator iter = polygon.getPathIterator(null);
		Polygon path1 = new Polygon();
		Polygon path2 = new Polygon();
		
		Polygon currentPath = path1;
		boolean trigger = false;
		
		while (!iter.isDone()) {
			iter.next();
			
			double[] coords = new double[6];
			
			int segmentType = iter.currentSegment(coords);
			
			switch (segmentType) {
			case PathIterator.SEG_MOVETO:	
			case PathIterator.SEG_LINETO:
				currentPath.addPoint((int) coords[0], (int) coords[1]);
				break;
			case PathIterator.SEG_CLOSE:
				break;
			default:
				throw new IllegalArgumentException("Bad path segment");
			};
			
			// No assumptions about the order of ocurrence
			if (A.x == coords[0] && A.y == coords[1]) {
				if (!trigger) {
					/* 
					 * First time
					 */
					if (!A.equals(B)) {
						currentPath.addPoint((int) B.x, (int) B.y);
					}
					
					// switch currentPath
					currentPath = (currentPath == path1) ? path2 : path1;
					
					// newCurrentPath.moveTo(A) (or B)
					if (!A.equals(B)) {
						currentPath.addPoint((int) A.x, (int) A.y);
					}
					
					trigger = !trigger;
				} else {
					/* 
					 * Second time
					 */
					// switch currentPath
					currentPath = (currentPath == path1) ? path2 : path1;
				}
			}
			
			if (B.x == coords[0] && B.y == coords[1]) {
				if (!trigger) {
					/* 
					 * First time
					 */
					if (!A.equals(B)) {
						currentPath.addPoint((int) A.x, (int) A.y);
					}
					
					// switch currentPath
					currentPath = (currentPath == path1) ? path2 : path1;
					
					// newCurrentPath.moveTo(A) (or B)
					if (!A.equals(B)) {
						currentPath.addPoint((int) B.x, (int) B.y);
					}
					
					trigger = !trigger;
				} else {
					/* 
					 * Second time
					 */
					// switch currentPath
					currentPath = (currentPath == path1) ? path2 : path1;
				}
			}
			
		}
		
		Cell cellAB = new Cell(path1, new PolygonRoi(path1, PolygonRoi.POLYGON), this.cell.getImageProcessor(), this.cell.getCalibration());
		Cell cellBA = new Cell(path2, new PolygonRoi(path2, PolygonRoi.POLYGON), this.cell.getImageProcessor(), this.cell.getCalibration());
		
		return new Cell[]{cellAB, cellBA};
	}
	
	static Orientation computePolygonOrientation(Polygon p) {		
		PathIterator iter = p.getPathIterator(null);
			
		Point A = null;
		Point B = null;
		Point C = null;
		
		int counter = 0;
		
		while(!iter.isDone()) {
			iter.next();
			
			double[] coords = new double[6];
			
			int segmentType = iter.currentSegment(coords);
			
			switch (segmentType) {
			case PathIterator.SEG_MOVETO:	
			case PathIterator.SEG_LINETO:
				// Set first two points of contour as A and B
				switch(counter++) {
				case 0:
					A = new Point((int) coords[0], (int) coords[1]);
					break;
				case 1:
					B = new Point((int) coords[0], (int) coords[1]);
					break;
				default:
					C = new Point((int) coords[0], (int) coords[1]);
					
					Line2D lineAC = new Line2D.Float(A, C);
					switch (lineAC.relativeCCW(B)) {
					case 1:
						return Orientation.CLOCKWISE;
					case -1:
						return Orientation.COUNTERCLOCKWISE;
					case 0:
						// "Note that an indicator value of 0 is rare and not useful for determining colinearity because of floating point rounding issues."
						// http://docs.oracle.com/javase/7/docs/api/java/awt/geom/Line2D.html#relativeCCW(java.awt.geom.Point2D)
						// => continue iterating the polygon until we find an orientation..
						break;
					};	
				};
				break;
			case PathIterator.SEG_CLOSE:
				break;
			default:
				throw new IllegalArgumentException("Bad path segment");
			};
		}
		
		return null;
	}

	private List<Coordinate> connectCoordinatesWithLine(Coordinate A, Coordinate B) {
		List<Coordinate> tempList = new LinkedList<Coordinate>();
		
		int deltaX = Math.abs(A.getX() - B.getX()) + 1;
		int deltaY = Math.abs(A.getY() - B.getY()) + 1;
		
		int shiftX = Math.min(A.getX(), B.getX());
		int shiftY = Math.min(A.getY(), B.getY());
		
		ImageProcessor ip = new FloatProcessor(deltaX, deltaY);
		ip.drawLine(A.getX() - shiftX, A.getY() - shiftY, B.getX() - shiftX, B.getY() - shiftY);
		
		float[][] pixels = ip.getFloatArray();
		
//		new ImagePlus("Labeled image", ip).show();
		
		for (int i=0; i < pixels.length; ++i) {
			for (int j=0; j < pixels[i].length; ++j) {
				if (pixels[i][j] != 0) {
					Coordinate temp = new Coordinate((short) (i+shiftX), (short) (j+shiftY));
					if (!tempList.contains(temp)) {
						tempList.add(temp);
					}
				}
			}
		}
		
		return tempList;
	}
	
	@Deprecated
	private void computeSplit(Cell cell, Point A, Point B) {
		
	}
	
	void reversePolygonOrder(Polygon p) {
		int[] x = p.xpoints;
		int[] y = p.ypoints;
		
		// x and y may contain more the <npoints> points 
		int n = p.npoints;
		
		// In-place swap
		int tmp;
		for (int i = 0; i < n/2; i++) {
			tmp = x[i];
			x[i] = x[n-1-i];
			x[n-1-i] = tmp;
			
			tmp = y[i];
			y[i] = y[n-1-i];
			y[n-1-i] = tmp;
		}
	}
	
	private class Assignment implements Comparable<Assignment> {
		
		/* Private fields */
		private Point A;
		private Point B;
		private double energy;
		private Polygon outerContour;
		private Polygon innerContour;
		
		/**
		 * @param a
		 * @param b
		 * @param energy
		 */
		@Deprecated
		public Assignment(Point a, Point b, double energy) {
			this(a, b, energy, null, null);
		}
		
		/**
		 * @param a
		 * @param b
		 * @param energy
		 * @param outerContour
		 */
		public Assignment(Point a, Point b, double energy, Polygon outerContour) {
			this(a, b, energy, outerContour, null);
		}
		
		/**
		 * @param a
		 * @param b
		 * @param energy
		 * @param outerContour
		 * @param innerContour
		 */
		public Assignment(Point a, Point b, double energy, Polygon outerContour, Polygon innerContour) {
			super();
			this.A = a;
			this.B = b;
			this.energy = energy;
			this.outerContour = outerContour;
			this.innerContour = innerContour;
		}

		/**
		 * @return the a
		 */
		public Point getA() {
			return A;
		}

		/**
		 * @param a the a to set
		 */
		public void setA(Point a) {
			A = a;
		}

		/**
		 * @return the b
		 */
		public Point getB() {
			return B;
		}

		/**
		 * @param b the b to set
		 */
		public void setB(Point b) {
			B = b;
		}

		/**
		 * @return the distance
		 */
		public double getEnergy() {
			return energy;
		}

		/**
		 * @param distance the distance to set
		 */
		public void setEnergy(double energy) {
			this.energy = energy;
		}
		
		/**
		 * @return the outerContour
		 */
		public Polygon getOuterContour() {
			return outerContour;
		}

		/**
		 * @param outerContour the outerContour to set
		 */
		public void setOuterContour(Polygon outerContour) {
			this.outerContour = outerContour;
		}

		/**
		 * @return the innerContour
		 */
		public Polygon getInnerContour() {
			return innerContour;
		}

		/**
		 * @param innerContour the innerContour to set
		 */
		public void setInnerContour(Polygon innerContour) {
			this.innerContour = innerContour;
		}

		@Override
		public int compareTo(Assignment o) {
			return ((Double)this.getEnergy()).compareTo(o.getEnergy());
		}
	}
	
}
