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

import java.util.Map;

/**
 * {@link DysonStatistics} is the central place where dyson's 
 * {@link MailEvent}s are being reported and runtime information is 
 * queried.   
 *
 * @author kulo
 */
public interface DysonStatistics
{
	enum MailEvent 
	{ 
		/**
		 * Indicates that a new mail is being handled by the 
		 * {@link Dyson#getSmtpServer() SMTP server}
		 */
		MAIL_HANDLED,
		/**
		 * Indicates that a new mail is being discarded by the 
		 * {@link Dyson#getSmtpServer() SMTP server}
		 */
		MAIL_DISCARDED,
		/**
		 * Indicates that a new mail has been successfully delivered to the 
		 * incoming directory by the {@link Dyson#getStorage() storage}
		 */
		MAIL_CAME_IN,
		/**
		 * Indicates taht a mail has finally been successfully delivered 
		 * to the processed directory by the {@link Dyson#getStorage() storage}
		 */
		MAIL_PROCESSED;
	}//enum MailEvent
	
	/**
	 * Indicates the occurence of the passed event.
	 * 
	 * @param event
	 */
	void fire( MailEvent event );
	
	/**
	 * Returns a {@link Map} containing pairs of {@link String strings}
	 * about dyson's runtime status quo.
	 * 
	 * @return always a valid {@link Map} instance, never <code>null</code>.
	 */
	Map<String, String> getRuntimeInformation();

}//interface DysonStatistics
