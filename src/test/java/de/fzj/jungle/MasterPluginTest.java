package de.fzj.jungle;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.PortableInterceptor.SUCCESSFUL;

import de.fzj.jungle.MasterPlugin;

public class MasterPluginTest {

	MasterPlugin masterPlugin;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		masterPlugin = new MasterPlugin();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testParseMacroOptionsSimple() {
		Map<String, Object> optionsMap = masterPlugin.parseMacroOptions("preprocessing segmentation");
		
		assertTrue(optionsMap.containsKey("executePreprocessing") && optionsMap.containsKey("executeSegmentation"));
		assertTrue((Boolean) optionsMap.get("executePreprocessing"));
		assertTrue((Boolean) optionsMap.get("executeSegmentation"));
	}

	@Test
	public void testParseMacroOptionsSizeFilter() {
		Map<String, Object> optionsMap = masterPlugin.parseMacroOptions("preprocessing segmentation filter=size");
		
		assertTrue(optionsMap.containsKey("filterMethod"));
		assertTrue((Boolean) optionsMap.get("filterMethod").equals("Size Filter"));
	}
	
	@Test
	public void testParseMacroOptionsConvexHullFilter() {
		Map<String, Object> optionsMap = masterPlugin.parseMacroOptions("preprocessing segmentation filter=convexhull");
		
		assertTrue(optionsMap.containsKey("filterMethod"));		
		assertTrue((Boolean) optionsMap.get("filterMethod").equals("Convex Hull Filter"));
	}
	
	@Test
	public void testParseMacroOptionsSizeFilterValues() {
		Map<String, Object> optionsMap = masterPlugin.parseMacroOptions("preprocessing segmentation filter=size minimalsize=100 maximalsize=200 backgroundsize=5000");
		
		assertTrue(optionsMap.containsKey("minimalSize"));		
		assertTrue((Integer) optionsMap.get("minimalSize") == 100);
		
		assertTrue(optionsMap.containsKey("maximalSize"));		
		assertTrue((Integer) optionsMap.get("maximalSize") == 200);
		
		assertTrue(optionsMap.containsKey("backgroundSize"));		
		assertTrue((Integer) optionsMap.get("backgroundSize") == 5000);
	}
	
	@Test
	public void testParseMacroOptionsConvexHullFilterValues() {
		Map<String, Object> optionsMap = masterPlugin.parseMacroOptions("preprocessing segmentation filter=convexhull deviation=0.25");
		
		assertTrue(optionsMap.containsKey("deviationConvexHull"));		
		assertTrue((Double) optionsMap.get("deviationConvexHull") == 0.25);
	}
	
	@Test
	public void testParseMacroOptionsDebug() {
		Map<String, Object> optionsMap = masterPlugin.parseMacroOptions("preprocessing segmentation thresholdingdebug segmentationdebug filter=convexhull deviation=0.25");
		
		assertTrue(optionsMap.containsKey("thresholdingDebug"));		
		assertTrue((Boolean) optionsMap.get("thresholdingDebug"));
		
		assertTrue(optionsMap.containsKey("segmentationDebug"));		
		assertTrue((Boolean) optionsMap.get("segmentationDebug"));
	}
}
