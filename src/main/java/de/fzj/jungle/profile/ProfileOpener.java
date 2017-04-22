package de.fzj.jungle.profile;

import java.io.IOException;
import java.util.List;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * WIP
 * 
 * @author Stefan Helfrich
 */
@Plugin(type = Command.class, headless = true, menu = {@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
mnemonic = MenuConstants.PLUGINS_MNEMONIC), @Menu(label = "JuNGLE"), @Menu(label = "Profile Opener")})
public class ProfileOpener implements Command
{
	@Parameter
	ProfileService profileService;

	@Override
	public void run()
	{
		try
		{
			Profile profile = profileService.open( "/Users/stefan/legacy_profile_test.xml" );
			System.out.println( profile.getMedianRadius() );
			profileService.save( profile, "/Users/stefan/legacy_profile_test_resaved.xml" );
			List< Profile > availableProfiles = profileService.getAvailableProfiles();

			for ( Profile availableProfile : availableProfiles )
			{
				System.out.println( availableProfile.getProfileName() );
			}
		}
		catch ( IOException exc )
		{
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
	}

	public static void main( String[] args )
	{
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();
		ij.command().run(ProfileOpener.class, true);
	}
}
