package de.fzj.jungle.profile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathFactory;
import org.scijava.Priority;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.util.FileUtils;

/**
 * An {@link IOPlugin} implementation that can open and save {@link AbstractProfile}s
 * from legacy XML files as previously implemented by Christopher Probst.
 * 
 * @author Stefan Helfrich
 */
@Plugin( type = IOPlugin.class, priority = Priority.HIGH_PRIORITY )
public class LegacyProfileIOPlugin extends AbstractIOPlugin< Profile >
{

	@Override
	public Class< Profile > getDataType()
	{
		return Profile.class;
	}

	@Override
	public boolean supportsOpen( final String source )
	{
		// TODO Check if the file at the location is a legacy XML
		return FileUtils.getExtension( source ).equals( "xml" );
	}

	@Override
	public Profile open( final String source ) throws IOException
	{
		return parseXML( source );
	}

	private Profile parseXML( String source )
	{
		try
		{
			SAXBuilder sb = new SAXBuilder( XMLReaders.NONVALIDATING );
			Document doc = sb.build( new File( source ) );

			XPathFactory xpfac = XPathFactory.instance();

			// Parse elements from XML file
			Element lowerThreshold = xpfac.compile( "/profile/sim/lowerthreshold", Filters.element() ).evaluate( doc ).get( 0 );
			Element upperThreshold = xpfac.compile( "/profile/sim/upperthreshold", Filters.element() ).evaluate( doc ).get( 0 );
			Element gaussRadius = xpfac.compile( "/profile/sim/gaussradius", Filters.element() ).evaluate( doc ).get( 0 );

			Element method = xpfac.compile( "/profile/thresholding/method", Filters.element() ).evaluate( doc ).get( 0 );
			Element radius = xpfac.compile( "/profile/thresholding/radius", Filters.element() ).evaluate( doc ).get( 0 );
			Element k1 = xpfac.compile( "/profile/thresholding/k1", Filters.element() ).evaluate( doc ).get( 0 );
			Element k2 = xpfac.compile( "/profile/thresholding/k2", Filters.element() ).evaluate( doc ).get( 0 );
			Element r = xpfac.compile( "/profile/thresholding/r", Filters.element() ).evaluate( doc ).get( 0 );

			Element dilations = xpfac.compile( "/profile/operations/dilations", Filters.element() ).evaluate( doc ).get( 0 );
			Element erosions = xpfac.compile( "/profile/operations/erosions", Filters.element() ).evaluate( doc ).get( 0 );
			Element medianRadius = xpfac.compile( "/profile/operations/medianradius", Filters.element() ).evaluate( doc ).get( 0 );

			// Create a Profile from the contents of the XML file
			AbstractProfile profile = new DefaultProfile();
			profile.simLowerThreshold = Double.parseDouble( lowerThreshold.getText() );
			profile.simUpperThreshold = Double.parseDouble( upperThreshold.getText() );
			profile.simGaussRadius = Double.parseDouble( gaussRadius.getText() );

			profile.thresholdingMethod = method.getText();
			profile.thresholdingRadius = Integer.parseInt( radius.getText() );
			profile.thresholdingK1 = Double.parseDouble( k1.getText() );
			profile.thresholdingK2 = Double.parseDouble( k2.getText() );
			profile.thresholdingR = Double.parseDouble( r.getText() );

			profile.numberOfDilations = Integer.parseInt( dilations.getText() );
			profile.numberOfErosions = Integer.parseInt( erosions.getText() );
			profile.medianRadius = Double.parseDouble( medianRadius.getText() );

			File sourceFile = new File(source);
			String sourceFilename = sourceFile.getName();
			profile.profileName = sourceFilename.replaceAll( "."+FileUtils.getExtension( sourceFilename ), "" );

			return profile;
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public boolean supportsSave( final Object data, final String destination )
	{
		if ( !( data instanceof AbstractProfile ) ) { return false; }
		return true;
	}

	@Override
	public void save( final Profile data, final String destination ) throws IOException
	{
		writeXML( data, destination );
	}

	private void writeXML( final Profile data, final String destination ) throws IOException
	{
		// root elements
		Document doc = new Document();
		Element profileElement = new Element( "profile" );

		// ----
		Element simElement = new Element( "sim" );

		Element lowerThresholdElement = new Element( "lowerthreshold" );
		lowerThresholdElement.setText( Double.toString( data.getSimLowerThreshold() ) );
		simElement.addContent( lowerThresholdElement );

		Element upperThresholdElement = new Element( "upperthreshold" );
		upperThresholdElement.setText( Double.toString( data.getSimUpperThreshold() ) );
		simElement.addContent( upperThresholdElement );

		Element gaussRadiusElement = new Element( "gaussradius" );
		gaussRadiusElement.setText( Double.toString( data.getSimGaussRadius() ) );
		simElement.addContent( gaussRadiusElement );

		profileElement.addContent( simElement );

		// -----
		Element thresholdingElement = new Element( "thresholding" );

		Element methodElement = new Element( "method" );
		methodElement.setText( data.getThresholdingMethod() );
		thresholdingElement.addContent( methodElement );

		Element radiusElement = new Element( "radius" );
		radiusElement.setText( Integer.toString( data.getThresholdingRadius() ) );
		thresholdingElement.addContent( radiusElement );

		Element k1Element = new Element( "k1" );
		k1Element.setText( Double.toString( data.getThresholdingK1() ) );
		thresholdingElement.addContent( k1Element );

		Element k2Element = new Element( "k2" );
		k2Element.setText( Double.toString( data.getThresholdingK2() ) );
		thresholdingElement.addContent( k2Element );

		Element rElement = new Element( "r" );
		rElement.setText( Double.toString( data.getThresholdingR() ) );
		thresholdingElement.addContent( rElement );

		profileElement.addContent( thresholdingElement );

		// ----
		Element operationsElement = new Element( "operations" );

		Element dilationsElement = new Element( "dilations" );
		dilationsElement.setText( Integer.toString( data.getNumberOfDilations() ) );
		operationsElement.addContent( dilationsElement );

		Element erosionsElement = new Element( "erosions" );
		erosionsElement.setText( Integer.toString( data.getNumberOfErosions() ) );
		operationsElement.addContent( erosionsElement );

		Element medianRadiusElement = new Element( "medianradius" );
		medianRadiusElement.setText( Double.toString( data.getMedianRadius() ) );
		operationsElement.addContent( medianRadiusElement );

		profileElement.addContent( operationsElement );

		doc.addContent( profileElement );

		// new XMLOutputter().output(doc, System.out);
		XMLOutputter xmlOutput = new XMLOutputter();

		// display nice nice
		xmlOutput.setFormat( Format.getPrettyFormat() );
		xmlOutput.output( doc, new FileWriter( destination ) );
	}

}
