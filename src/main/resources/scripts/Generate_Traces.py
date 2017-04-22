from ij import IJ
from ij.gui import PolygonRoi, Roi, Overlay
from ij.process import ImageStatistics
from ij.measure import ResultsTable

# Use case: select several ROIs in current frame by adding them to the Overlay.

# Get current ImagePlus
imp = IJ.getImage()

# Get image information
width = imp.width
height = imp.height
channels = imp.getNChannels()
slices = imp.getNSlices()
frames = imp.getNFrames()

# TODO Handle fake stack

# Initialize arrays for storing cell numbers and areas
overlay = imp.getOverlay()
roiNumber = overlay.size()
intensities = [[[0 for x in range(frames)] for x in range(channels)] for x in range(roiNumber)]

# Iterate over Overlay entries	
for i in range(0,roiNumber):
	#Select a Roi
	roi = overlay.get(i);

	# Get slice number and area for the selected ROI
	channel = roi.getCPosition()
	slice = roi.getZPosition()
	frame = roi.getTPosition()
	
	# TODO Handle fake stack

	# Iterate over channels to get intensity
	for c in range(0,channels):
		# Iterate over frames
		for j in range(0, frames):
			# If we have two or more channels it's unlikely that we have a fake z-stack
			imp.setPosition(c+1, slice+1, j+1); # setPosition requires a 1-based position
			
			# Get its ImageProcessor  
			ip = imp.getProcessor()
  
			options = ImageStatistics.MEAN #| ImageStatistics.MEDIAN | ImageStatistics.AREA
			stats = ImageStatistics.getStatistics(ip, options, imp.getCalibration()) 
			
			intensities[i][c][j] = stats.mean

# Reset view
imp.setPosition(1,1,1)

table = ResultsTable()
for i in range(roiNumber):
	for j in range(channels):
		for k in range(frames):
			table.setValue("Cell "+str(i)+" Channel "+str(j), k, intensities[i][j][k])

# Set options on result table
table.showRowNumbers(True)

table.show("Traces")
