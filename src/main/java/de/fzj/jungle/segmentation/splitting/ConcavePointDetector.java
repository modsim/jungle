/**
 * 
 */
package de.fzj.jungle.segmentation.splitting;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import de.fzj.jungle.segmentation.Contour;
import de.fzj.jungle.segmentation.Coordinate;

/**
 * Implements the detection of splitting points based on concavity of the cell
 * contour.
 * 
 * @author Stefan Helfrich
 */
public class ConcavePointDetector {

	private Contour contour;
	private List<Coordinate> bottlenecks;
	private Contour polygonApproximatedContour;
	private double THRESHOLD = 0.7;

	/**
	 * Constructor.
	 * 
	 * @param contour
	 *            The contour of a cluster of cells that has to be split
	 * @param bottlenecks
	 *            The determined bottlenecks for the contour
	 */
	public ConcavePointDetector(Contour contour, List<Coordinate> bottlenecks) {
		this.contour = contour;
		this.bottlenecks = bottlenecks;
	}

	/**
	 * Standard constructor.
	 */
	public ConcavePointDetector() {
		this.contour = new Contour();
		this.bottlenecks = new ArrayList<Coordinate>();
	}

	public void execute() {
		approximatePolygon(2, 3.5);
		detectCavity(.0, .5);
	}

	/**
	 * This function implements the approximation of the contour. This
	 * implementation follows the algorithm of Section 3.1 in
	 * "Splitting touching cells based on concave points and ellipse fitting"
	 * (Bai et al., Pattern Recognition, 2009). For more details see publication
	 * 
	 * @param nStep
	 *            Number of points between p_i and p_j (> 1)
	 * @param dTh
	 *            Threshold of distance between approx. line and a point
	 *            (usually between 3 and 4)
	 */
	private void approximatePolygon(int nStep, double dTh) {
		// Determine coordinates p_i and p_j
		int i = 0;
		int j = i + nStep;
		Coordinate p_i = this.contour.get(i);
		Coordinate p_j = this.contour.get(j);

		// Repeat procedure until p_i has visited all points on contour C.
		while (i < this.contour.size()) {
			// Generate line between p_i and p_j
			Line2D.Double line = new Line2D.Double(p_i.getX(), p_i.getY(),
					p_j.getX(), p_j.getY());

			// Calculate distances of points p_i, p_i+1, ... , p_j to line and
			// compare with dTh. If distance of point p_t exceeds threshold set
			// p_i = p_t and continue. If the threshold is not exceeded set
			// p_j++.
			for (int t = i + 1; t < j; t++) {
				Coordinate p_t = this.contour.get(t);
				double dist = line.ptLineDist(p_t.getX(), p_t.getY());

				if (dist >= dTh) {
					polygonApproximatedContour.add(p_t);
					p_i = p_t;
					i = t;

					// Re-initialization of p_j at this point?
					break;
				}
			}

			++j;
			p_j = this.contour.get(j);
		}
	}

	/**
	 * Extracts concave points based on Section 3.2 in
	 * "Splitting touching cells based on concave points and ellipse fitting"
	 * (Bai et al., Pattern Recognition, 2009). Stores the points in a local
	 * list of bottlenecks.
	 * 
	 * @param a1
	 *            Lower threshold of concavity range
	 * @param a2
	 *            Upper threshold of concavity range
	 */
	private void detectCavity(double a1, double a2) {
		// Iterate over contour. Negative indices and indices >= size() are
		// handled by Contour implementation.
		for (int i = 0; i < this.polygonApproximatedContour.size(); ++i) {
			Coordinate p_pre = this.polygonApproximatedContour.get(i - 1);
			Coordinate p_c = this.polygonApproximatedContour.get(i);
			Coordinate p_next = this.polygonApproximatedContour.get(i + 1);

			// Calculate angle difference / concavity.
			double concavity = computeConcavity(p_pre, p_c, p_next);

			// If difference is in range (a1, a2)
			if (a1 < concavity && concavity < a2) {
				// Create deep copy because intersect() changes the object
				Area enclosedArea = (Area) this.contour.getEnclosedArea()
						.clone();

				// Create line between p_pre and p_next
				Line2D.Double line = new Line2D.Double(p_pre.getX(),
						p_pre.getY(), p_next.getX(), p_next.getY());

				// Calculate intersection of the line and the enclosed area
				enclosedArea.intersect(new Area(line));

				// Check if line passes through inside of cell
				if (!enclosedArea.isEmpty()) {
					// p_c is considered a possible concave point
					this.bottlenecks.add(p_c);
				}
			}
		}

		// TODO Handle different cases proposed in the publication
	}

	/**
	 * The function implements the computation of the concavity measure proposed
	 * in Section 3.2 of
	 * "Splitting touching cells based on concave points and ellipse fitting"
	 * (Bai et al., Pattern Recognition, 2009). For more details see
	 * publication.
	 * 
	 * @param p_pre
	 *            Previous point on the contour
	 * @param p_c
	 *            Point on the contour for which the concavity is to be computed
	 * @param p_next
	 *            Next point on the contour
	 * @return Concavity measure
	 */
	private double computeConcavity(Coordinate p_pre, Coordinate p_c,
			Coordinate p_next) {
		double anglePreCurrent = this.polygonApproximatedContour
				.calculateAngleToXAxis(p_pre, p_c);
		double angleCurrentNext = this.polygonApproximatedContour
				.calculateAngleToXAxis(p_c, p_next);

		double absoluteDifference = Math
				.abs(anglePreCurrent - angleCurrentNext);

		return (absoluteDifference < Math.PI) ? absoluteDifference
				: (Math.PI - absoluteDifference);
	}
}
