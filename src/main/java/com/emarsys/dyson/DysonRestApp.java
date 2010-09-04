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
package com.emarsys.dyson;

import org.restlet.Application;
import org.restlet.Restlet;

/**
 * Super class for for the concrete {@link Application}s that need access to the 
 * {@link DysonServer}.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public abstract class DysonRestApp extends Application implements DysonPart 
{
	public static final String ATTRIBUTE_DYSON_INSTANCE = "dyson.instance";
	
	protected final Dyson dyson;
	
	/**
	 * 
	 */
	public DysonRestApp( Dyson dyson ) 
	{
		super();
		this.dyson = dyson;
	}

	public Dyson getDyson() 
	{
		return this.dyson;
	}

	/**
	 * adds the {@link #getDyson() dyson} instance as context 
	 * attribute which will be used by DysonResources
	 */
	protected void addDysonInstanceToContextAttributes()
	{
		this.getContext().getAttributes().put( 
				ATTRIBUTE_DYSON_INSTANCE, this.getDyson() );
	}

	/**
	 * <p>
	 * Creates the root restlet.
	 * </p><p>
	 * Contains {@link #doCreateRoot()} as hook for concrete subclasses.
	 * </p><p>
	 * {@link #addDysonInstanceToContextAttributes() Adds} the 
	 * {@link #getDyson()} instance to the {@link #getContext() Restlet
	 * Context} before delegating the creation of the root restlet to
	 * {@link #doCreateRoot()}.
	 * </p>
	 */
	@Override
	public Restlet createRoot()
	{
		this.addDysonInstanceToContextAttributes();
		return this.doCreateRoot();
	}

	/**
	 * Has to be defined in the concrete implementation of {@link DysonRestApp}.
	 * 
	 * @see #createRoot()
	 */
	protected abstract Restlet doCreateRoot();
	
}//class DysonRestlet
