package de.fzj.jungle.skeletonize;

import java.util.List;

import de.fzj.jungle.segmentation.Contour;
import de.fzj.jungle.segmentation.Coordinate;
import de.fzj.jungle.segmentation.Direction;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Point;
import sc.fiji.analyzeSkeleton.SkeletonResult;

/**
 * WIP: compute length from skeleton of object.
 * 
 * @author Stefan Helfrich
 */
public class LengthCalculator implements PlugInFilter {
	
	private SkeletonResult skeletonResult;
	private AnalyzeSkeleton_ proc;
	private List<Point> skeletonPoints;
	private List<Point> elongatedPoints;
	private List<Point> endPoints;
	private Contour contour;
	
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		proc = new AnalyzeSkeleton_();
		proc.setup(arg, imp);
		
		return DOES_8G;
	}

	@Override
	public void run(ImageProcessor ip) {	
		ImagePlus imgPlus = new ImagePlus("Test", ip);
		
		skeletonResult = proc.run(AnalyzeSkeleton_.NONE, false, false, imgPlus, true, false);
		
		List<Point> endpoints = skeletonResult.getListOfEndPoints();
		skeletonPoints = skeletonResult.getListOfSlabVoxels();
		
//		Graph g = skeletonResult.getGraph()[0];
//		Vertex oldRoot = g.getRoot();
//		
//		Vertex root = g.getRoot();
//		List<Point> vertexPoints = root.getPoints();
//		
//		List<Edge> edges = g.getEdges();
		
		for (Point p : endpoints) {
			Point neighbour = getNeighbour(p);
			
			Direction elongationDirection = computeElongationDirection(p, neighbour);
			
			elongateSkeleton(p, elongationDirection);
		}
	}
	
	private Point getNeighbour(Point p) {
		// check for neighbours in 8-neighbourhood
		for (Direction dir : Direction.values()) {
			// equals() is overwritten, thus enabling this approach
			Point proposedPoint = new Point(p.x + dir.getDx(), p.y + dir.getDy(), 0);
			
			if (skeletonPoints.contains(proposedPoint)) {
				return proposedPoint;
			}
		}
		
		// if no neighbour is found, return null
		return null;
	}
	
	private Direction computeElongationDirection(Point p1, Point p2) {
		Direction dirP1toP2 = Direction.getDirection(p1.x - p2.x, p1.y - p2.y);
		
		return dirP1toP2.getOpposite();
	}
	
	private void elongateSkeleton(Point p, Direction dir) {
		// if p+dir is contour point return
		Point newPoint = new Point(p.x + dir.getDx(), p.y + dir.getDy(), 0);
		
		if (contour.contains(convertPointToCoordinate(newPoint))) {
			endPoints.add(newPoint);
			return;
		}
		// add newPoint to elongatedPoints
		elongatedPoints.add(newPoint);
		
		// recursively call elongateSkeleton
		elongateSkeleton(newPoint, dir);
	}
	
	private Coordinate convertPointToCoordinate(Point p) {
		return new Coordinate((short) p.x, (short) p.y);
	}
	
	// TODO Test!
	private double computeCellLength() {
		// Start at endPoints.get(0)
		Point startPoint = endPoints.get(0);
		Point endPoint = endPoints.get(endPoints.size()-1);
		
		return .0;
	}
	
//	private void sortElongatedPoints(Point startPoint) {
//		startPoint.
//	}
}
