#!/usr/bin/env python
from __future__ import with_statement

from ij import IJ, ImagePlus
from ij.io import DirectoryChooser

from java.lang import Runtime, Runnable
from java.io import File

import os
import sys
import csv

from fiji.plugin.trackmate import Model
from fiji.plugin.trackmate import Settings
from fiji.plugin.trackmate import TrackMate
from fiji.plugin.trackmate import SelectionModel
from fiji.plugin.trackmate import Logger
from fiji.plugin.trackmate.detection import LogDetectorFactory
from fiji.plugin.trackmate.tracking import FastLAPTrackerFactory
from fiji.plugin.trackmate.tracking import LAPUtils
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer as HyperStackDisplayer
import fiji.plugin.trackmate.features.FeatureFilter as FeatureFilter
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer as TrackDurationAnalyzer
from fiji.plugin.trackmate.io import TmXmlReader
from fiji.plugin.trackmate.io import TmXmlWriter

from de.fzj.jungle.trackmate.action import JungleExporter
from de.fzj.jungle.trackmate.action import FluorescenceThresholdScreener
from de.fzj.jungle.trackmate.detection import OverlayDetectorFactory
from de.fzj.jungle.trackmate.features.spot import SpotFluorescenceAnalyzerFactory

def executeTrackmate(path):
	model = Model()

	# Send all messages to ImageJ log window.
	model.setLogger(Logger.VOID_LOGGER)

	#------------------------
	# Prepare settings object
	#------------------------      
	settings = Settings()
	settings.setFrom(imp)

	# Configure detector - We use the Strings for the keys
	settings.detectorFactory = OverlayDetectorFactory()
	settings.detectorSettings = { 
		'RADIUS' : 7.5,
		'TARGET_CHANNEL' : 1
	}

	settings.addSpotAnalyzerFactory(SpotFluorescenceAnalyzerFactory())

	# Configure tracker - We want to allow merges and fusions
	settings.trackerFactory = FastLAPTrackerFactory()
	settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap() # almost good enough
	settings.trackerSettings['ALLOW_TRACK_SPLITTING'] = True
	settings.trackerSettings['SPLITTING_MAX_DISTANCE'] = 45.0

	#----------------------
	# Instantiate trackmate
	#----------------------  
	trackmate = TrackMate(model, settings)

	#------------
	# Execute all
	#------------
	trackmate.process()

	#----------------
	# Display results
	#----------------
	model.getLogger().log('Found ' + str(model.getTrackModel().nTracks(True)) + ' tracks.')

	selectionModel = SelectionModel(model)
	displayer =  HyperStackDisplayer(model, selectionModel, imp)
	displayer.render()
	displayer.refresh()

#	#-------------------
#	# Instantiate writer
#	#-------------------
#	tmFile = File(os.path.splitext(path)[0]+"_trackmate.xml")
#	writer = TmXmlWriter(tmFile)
#	writer.appendModel(model)
#	writer.writeToFile()
#
	#-------------------
	# Instantiate action
	#-------------------
#	action = JungleExporter(None);
#
#	phyloFile = File(os.path.splitext(path)[0]+".xml")
#	action.execute(trackmate, phyloFile)

	#-------------------
	# Instantiate action
	#-------------------
	action = FluorescenceThresholdScreener();
	action.setThresholds(550.0)
	action.execute(trackmate)

	return action.isThresholdExceeded()


# Open files that end in _final.tif
srcDir = DirectoryChooser("Choose!").getDirectory()
if not srcDir:
	# user canceled dialog
	sys.exit(1)

for root, directories, filenames in os.walk(srcDir):
	for filename in filenames:
		# Skip files that are not final / tif
		if not filename.endswith("_rois.tif"):
			continue
		path = os.path.join(root, filename)
		
		# Open the image
		imp = IJ.openImage(path)

		# Export Overlay to RoiManager
#		IJ.run(imp, "To ROI Manager", "");

		# Execute trackmate
		thresholdExceeded = executeTrackmate(path)

		# Clean RoiManager
#		RoiManager.getInstance().runCommand("Delete")

		# Close image
		imp.close()

		fileResult = []
		fileResult.append(filename)
		fileResult.append(thresholdExceeded)

		#-------------------
		# Write to results
		#-------------------
		with open(os.path.join(root, 'results.csv'), 'a') as resultsFile:
			writer = csv.writer(resultsFile, delimiter='\t')
			writer.writerow(fileResult)

sys.exit(0)

