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

import java.io.File;
import java.io.InputStream;

/**
 * {@link MailStorageFileNamingScheme} defines a naming scheme 
 * for storing mail data in a file system.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public abstract class MailStorageFileNamingScheme extends GenericDysonPart
{
	/**
	 * 
	 * @param dyson
	 */
	public MailStorageFileNamingScheme( Dyson dyson ) 
	{
		super( dyson );
	}

	/**
	 * Returns a {@link File} where the passed mail data should be stored.
	 * 
	 * @param parentDirectory - the parent directory to for the storage file
	 * @param data - the mail data to be stored
	 * @throws DysonException - on any error
	 * @return always a valid {@link File} instance, never <code>null</code>.
	 */
	public abstract File getMailFile( File parentDirectory, InputStream data ) 
		throws DysonException;

}//interface MailStorageFileNamingScheme
