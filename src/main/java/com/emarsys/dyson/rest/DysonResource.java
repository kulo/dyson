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

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

import com.emarsys.dyson.Dyson;
import com.emarsys.dyson.DysonPart;
import com.emarsys.dyson.DysonRestApp;

/**
 * @author kulo
 */
public class DysonResource extends Resource implements DysonPart
{

	/**
	 * 
	 */
	public DysonResource() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public DysonResource( Context context, Request request, Response response )
	{
		super(context, request, response);
	}

	@Override
	public Dyson getDyson() 
	{
		Dyson dyson = (Dyson) this.getContext().getAttributes().get( 
				DysonRestApp.ATTRIBUTE_DYSON_INSTANCE );
		if( dyson == null )
		{
			throw new IllegalStateException( 
				"there's no Dyson instance in the restlet context (key: " + 
				DysonRestApp.ATTRIBUTE_DYSON_INSTANCE + ")!" );
		}
		
		return dyson;
	}
}
