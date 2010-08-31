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

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.restlet.Server;
import org.subethamail.smtp.MessageListener;
import org.subethamail.smtp.server.SMTPServer;

import com.emarsys.ecommon.prefs.config.Configurable;
import com.emarsys.ecommon.prefs.config.Configuration;
import com.emarsys.ecommon.prefs.config.ISetting;

/**
 * <p>
 * <em>dyson</em> -- an "absorbing" SMTP server for 
 * testing purposes.
 * </p>
 * <h3>what is dyson?</h3>
 * <p>
 * dyson's a simple mail server whose sole aim is to receive mails 
 * over the SMTP protocol and to store them persistently
 * as quickly as possible (sic!).
 * </p><p>
 * the name "dyson" is inspired by 
 * <a href="http://en.wikipedia.org/wiki/Freeman_Dyson">freeman dyson</a> 
 * and his theory of a <a href="http://en.wikipedia.org/wiki/Dyson_sphere"> 
 * dyson sphere</a>, i.e. a structure that is meant to completely 
 * encompass a star and capture most or all of its energy output. 
 * <br/>
 * think of the dyson MTA as a dyson sphere for mailing environments.
 * </p>
 * <h3>getting started</h3>
 * <p>
 * to use dyson as an SMTP daemon you may start a 
 * {@link DysonServer} java process (or your own implementation);
 * see the server's {@link DysonServer#main(String[]) main-method} as well as 
 * {@link DysonConfig} for further details on how to configure and run dyson. 
 * </p>
 * <h3>dyson's parts</h3>
 * <p>
 * basically dyson consists of three server components 
 * and itself forms the glue between those 
 * {@link DysonPart dyson parts}:
 * <ul>
 * 	<li>the Subethamail {@link #getSmtpServer() SMTPServer} (42!)</li>
 *  <li>the {@link #getStorage() DysonStorage}</li>
 *  <li>and the {@link #getRestletServer() RESTlet Server}</li>
 * </ul>
 * </p><p>
 * the SMTP component is responsible for listening on the specified
 * port, receiving emails and passing the incomming data stream to
 * {@link DysonMessageListener message listeners} which will handle any 
 * further actions.
 * </p><p>
 * 
 * </p><p>  
 * Where the {@link DysonStorage} finally puts the mail files is 
 * up to its concrete implementation as well as its associated 
 * {@link MailStorageFileNamingScheme}.
 * <br/>
 * Either the concrete {@link DysonStorage} implementation either
 * its {@link MessageListener} as well as concrete 
 * {@link MailStorageFileNamingScheme} implementations are 
 * dynamically configurable through {@link DysonConfig}.
 * </p>
 *  
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public interface Dyson extends DysonConfig, Configurable 
{
	/**
	 * <p>
	 * Retrieves {@link Dyson dyson's} {@link Configuration}.
	 * </p><p>
	 * 
	 * </p>
	 * @return always a valid {@link Configuration}, never <code>null</code>
	 * @see com.emarsys.ecommon.prefs.config.Configurable#getConfiguration()
	 */
	Configuration getConfiguration();

	/**
	 * @return the smtpServer, never <code>null</code>
	 */
	SMTPServer getSmtpServer();

	/**
	 * @return the {@link DysonStorage}, never <code>null</code>
	 */
	DysonStorage getStorage();
	
	/**
	 * @return the {@link Server RESTlet Server}, never <code>null</code>
	 */
	Server getRestletServer();
	
	/**
	 * @return the {@link DysonStatistics}, never <code>null</code>
	 */
	DysonStatistics getStatistics();
	
	/**
	 * <p>
	 * Returns a JavaMail(tm) {@link Session} that
	 * has this very {@link DysonServer}
	 * configured as its {@link Transport}.
	 * </p><p>
	 * You can create {@link MimeMessage}s using this 
	 * {@link Session} by using one of its corresponding
	 * {@link MimeMessage#MimeMessage(Session) constructors}.
	 * </p><p>
	 * 
	 * @return always a valid {@link Session} instance,
	 * 		never <code>null</code>.
	 */
	Session getJMailSession();

	/**
	 * Checks whether this dyson server is running or not.
	 * 
	 * @return <code>true</code> if either the 
	 * 	{@link SMTPServer#isRunning() SMTP server} OR the
	 * 	{@link DysonStorage#isRunning() storage} OR the 
	 *  {@link Server#isStarted() RESTlet server} is 
	 *  (still) running.
	 * 
	 * @see org.subethamail.smtp.server.SMTPServer#isRunning()
	 * @see DysonStorage#isRunning()
	 */
	boolean isRunning();

	/**
	 * <p>
	 * Factory method which dynamically creates new instances 
	 * of {@link DysonPart} and associates them with this {@link Dyson}.
	 * </p><p>
	 * {@link Dyson} has been designed for flexibility and thus almost all 
	 * of the components which make up the server (yes, it's the 
	 * {@link DysonPart parts}) are loaded and created dynamically during 
	 * dyson's initialization.<br/>
	 * The concrete {@link Class classes} which implement the 
	 * {@link DysonPart DysonParts} are specified in dyson's 
	 * {@link #getConfiguration() configuration}. 
	 * </p>
	 * 
	 * @param <P> 
	 * 	the concrete type of the part to be instantiated	
	 * @param settingName
	 * 	the {@link ISetting#getName() name} of the {@link ISetting} that 
	 * 	specifies the concrete {@link Class} implementing the passed
	 * 	<code>partType</code>
	 * @param partType 
	 * 	the (super) type of the {@link DysonPart} to be created
	 * @return always a valid {@link DysonPart} instance, 
	 * 	never <code>null</code>
	 * @throws DysonException - 
	 * 	on any error during the instantiation of the dyson part
	 * 
	 * @see DysonPart
	 * @see GenericDysonPart
	 */
	<P extends DysonPart> P newDysonPart( 
			String settingName, Class<? extends P> partType )
		throws DysonException;
	
}//interface Dyson