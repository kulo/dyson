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

/**
 * Indicates a severe runtime error in dyson that forces the server
 * to shutdown.
 * 
 * {@link DysonServer} makes intensive use of {@link DysonException}
 * (and other {@link RuntimeException}s) because almost all 
 * errors/exception should force the server to shutdown and therefore
 * need not to be checked.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public class DysonException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 * @param cause
	 */
	public DysonException( String message, Throwable cause )
	{
		super( message, cause );
	}

	/**
	 * @param message
	 */
	public DysonException( String message )
	{
		super( message );
	}
	
}//class DysonException
