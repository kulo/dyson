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

import com.emarsys.ecommon.prefs.config.Configuration;
import com.emarsys.ecommon.prefs.config.declaration.DeclareConfiguration;
import com.emarsys.ecommon.time.Dates;

/**
 * <p>
 * {@link DeclareConfiguration Declares} dyson's {@link Configuration}.
 * </p><p>
 * You can override the default values by specifying equally named
 * {@link System#getProperties() system properties} using <tt>java</tt>'s
 * command line arguments (e.g. -Ddyson.smtp.port=10025) 
 * when starting the {@link DysonServer} or by specifying a 
 * seperate properties file using the system property 
 * {@value #SERVER_PROPERTIES_FILE}.
 * <br/>
 * The resolution precedence for settings will be:<br/><br/>
 * <tt>properties file -&gt; system properties -&gt; defaults</tt> 
 * </p>
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
@DeclareConfiguration(
		name = "com.emarsys.dyson",
		type = Configuration.Type.DEFAULT,
		defaultSuffix = "_DEFAULT" )
public interface DysonConfig
{
	/** Defines String constants that should not be considered as
	 * 	part of configuration declaration */
	String[] EXCLUDED_NAMES = {};
	
	
	//dyson server settings
	
	/**
	 * System property that specifies the path to an optional
	 * properties file that overrides the defaults or other 
	 * the system properties.<br/>
	 * NOTE: there's no default value, if the setting/system property is not
	 * set then there's no properties file to read.
	 */
	String SERVER_PROPERTIES_FILE = "dyson.server.properties.file";
	
	/** Enables the dyson server to register a hook that calls its stop()
	 * 	method on the shutdown of the JVM */
	String SERVER_SHUTDOWN_HOOK_ENABLED = "dyson.server.shutdown.hook.enabled";
	/** The shutdown hook is enable by default. */
	String SERVER_SHUTDOWN_HOOK_ENABLED_DEFAULT = Boolean.TRUE.toString();

	//REST server settings
	
	/** Specifies the port on which dyson's rest component should listen */
	String REST_SERVER_PORT = "dyson.rest.server.port";
	/** Default port for the rest server: {@value #REST_SERVER_PORT_DEFAULT}*/
	String REST_SERVER_PORT_DEFAULT = "1080";
	
	/** Specifies the concrete class that should be instanced 
	 * to run the application in dyson's REST component */
	String REST_APPLICATION_CLASS = "dyson.rest.application.class";
	/** The default restlet application class */
	String REST_APPLICATION_CLASS_DEFAULT = "com.emarsys.dyson.rest.DefaultDysonRestApp";
	
	//SMTP server settings

	/** Announce TLS support on SMTP EHLO requests */
	String SMTP_ANNOUNCE_TLS_SUPPORT = "dyson.smtp.announce.tls.support";
	/** Per default do not announce TLS support */
	String SMTP_ANNOUNCE_TLS_SUPPORT_DEFAULT = Boolean.FALSE.toString();
	
	/** The SMTP server's connection timeout in milli seconds */
	String SMTP_CONNECTION_TIMEOUT_MILLIS = "dyson.smtp.connection.timeout.millis";
	/** Default SMTP connection timeout after 1 min */
	String SMTP_CONNECTION_TIMEOUT_MILLIS_DEFAULT = String.valueOf( Dates.MINUTE_IN_MILLIS );
	
	/** Defines whether the discarding mechanism for recipients is enabled.
	 *  */
	String SMTP_DISCARD_RECIPIENTS_ENABLED = "dyson.smpt.discard.recipients.enabled";
	/** Per default discarding recipients is enabled. */
	String SMTP_DISCARD_RECIPIENTS_ENABLED_DEFAULT = Boolean.TRUE.toString();
	
	/** TO recipients matching this regex will be discarded if 
	 *  {@link #SMTP_DISCARD_RECIPIENTS_ENABLED discarding is enabled} */
	String SMTP_DISCARD_RECIPIENTS_REGEX = "dyson.smtp.discard.recipients.regex";
	/** Per default discard \@example.com recipients */
	String SMTP_DISCARD_RECIPIENTS_REGEX_DEFAULT = ".*@example.com";
	
	/** Cache mails greater than this threshold to disk while handling them.
	 *  Value has to be a power of 2. */
	String SMTP_MAIL_DISK_CACHING_THRESHOLD_BYTES = "dyson.smtp.mail.disk.caching.threshold.bytes";
	/** Per default cache mails greater than 1MB to the disk */
	String SMTP_MAIL_DISK_CACHING_THRESHOLD_BYTES_DEFAULT = String.valueOf( (int) Math.pow( 2, 10 ) );
	
	/**
	 * The maximum number of parallel connections the SMTP accepts.
	 */
	String SMTP_MAX_CONNECTIONS = "dyson.smtp.max.connections";
	/** Per default use a maximum of {@value #SMTP_MAX_CONNECTIONS_DEFAULT} 
	 *  connections, our ptmas use a value which is by far smaller,
	 *  usually the max. connections are determined per domain 
	 *  and are set to 50-100 */
	String SMTP_MAX_CONNECTIONS_DEFAULT = "10000";
	
	/** The port dyson's SMTP server listens to */
	String SMTP_PORT = "dyson.smtp.port";
	/** The default SMTP port is {@value #SMTP_PORT_DEFAULT} */
	String SMTP_PORT_DEFAULT = "1025";
	
	
	//storage config
	
	/** Use this concrete storage implementation.
	 * 	The class has to be a subclass of DysonStorage */
	String STORAGE_CLASS = "dyson.storage.class";
	/** Per default use the DysonStorage class itself */
	String STORAGE_CLASS_DEFAULT = "com.emarsys.dyson.storage.DefaultDysonStorage";

	/** Defines the storage's incoming directory */
	String STORAGE_DIR_INCOMING = "dyson.storage.dir.incoming";
	/** Storage's default incoming directory: 
	 *  {@value #STORAGE_DIR_INCOMING_DEFAULT} */
	String STORAGE_DIR_INCOMING_DEFAULT = "/var/tmp/dyson/incoming";
	
	/** Defines the storage's directory for already processed mails */
	String STORAGE_DIR_PROCESSED = "dyson.storage.dir.processed";
	/** Storage's default directory for already processed mails: 
	 *  {@value #STORAGE_DIR_PROCESSED_DEFAULT} */
	String STORAGE_DIR_PROCESSED_DEFAULT = "/var/tmp/dyson/processed";
	
	/** Defines the buffer size for the FileOutputStream of the storage */
	String STORAGE_FILE_BUFFER_SIZE_BYTES = "dyson.storage.file.buffer.size.bytes";
	String STORAGE_FILE_BUFFER_SIZE_BYTES_DEFAULT = "1024";
	
	/** Use this MessageListener implementation to handle incoming mail. 
	 *  The specified class has to be a subclass of MessageListener and
	 *  must provide a default constructor. */
	String STORAGE_INCOMING_MESSAGE_LISTENER_CLASS = "dyson.storage.incoming.message.listener.class";
	String STORAGE_INCOMING_MESSAGE_LISTENER_CLASS_DEFAULT = "com.emarsys.dyson.storage.IncomingStorageMessageListener";

	/** Defines the suffix (without the '.') for files that store mails  */
	String STORAGE_MAIL_FILE_SUFFIX = "dyson.storage.mail.file.suffix";
	String STORAGE_MAIL_FILE_SUFFIX_DEFAULT = "mail";
	
	/** Defines the suffix (without the '.') for files that store partial mails 
	 * 	(currently written) */
	String STORAGE_MAIL_PARTIAL_FILE_SUFFIX = "dyson.storage.mail.partial.file.suffix";
	String STORAGE_MAIL_PARTIAL_FILE_SUFFIX_DEFAULT = "part";
	
	/** Defines the mail storage naming scheme implementation used to
	 * 	create the final file name for already processed mails */
	String STORAGE_PROCESSED_MAIL_NAMING_SCHEME_CLASS = "dyson.storage.processed.mail.naming.scheme.class";
	String STORAGE_PROCESSED_MAIL_NAMING_SCHEME_CLASS_DEFAULT = "com.emarsys.dyson.storage.FlexibleMailStorageNamingScheme";
	
	/** A comma-seperated list of the string representation of 
	 * FlexibleMailStorageNamingScheme.Tokens that define the naming scheme
	 * if {@link #STORAGE_PROCESSED_MAIL_NAMING_SCHEME_CLASS} is set to 
	 * the said flexible implementation */
	String STORAGE_PROCESSED_MAIL_NAMING_SCHEME_TOKENS = "dyson.storage.processed.mail.naming.scheme.tokens";
	/** Per default the flexible naming scheme is set to */
	String STORAGE_PROCESSED_MAIL_NAMING_SCHEME_TOKENS_DEFAULT = "RECIPIENT_DOMAIN,RECIPIENT_NAME,CURRENT_TIMESTAMP";

}//interface DysonConfig
