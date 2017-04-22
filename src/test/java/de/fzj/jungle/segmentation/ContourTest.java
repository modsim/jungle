/**
 * 
 */
package de.fzj.jungle.segmentation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import de.fzj.jungle.segmentation.Contour;
import de.fzj.jungle.segmentation.Coordinate;

/**
 * @author Stefan Helfrich <s.helfrich@fz-juelich.de>
 * @version 0.1
 * 
 */
public class ContourTest {

	Contour contour;
	ArrayList<Coordinate> list;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.list = new ArrayList<Coordinate>();
		for (int i = 0; i < 5; ++i) {
			this.list.add(new Coordinate((short) i, (short) i, (short) i));
		}

		this.contour = new Contour(this.list);
	}

	/**
	 * Test method for {@link de.fzj.jungle.segmentation.Contour#get(int)}.
	 */
	@Test
	public void testGetStandard() {
		// Normal cases
		for (int i = 0; i < this.list.size(); ++i) {
			assert (this.contour.get(i) == list.get(i));
		}
	}

	/**
	 * Test method for {@link de.fzj.jungle.segmentation.Contour#get(int)}.
	 */
	@Test
	public void testGetNegative() {
		// Negative cases
		for (int i = 0; i < 3; ++i) {
			assert (this.contour.get(-i * list.size() - 1) == list.get(list
					.size() - 1));
		}
	}

	/**
	 * Test method for {@link de.fzj.jungle.segmentation.Contour#get(int)}.
	 */
	@Test
	public void testGetPositive() {
		// Positive cases
		for (int i = 0; i < 3; ++i) {
			assert (this.contour.get(i * list.size()) == list.get(0));
		}
	}

	/**
	 * Test method for
	 * {@link de.fzj.jungle.segmentation.Contour#getNormalAngle(de.fzj.jungle.segmentation.Coordinate)}
	 * .
	 */
	@Test
	public void testGetNormalAngle() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link de.fzj.jungle.segmentation.Contour#calculateAngleToXAxis(de.fzj.jungle.segmentation.Coordinate, de.fzj.jungle.segmentation.Coordinate)}
	 * .
	 */
	@Test
	public void testCalculateAngleToXAxis() {
		fail("Not yet implemented"); // TODO
	}
	
	/**
	 * Test method for
	 * {@link de.fzj.jungle.segmentation.Contour#iterator(de.fzj.jungle.segmentation.Coordinate, de.fzj.jungle.segmentation.Coordinate)}
	 * .
	 */
	@Test
	public void testIterator1() {
		Coordinate A = list.get(0);
		Coordinate B = list.get(3);
		
		Iterator iter = this.contour.iterator(A, B);
		assert(iter.hasNext());
		assert(iter.next() == list.get(0));
		assert(iter.next() == list.get(1));
		assert(iter.next() == list.get(2));
		assert(!iter.hasNext());
		
		Iterator iter2 = this.contour.iterator(B, A);
		assert(iter2.hasNext());
		assert(iter2.next() == list.get(3));
		assert(iter2.next() == list.get(4));
		assert(!iter2.hasNext());
	}

	/**
	 * Test method for
	 * {@link de.fzj.jungle.segmentation.Contour#iterator(de.fzj.jungle.segmentation.Coordinate, de.fzj.jungle.segmentation.Coordinate)}
	 * .
	 */
	@Test
	public void testIterator2() {
		Coordinate A = list.get(0);
		Coordinate B = list.get(1);
		
		Iterator iter = this.contour.iterator(A, B);
		assert(iter.hasNext());
		assert(iter.next() == list.get(0));
		assert(!iter.hasNext());
		
		Iterator iter2 = this.contour.iterator(B, A);
		assert(iter2.hasNext());
		assert(iter2.next() == list.get(1));
		assert(iter2.next() == list.get(2));
		assert(iter2.next() == list.get(3));
		assert(iter2.next() == list.get(4));
		assert(!iter2.hasNext());
	}
	
}
