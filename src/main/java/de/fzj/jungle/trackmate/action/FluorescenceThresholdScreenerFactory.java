package de.fzj.jungle.trackmate.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

/**
 * {@link TrackMateActionFactory} for a {@link FluorescenceThresholdScreener}.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = TrackMateActionFactory.class )
public class FluorescenceThresholdScreenerFactory implements TrackMateActionFactory
{

	public static final String KEY = "FLUORESCENCE_SCREENER";

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/ISBIlogo.png" ) );

	public static final String NAME = "Screen for Fluorescence";

	public static final String INFO_TEXT = "<html>Action that checks if at least one spot has exceeded a provided threshold.</html>";

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public TrackMateAction create( TrackMateGUIController controller )
	{
		return new FluorescenceThresholdScreener();
	}

}
