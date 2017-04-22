package de.fzj.jungle.segmentation;

import java.awt.geom.Area;
import java.awt.geom.Path2D.Double;
import java.io.Serializable;

/**
 * Custom implementation of {@link Area} to make it serializable.
 * 
 * @author Stefan Helfrich
 */
@Deprecated
public class CustomArea extends Area implements Serializable
{

	private static final long serialVersionUID = 1L;

	public CustomArea( Double path )
	{
		super( path );
	}

	public CustomArea()
	{
		super();
	}

}
