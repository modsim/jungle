/**
 * 
 */
package de.fzj.jungle.profile;

import java.io.IOException;
import java.util.List;

import net.imagej.ImageJService;

/**
 * Interface for services that work with {@link Profile}s.
 * 
 * @author Stefan Helfrich
 */
// FIXME ProfileService should not be an ImageJService
// This way, however, it get's picked up by SciJava for discovery
public interface ProfileService extends ImageJService
{
	/**
	 * Loads a {@link Profile} from the given source. For extensibility, the
	 * nature of the source is left intentionally general, but two common
	 * examples include file paths and URLs.
	 * 
	 * @param source
	 *            The source (e.g., file path) from which a profile should be
	 *            loaded.
	 * @return An object representing the loaded data, or null if the source is
	 *         not supported.
	 * @throws IOException
	 *             if something goes wrong loading the data.
	 */
	Profile open( String source ) throws IOException;

	/**
	 * Saves a {@link Profile} to the given destination. The nature of the
	 * destination is left intentionally general, but the most common example is
	 * a file path.
	 * 
	 * @param data
	 *            The profile to be saved to the destination.
	 * @param destination
	 *            The destination (e.g., file path) to which data should be
	 *            saved.
	 * @throws IOException
	 *             if something goes wrong saving the data.
	 */
	void save( Profile data, String destination ) throws IOException;

	/**
	 * Returns a list of all available {@link Profile} instances. That is,
	 * classes implementing the {@code Profile} interface as well as profiles
	 * that are located in the profiles folder of an ImageJ instance.
	 * 
	 * @return A list of {@link Profile}s that are available for use.
	 */
	List< Profile > getAvailableProfiles();
}
