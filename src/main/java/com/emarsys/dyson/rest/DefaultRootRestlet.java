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

import java.util.TreeSet;
import java.util.Map.Entry;

import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.emarsys.dyson.Dyson;
import com.emarsys.dyson.DysonRestlet;
import com.emarsys.dyson.DysonServer;
import com.emarsys.ecommon.prefs.config.ISetting;

/**
 * {@link DefaultRootRestlet} - nomen est omen: 
 * this {@link Restlet} will be instantiated on creation of 
 * {@link DysonServer} using its default constructor and be 
 * attached as root restlet to the Restlet {@link Server}.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public class DefaultRootRestlet extends DysonRestlet 
{
	/**
	 * 
	 * @param dyson
	 */
	public DefaultRootRestlet( Dyson dyson ) 
	{
		super( dyson );
	}

	@Override
	public void handle( Request request, Response response ) 
	{   
		StringBuilder buf = new StringBuilder()
		.append( "dyson runtime information\n" )
		.append( "-------------------------\n\n" );
		
		for( Entry<String, String> entry : 
			 this.dyson.getStatistics().getRuntimeInformation().entrySet() )
		{
			buf.append( entry ).append( '\n' );
		}
		
		buf.append( '\n' )
		.append( "dyson configuration\n" )
		.append( "-------------------\n\n" );
		
		ISetting setting;
		for( String name : new TreeSet<String>(
				this.dyson.getConfiguration().getDeclaration().getSettingNames() ) )
		{
			setting = this.dyson.getConfiguration().get( name );
			buf.append( name ).append( '=' )
			.append( setting != null ? setting.getValue() : "null" )
			.append( '\n' );
		}
		
		buf.append( '\n' );
		
		response.setEntity( buf.toString(), MediaType.TEXT_PLAIN );   
	}   

}//class DefaultRootRestlet
