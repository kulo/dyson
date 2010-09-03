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

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.emarsys.ecommon.prefs.config.ISetting;

/**
 * @author kulo
 */
public class RuntimeInformationResource extends DysonResource
{
	public RuntimeInformationResource(
			Context context, Request request, Response response ) 
	{  
		super(context, request, response);    
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));  
	}  

	@Override
	public Representation represent( Variant variant ) 
	{  
		Representation result = null;  
		if( variant.getMediaType().equals( MediaType.TEXT_PLAIN) ) 
		{  
			result = new StringRepresentation( this.getRuntimeInformation() );  
		}  
		return result;  
	}  
	
	protected String getRuntimeInformation()
	{
		StringBuilder buf = new StringBuilder()
		.append( "dyson runtime information\n" )
		.append( "-------------------------\n\n" );

		for( Entry<String, String> entry : 
			this.getDyson().getStatistics().getRuntimeInformation().entrySet() )
		{
			buf.append( entry ).append( '\n' );
		}

		buf.append( '\n' )
		.append( "dyson configuration\n" )
		.append( "-------------------\n\n" );

		ISetting setting;
		for( String name : new TreeSet<String>(
				this.getDyson().getConfiguration().getDeclaration().getSettingNames() ) )
		{
			setting = this.getDyson().getConfiguration().get( name );
			buf.append( name ).append( '=' )
			.append( setting != null ? setting.getValue() : "null" )
			.append( '\n' );
		}

		buf.append( '\n' );
		return buf.toString();
	}
	
}//class RuntimeInformationResource
