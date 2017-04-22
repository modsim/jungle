package de.fzj.jungle.preprocessing.hough;
import ij.IJ;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This sample code is made available as part of the book "Digital Image
 * Processing - An Algorithmic Introduction using Java" by Wilhelm Burger
 * and Mark J. Burge, Copyright (C) 2005-2008 Springer-Verlag Berlin, 
 * Heidelberg, New York.
 * Note that this code comes with absolutely no warranty of any kind.
 * See http://www.imagingbook.com for details and licensing conditions.
 * 
 * Date: 2010-07-24
 */
public class LinearHT {
	ImageProcessor ip;		// reference to original image
	int uc, vc; 			// x/y-coordinate of image center
	double rMax;			// maximum radius
	int nAng;				// number of steps for the angle  (a = 0 ... PI)
	int nRad; 				// number of steps for the radius (r = -r_max ... +r_max)
	int cRad;				// array index for zero radius (r = 0)
	double dAng;			// increment of angle
	double dRad; 			// increment of radius
	int[][] houghArray; 	// Hough accumulator array
	int[][] localMaxArray;	// array of accumulator local maxima

	// --------------  public methods ------------------------
	
	public LinearHT(ImageProcessor ip, int nAng, int nRad) {
		this.ip = ip;
		this.uc = ip.getWidth()/2; 
		this.vc = ip.getHeight()/2;
		this.nAng = nAng; 
		this.dAng = Math.PI / nAng;
		this.nRad = nRad;
		this.cRad = nRad / 2;
		this.rMax = Math.sqrt(uc * uc + vc * vc);
		this.dRad = (2.0 * rMax) / nRad;
		this.houghArray = new int[nAng][nRad]; // cells are initialized to zero
		fillHoughArray();
		findLocalMaxima();
	}
	
	/* 
	 * Find and return the parameters of the n 
	 * strongest lines (with max. pixel counts)
	 */
	public List<HoughLine> getMaxLines(int maxCnt, int minPts) {
		HoughLine[] linArr = new HoughLine[maxCnt];
		// create an array of n blank HoughLine objects
		for (int i = 0; i < linArr.length; i++) {
			linArr[i] = new HoughLine(0, 0, -1);
		}

		for (int ri = 0; ri < nRad; ri++) {
			for (int ai = 0; ai < nAng; ai++) {
				int hcount = localMaxArray[ai][ri];
				if (hcount >= minPts) {
					HoughLine last = linArr[linArr.length - 1];
					if (hcount > last.count) {
						last.angle = realAngle(ai);
						last.radius = realRadius(ri);
						last.count = hcount;
						Arrays.sort(linArr);	// this may be more efficient with insert sort
					}
				}
			}
		}
		
		List<HoughLine> lineList = new ArrayList<>();
		for (HoughLine hl : linArr) {
			if (hl.getCount() < minPts) break;
			lineList.add(hl);
		}
		return lineList;
		//return Arrays.asList(lines);
	}
	
	public FloatProcessor getAccumulatorImage() {
		if (houghArray == null)
			throw new Error("houghArray is not initialized");
		FloatProcessor fp = new FloatProcessor(nAng,nRad);
		for (int ri = 0; ri < nRad; ri++) {
			for (int ai = 0; ai < nAng; ai++) {
				fp.setf(ai, ri, houghArray[ai][ri]);
			}
		}
		fp.resetMinAndMax();
		return fp;
	}
	
	public FloatProcessor getLocalMaxImage() {
		assert localMaxArray != null;
		FloatProcessor fp = new FloatProcessor(nAng,nRad);
		for (int ri = 0; ri < nRad; ri++) {
			for (int ai = 0; ai < nAng; ai++) {
				fp.setf(ai, ri, localMaxArray[ai][ri]);
			}
		}
		fp.resetMinAndMax();
		return fp;
	}
	

	
	// --------------  nonpublic methods ------------------------
	
	void fillHoughArray() {
		IJ.showStatus("filling accumulator ...");
		int h = ip.getHeight();
		int w = ip.getWidth();
		for (int v = 0; v < h; v++) {
			IJ.showProgress(v, h);
			for (int u = 0; u < w; u++) {
				if (ip.get(u, v) > 0) {		// this is an edge pixel
					doPixel(u, v);
				}
			}
		}
		IJ.showProgress(1, 1);
	}

	void doPixel(int u, int v) {
		int x = u - uc;
		int y = v - vc;
		for (int ai = 0; ai < nAng; ai++) {
			double theta = dAng * ai;
			double r = x * Math.cos(theta) + y * Math.sin(theta);
			int ri =  cRad + (int) Math.rint(r / dRad);
			if (ri >= 0 && ri < nRad) {
				houghArray[ai][ri]++;
			}
		}
	}
	
	void findLocalMaxima() {
		IJ.showStatus("finding local maxima");
		localMaxArray = new int[nAng][nRad]; //initialized to zero
		for (int ai = 0; ai < nAng; ai++) {
			// angle dimension is treated cyclically:
			int a1 = (ai > 0) ? ai-1 : nAng-1;
			int a2 = (ai < nAng-1) ? ai+1 : 0;
			for (int ri = 1; ri < nRad - 1; ri++) {
				int ha = houghArray[ai][ri];
				// this test is problematic if 2 identical cell values 
				// appear next to each other!
				boolean ismax =
					ha > houghArray[a1][ri-1] &&
					ha > houghArray[a1][ri] &&
					ha > houghArray[a1][ri+1] &&
					ha > houghArray[ai][ri-1] &&
					ha > houghArray[ai][ri+1] &&
					ha > houghArray[a2][ri-1] &&
					ha > houghArray[a2][ri] &&
					ha > houghArray[a2][ri+1] ;
				if (ismax)
					localMaxArray[ai][ri] = ha;
			}
		}
	}
	
	//returns real angle for angle index ai
	double realAngle(int ai) {	
		return ai * dAng;
	}
	
	//returns real radius for radius index ri (with respect to image center <uc,vc>)
	double realRadius(int ri) {	
		return (ri - cRad) * dRad;
	}
	

	/*
	 * This class represents a straight line in Hessian normal form.
	 */
	public class HoughLine implements Comparable<HoughLine> {
		// must be comparable for sorting (by count)
		private double angle;	// the orientation angle of this line
		private double radius;	// the radius (perpendicular distance from image center <uc,vc>)
		private int count;		//number of contributing image points
		
		// no public constructor
		private HoughLine(double angle, double radius, int count){
			this.angle  = angle;	
			this.radius = radius;	
			this.count  = count;	
		}
		
		public double getAngle() {
			return angle;
		}
		
		public double getRadius() {
			return radius;
		}
		
		public int getCount() {
			return count;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(angle);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + count;
			temp = Double.doubleToLongBits(radius);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			HoughLine h2;
			
			if (obj instanceof HoughLine) {
				h2 = (HoughLine) obj;
				
				if (Math.abs(this.getRadius()) == Math.abs(h2.getRadius()) && (this.getAngle() % Math.PI == h2.getAngle() % Math.PI)) {
					return true;
				}
			}
			
			return false;
		}

		@Override
		public int compareTo (HoughLine hl){
			HoughLine hl1 = this;
			HoughLine hl2 = hl;
			if (hl1.count > hl2.count)
				return -1;
			else if (hl1.count < hl2.count)
				return 1;
			else
				return 0;
		}
		
		@Override
		public String toString() {
			return String.format("%s <angle=%.3f, radius=%.3f, count=%d>", 
					HoughLine.class.getSimpleName(), angle, radius, count);
		}
		
		// returns an equivalent Line2D.Double object (to be drawn on a canvas)
		// with coordinates relative to the image origin
		public Line2D.Double makeLine2D() {
			double length = rMax/2;
			double dx = radius * Math.cos(angle);
			double dy = radius * Math.sin(angle);
			double xs = uc + dx;
			double ys = vc + dy;
			double x1 = xs - (dy * length);
			double x2 = xs + (dy * length);
			double y1 = ys + (dx * length);
			double y2 = ys - (dx * length);
			return new Line2D.Double(x1, y1, x2, y2);
		}
		
		// find point of intersection for one pair of lines,
		// returns point in image coordinates
		public Point2D.Double intersectWith (HoughLine hl2) {
			HoughLine hl1 = this;
			double th1 = hl1.getAngle();
			double th2 = hl2.getAngle();
			if (th1 == th2)	return null;
			double r1  = hl1.getRadius();
			double r2  = hl2.getRadius();
			double s = 1 / Math.sin(th2 - th1);
			double x = uc + s * (r1 * Math.sin(th2) - r2 * Math.sin(th1));
			double y = vc + s * (r2 * Math.cos(th1) - r1 * Math.cos(th2));
			return new Point2D.Double(x,y);
		}

		private LinearHT getOuterType() {
			return LinearHT.this;
		}
	
	} // end of class HoughLine
	
	public static class HoughLinePair implements Comparable<HoughLinePair> {

		private HoughLine h1;
		private HoughLine h2;
		private int sumOfScores;
		
		@Override
		public int compareTo(HoughLinePair p) {
			if (this.getSumOfScores() < p.getSumOfScores()) {
				return -1;
			}
			if (this.getSumOfScores() == p.getSumOfScores()) {
				return 0;
			} 
			if (this.getSumOfScores() > p.getSumOfScores()) {
				return 1;
			}
			
			return 0;
		}
		
		public HoughLinePair(HoughLine h1, HoughLine h2, int sumOfScores) {
			this.h1 = h1;
			this.h2 = h2;
			this.sumOfScores = sumOfScores;
		}

		
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((h1 == null) ? 0 : h1.hashCode());
			result = prime * result + ((h2 == null) ? 0 : h2.hashCode());
			result = prime * result + sumOfScores;
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			HoughLinePair otherHoughLinePair;
			
			if (obj instanceof HoughLinePair) {
				otherHoughLinePair = (HoughLinePair) obj;
				
				if (this.getH1().equals(otherHoughLinePair.getH1())) {
					if (this.getH2().equals(otherHoughLinePair.getH2())) {
						if (this.getSumOfScores() == otherHoughLinePair.getSumOfScores()) {
							return true;
						}
					}
				}
			}
			
			return false;
		}

		/**
		 * @return the h1
		 */
		public HoughLine getH1() {
			return h1;
		}

		/**
		 * @return the h2
		 */
		public HoughLine getH2() {
			return h2;
		}

		/**
		 * @return the sumOfScores
		 */
		public int getSumOfScores() {
			return sumOfScores;
		}
		
		public int getDistance() {
			double r1 = Math.abs(getH1().getRadius());
			double r2 = Math.abs(getH2().getRadius());
			return (int) Math.round(Math.abs(r1 + r2));
		}
	}
} // end of class LinearHT






