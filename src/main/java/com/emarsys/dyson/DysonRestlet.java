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

import org.restlet.Restlet;

/**
 * Super class for {@link Restlet}s that need access to the 
 * {@link DysonServer}.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public class DysonRestlet extends Restlet implements DysonPart 
{
	protected final Dyson dyson;
	
	/**
	 * 
	 */
	public DysonRestlet( Dyson dyson ) 
	{
		super();
		this.dyson = dyson; 
	}

	public Dyson getDyson() 
	{
		return this.dyson;
	}
	
}//class DysonRestlet
