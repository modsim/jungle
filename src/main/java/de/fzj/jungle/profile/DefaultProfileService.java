/**
 * 
 */
package de.fzj.jungle.profile;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.scijava.InstantiableException;
import org.scijava.io.IOService;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import ij.Prefs;

/**
 * Default service for working with {@link AbstractProfile}s.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = Service.class )
public class DefaultProfileService extends AbstractPTService< Profile > implements ProfileService
{

	@Parameter
	IOService ioService;

	@Override
	public Profile open( final String source ) throws IOException
	{
		return ( Profile ) ioService.open( source );
	}

	@Override
	public void save( final Profile data, final String destination ) throws IOException
	{
		ioService.save( data, destination );
	}

	@Override
	public List< Profile > getAvailableProfiles()
	{
		List< Profile > profiles = new ArrayList<>();

		List< Profile > annotatedProfiles = getAnnotatedProfiles();
		profiles.addAll( annotatedProfiles );

		List< Profile > profilesFromFiles = getProfilesFromFolder();
		profiles.addAll( profilesFromFiles );

		return profiles;
	}

	private List< Profile > getProfilesFromFolder()
	{
		List< Profile > profiles = new ArrayList<>();

		// Get path to ImageJ directory for this instance
		String pathToImageJ = Prefs.getImageJDir();
		File imageJFolder = new File( pathToImageJ );

		// Check if profile directory exits
		File profilePath = new File( imageJFolder, "profile" );

		try (DirectoryStream< Path > directoryStream = Files.newDirectoryStream( Paths.get( profilePath.getAbsolutePath() ) ))
		{
			for ( Path path : directoryStream )
			{
				Profile profile = open( path.toString() );
				if ( profile != null )
				{
					profiles.add( profile );
				}
			}
		}
		catch ( IOException ex )
		{}

		return profiles;
	}

	/**
	 * TODO Documentation
	 */
	private List< Profile > getAnnotatedProfiles()
	{
		List< Profile > profiles = new ArrayList<>();
		for ( final PluginInfo< Profile > info : getPlugins() )
		{
			try
			{
				Profile profile = info.createInstance();
				profiles.add( profile );
			}
			catch ( InstantiableException exc )
			{
				exc.printStackTrace();
			}
		}
		return profiles;
	}

	@Override
	public Class< Profile > getPluginType()
	{
		return Profile.class;
	}

}
