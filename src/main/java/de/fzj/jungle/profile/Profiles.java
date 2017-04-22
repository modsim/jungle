package de.fzj.jungle.profile;

/**
 * Helper class for working with {@link Profile}s.
 * 
 * @author Stefan Helfrich
 *
 */
public final class Profiles
{
	private Profiles()
	{
		// NB: prevent instantiation of utility class.
	}

	/**
	 * Convert from a string representation of a profile to a {@link Profile}
	 * object.
	 * 
	 * @param profileName
	 * @return {@link Profile} for provided name
	 */
	public static Profile create( final String profileName )
	{
		Profile profile = null;

		if ( profileName.equals( "Default" ) )
			profile = DefaultProfile.getInstance();
		if ( profileName.equals( "Sophie" ) )
			profile = SophieProfile.getInstance();
		if ( profileName.equals( "Raphael" ) )
			profile = DFGProfile.getInstance();
		if ( profileName.equals( "DFG" ) )
			profile = BalabanProfile.getInstance();
		if ( profileName.equals( "Balaban" ) )
			profile = BalabanProfile.getInstance();
		if ( profileName.equals( "BalabanBHI" ) )
			profile = BalabanBHIProfile.getInstance();

		return profile;
	}
}
