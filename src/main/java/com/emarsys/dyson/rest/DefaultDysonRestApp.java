/**
 * 
 */
package com.emarsys.dyson.rest;

import java.io.File;
import java.io.IOException;

import org.restlet.Context;
import org.restlet.Directory;
import org.restlet.Restlet;
import org.restlet.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emarsys.dyson.Dyson;
import com.emarsys.dyson.DysonException;
import com.emarsys.dyson.DysonRestApp;
import com.emarsys.dyson.DysonStorage;

/**
 * <p>
 * Default {@link DysonRestApp} implementation.
 * </p><p>
 * Registers the following resources:
 * <ul>
 * 	<li><tt>/info</tt> =&gt; {@link RuntimeInformationResource}</li>
 * 	<li><tt>/storage/incoming</tt> =&gt; 
 * 			{@link DysonStorage#getIncomingDirName() incoming dir}</li>
 *  <li><tt>/storage/incoming</tt> =&gt; 
 * 			{@link DysonStorage#getProcessedDirName() processed dir}</li>
 * </ul>
 * </p>
 * @author kulo
 */
public class DefaultDysonRestApp extends DysonRestApp 
{
	private static final Logger log = 
		LoggerFactory.getLogger( DefaultDysonRestApp.class );
	
	/**
	 * @param dyson
	 */
	public DefaultDysonRestApp( Dyson dyson ) 
	{
		super(dyson);
	}

	/**
	 * @see com.emarsys.dyson.DysonRestApp#createRoot()
	 */
	@Override
	public Restlet createRoot() 
	{
		Context context = this.getContext();
		
		//add the Dyson instance as context attribute
		//which will be used by DysonResources
		this.getContext().getAttributes().put( 
				ATTRIBUTE_DYSON_INSTANCE, this.getDyson() );

		//the root restlet is a router which dispatches the requests to the 
		//concrete DysonResources
		Router router = new Router( context );
		router.attach( "/info", 
				RuntimeInformationResource.class );
		
		router.attach( "/storage/incoming", this.getDirectory( 
				this.getDyson().getStorage().getIncomingDirName() ) );
		router.attach( "/storage/processed", this.getDirectory( 
				this.getDyson().getStorage().getProcessedDirName() ) );
	
		
		return router;
	}
	
	protected Directory getDirectory( String path )
	{
		String canonicalPath = null;
		
		try
		{
			canonicalPath = "file://" + new File( path ).getCanonicalPath();
		}
		catch( IOException ioe )
		{
			throw new DysonException( 
					"cannot get cannonical representation of storage dir " +
					path, ioe );
		}
		
		Directory directory = new Directory( 
				this.getContext(), canonicalPath );
		directory.setDeeplyAccessible( true);
		directory.setListingAllowed( true );
		directory.setModifiable( true );
		
		log.info( "got directory for {}: {}", path, directory );
		
		return directory;
	}
	
}//class DefaultDysonRestApps
