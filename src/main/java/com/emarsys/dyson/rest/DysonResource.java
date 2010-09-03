/**
 * 
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
				"there's no Dyson instance in the restlet context attributes!" );
		}
		
		return dyson;
	}
}
