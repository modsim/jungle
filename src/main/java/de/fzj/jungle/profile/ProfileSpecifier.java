package de.fzj.jungle.profile;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * WIP.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = Command.class, label = "Profile Specification", attrs = { @Attr( name = "no-legacy" ) } )
public class ProfileSpecifier extends ContextCommand
{

	@Parameter( label = "Profile name" )
	private String name;

	@Parameter( type = ItemIO.OUTPUT )
	private Profile profile;

	@Override
	public void run()
	{
		// TODO Let the user select the profile!
		profile = Profiles.create( "Default" );
	}

	public void setName( String name )
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public Profile getProfile()
	{
		return profile;
	}
}
