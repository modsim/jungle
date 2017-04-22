package de.fzj.jungle.trackmate.gui.panels.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.HashMap;
import java.util.Map;

import de.fzj.jungle.trackmate.detection.OverlayDetector;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.panels.detector.BasicDetectorConfigurationPanel;
import fiji.util.NumberParser;
import ij.ImagePlus;

/**
 * User interface for the {@link OverlayDetector}.
 * 
 * @author Stefan Helfrich
 */
public class OverlayDetectorConfigurationPanel extends BasicDetectorConfigurationPanel
{

	private static final long serialVersionUID = -1L;

	public OverlayDetectorConfigurationPanel( ImagePlus imp, String infoText, String detectorName, Model model )
	{
		super( imp, infoText, detectorName, model );

		super.jTextFieldBlobDiameter.setVisible( true );
		super.jLabelBlobDiameterUnit.setVisible( true );
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		double radius = ( Double ) settings.get( KEY_RADIUS );
		jTextFieldBlobDiameter.setText( "" + ( 2 * radius ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		Map< String, Object > settings = new HashMap<>( 2 );
		settings.put( KEY_TARGET_CHANNEL, 1 );
		final double expectedRadius = NumberParser.parseDouble( jTextFieldBlobDiameter.getText() ) / 2;
		settings.put( KEY_RADIUS, expectedRadius );
		return settings;
	}
}
