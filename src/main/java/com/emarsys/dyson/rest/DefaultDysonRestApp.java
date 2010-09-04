/**
 *   (c) Copyright 2007-2010 by emarsys eMarketing Systems AG
 * 
 *   This file is part of dyson.
 *
 *   dyson is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation; either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   dyson is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
	public Restlet doCreateRoot() 
	{
		Context context = this.getContext();
		
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
		
		log.debug( "got access to directory {} via REST", path );
		
		return directory;
	}
	
}//class DefaultDysonRestApps
