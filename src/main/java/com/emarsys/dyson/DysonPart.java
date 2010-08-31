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
 * A {@link DysonPart} is part of {@link Dyson} 
 * and has public access to it.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public interface DysonPart 
{
	/**
	 * Returns the {@link Dyson}.
	 * 
	 * @return always the same, non-<code>null</code> dyson instance
	 */
	Dyson getDyson();
	
}//class DysonServerPart
