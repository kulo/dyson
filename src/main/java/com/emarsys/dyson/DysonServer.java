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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Session;
import javax.mail.Transport;

import org.restlet.Server;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageListener;
import org.subethamail.smtp.server.SMTPServer;

import com.emarsys.ecommon.builder.time.CalendarBuilder;
import com.emarsys.ecommon.collections.MapUtil;
import com.emarsys.ecommon.concurrent.Threads;
import com.emarsys.ecommon.mail.JMailProperties;
import com.emarsys.ecommon.prefs.config.Configuration;
import com.emarsys.ecommon.prefs.config.ConfigurationBackend;
import com.emarsys.ecommon.prefs.config.ConfigurationDeclaration;
import com.emarsys.ecommon.prefs.config.ISetting;
import com.emarsys.ecommon.prefs.config.backend.DefaultsConfigurationBackend;
import com.emarsys.ecommon.prefs.config.backend.PropertiesConfigurationBackend;
import com.emarsys.ecommon.time.Dates;
import com.emarsys.ecommon.util.Classes;
import com.emarsys.ecommon.util.StopableRunnable;

/**
 * <p>
 * {@link DysonServer} is the default implementation of the {@link Dyson} MTA.
 * </p><p>
 * See {@link Dyson} for further details.
 * </p>
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public class DysonServer 
	implements  Dyson, DysonStatistics, StopableRunnable
{
	private static final Logger log = 
		LoggerFactory.getLogger( DysonServer.class );

	//dyson sever fields
	/**
	 * flag that indicates shutdown requests to dyson
	 */
	protected volatile boolean shouldStop = false;
	/**
	 * dyson's {@link Configuration}.
	 */
	protected Configuration config;
	/**
	 * dyson's {@link SMTPServer} component.
	 */
	protected SMTPServer smtpServer;
	/**
	 * dyson's {@link DysonStorage storage}.
	 */
	protected DysonStorage storage;
	/**
	 * the {@link MessageListener}s
	 */
	protected Collection<MessageListener> msgListeners;
	/**
	 * The RESTlet server to access runtime information.
	 */
	protected Server restServer;
	
	//runtime information
	protected Calendar startTime = null;
	protected AtomicInteger nbrOfHandledMails = new AtomicInteger( 0 );
	protected AtomicInteger nbrOfIncomingMails = new AtomicInteger( 0 );
	protected AtomicInteger nbrOfDiscardedMails = new AtomicInteger( 0 );
	protected AtomicInteger nbrOfProcessedMails = new AtomicInteger( 0 );
	
	//javamail "proxy" to dyson
	/**
	 * a JavaMail(tm) session that has this
	 * {@link DysonServer} configured as default {@link Transport}.
	 */
	protected Session session;

	/**
	 * <p>
	 * Creates a new {@link DysonServer} instance.
	 * </p><p>
	 * Initializes the server's {@link #initConfig(String[]) config},
	 * {@link #initStorage() storage}, {@link #initMsgListeners() 
	 * message listeners} as well as the {@link #initSmtp() SMTP server}.
	 * </p>
	 */
	public DysonServer()
	{
		this.initConfig();
		this.initStorage();
		this.initMsgListeners();
		this.initSmtp();
		this.initRestServer();
		this.registerJvmShutdownHook();
	}

	/**
	 * @see Dyson#newDysonPart(String, Class)
	 */
	public <P extends DysonPart> P newDysonPart(
			String settingName, Class<? extends P> partType ) 
	{
		if( settingName == null || settingName.length() == 0 )
		{
			throw new IllegalArgumentException(
					"invalid setting name for specifying the " +
					"concrete dyson part implementation: " + settingName );
		}
		if( partType == null )
		{
			throw new IllegalArgumentException(
					"dyson part type is null: " + settingName );
		}
			
		String partClassName = 
			this.getConfiguration().get( settingName ).getValue();
		
		P part = Classes.instantiator( partType, DysonException.class )
		.forName( partClassName )
		.useConstructor( Dyson.class )
		.withParams( this )
		.newInstance();
		
		return part;
	}

	/**
	 * <p>
	 * Initializes the server's {@link #config configuration}.
	 * </p><p>
	 * Creates the {@link Configuration} for this server by wrapping
	 * the {@link System#getProperties() system properties} into a 
	 * {@link PropertiesConfigurationBackend} that uses <tt>this</tt> 
	 * {@link Dyson} instance (i.e. the inherited {@link DysonConfig})
	 * as the source for its {@link ConfigurationDeclaration}.
	 * </p>
	 */
	protected void initConfig()
	{
		ConfigurationBackend backend, sysPropsBackend, filePropsBackend = null;

		sysPropsBackend =
			PropertiesConfigurationBackend.wrap( System.getProperties() );

		Properties propsFromFile = this.loadPropsFromFile();

		if( propsFromFile != null && !propsFromFile.isEmpty() )
		{
			filePropsBackend = 
				PropertiesConfigurationBackend.wrap( propsFromFile );

			backend = DefaultsConfigurationBackend.cascade( 
					filePropsBackend, sysPropsBackend );
		}
		else
		{
			backend = sysPropsBackend;
		}

		this.config = Configuration.getInstanceDeclaredBy( backend, this );

		this.logConfig();
	}
	
	/**
	 * Writes the initialized settings/config as debug messages 
	 * to the log file.
	 */
	protected void logConfig()
	{
		log.debug( "initializing config...");
		ISetting setting;
		for( String name: config.getDeclaration().getSettingNames() )
		{
			setting = config.get( name );
			log.debug( "got setting: {}={}", 
					name, (setting == null ? null : setting.getValue()) );
		}
	}

	/**
	 * Loads the {@link Properties} from 
	 * {@link DysonConfig#SERVER_PROPERTIES_FILE server props file}.
	 *  
	 * @return the loaded {@link Properties} or <code>null</code>
	 * 		if no properties file has been specified or found.
	 */
	protected Properties loadPropsFromFile()
	{
		Properties result = null;
		String propsFileName = System.getProperty( SERVER_PROPERTIES_FILE );
		if( propsFileName != null )
		{
			try
			{
				result = new Properties();
				result.load( new FileInputStream( propsFileName ) );
			} 
			catch( FileNotFoundException fnfe )
			{
				log.warn( "cannot load properties from " +
						"non-exitent file \'{}\': {}", propsFileName, fnfe );
			}
			catch (IOException ioe)
			{
				log.warn( "cannot load properties from " +
						"file \'{}\': {}", propsFileName, ioe );
			}
		}
		return result;
	}

	/**
	 * <p>
	 * {@link DysonStorage#initialize(Configuration) initializes} 
	 * the {@link DysonStorage storage} with this server's 
	 * {@link #config configuration}.
	 * </p><p>
	 * The configuration must have been initialized already!
	 * </p>
	 */
	protected void initStorage()
	{
		assert this.config != null;
		this.storage = DysonStorage.getInstance( this );
	}

	/**
	 * <p>
	 * Initializes the {@link #msgListeners message listeners} for
	 * the {@link #smtpServer SMTP server}.
	 * </p><p>
	 * A new {@link #newStorageMessageListener() storage message listener} 
	 * will be preset for the registration with the {@link #getSmtpServer()
	 * SMTP server}
	 * </p>
	 * @return
	 */
	protected void initMsgListeners()
	{
		this.msgListeners = new LinkedList<MessageListener>();
		this.msgListeners.add( this.newStorageMessageListener() );
		log.debug( "initialized message listeners: {}", this.msgListeners );
	}

	/**
	 * factory method for the {@link MessageListener message listeners}
	 * which have to be registered with the {@link SMTPServer SMTP component}.
	 * 
	 * @return
	 * @throws DysonException
	 */
	protected MessageListener newStorageMessageListener() 
		throws DysonException
	{
		return this.newDysonPart( 
				DysonConfig.STORAGE_INCOMING_MESSAGE_LISTENER_CLASS, 
				DysonMessageListener.class );
	}
	
	/**
	 * Initializes dyson's {@link #smtpServer SMTP server} with its
	 * {@link #config configuration}.
	 */
	protected void initSmtp()
	{
		assert this.config != null;
		assert this.msgListeners != null;
		
		this.smtpServer = new SMTPServer( this.msgListeners );

		this.smtpServer.setPort( 
				this.config.get( SMTP_PORT ).getIntValue() );
		this.smtpServer.setConnectionTimeout( 
				this.config.get( SMTP_CONNECTION_TIMEOUT_MILLIS ).getIntValue() );
		this.smtpServer.setDataDeferredSize( 
				this.config.get( 
						SMTP_MAIL_DISK_CACHING_THRESHOLD_BYTES ).getIntValue() );
		this.smtpServer.setMaxConnections( 
				this.config.get( SMTP_MAX_CONNECTIONS ).getIntValue() );
		this.smtpServer.setAnnounceTLS( 
				this.config.get( SMTP_ANNOUNCE_TLS_SUPPORT ).getBooleanValue() );

		log.debug( "initialized SMTP server" );
	}

	/**
	 * initializes dyson's REST server instance with port specified 
	 * under {@link DysonConfig#REST_SERVER_PORT} and registers as new 
	 * instance of {@link DysonConfig#REST_SERVER_ROOT_RESTLET_CLASS}. 
	 */
	protected void initRestServer()
	{
		assert this.config != null;
		
		int port = this.getConfiguration().get( REST_SERVER_PORT ).getIntValue();
		
		DysonRestlet rootRestlet = 
			this.newDysonPart( REST_SERVER_ROOT_RESTLET_CLASS, DysonRestlet.class );
		
		this.restServer = new Server( Protocol.HTTP, port, rootRestlet );
	}
	
	/**
	 * register server shutdown hook on jvm shutdown if enabled in the config
	 */
	protected void registerJvmShutdownHook()
	{
		if( this.config.get( SERVER_SHUTDOWN_HOOK_ENABLED ).getBooleanValue() )
		{
			Runnable finalizer = new Runnable()
			{
				public void run()
				{
					if( isRunning() )
					{
						log.debug( "executing dyson's JVM shutdown hook..." );
						stop();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook( new Thread( finalizer ) );
		}
	}

	/**
	 * @see com.emarsys.dyson.Dyson#getConfiguration()
	 */
	public Configuration getConfiguration()
	{
		return this.config;
	}

	/**
	 * @see Dyson#getSmtpServer()
	 */
	public SMTPServer getSmtpServer()
	{
		return smtpServer;
	}

	/**
	 * @see Dyson#getStorage()
	 */
	public DysonStorage getStorage() 
	{
		return this.storage;
	}
	
	/**
	 * @see Dyson#getRestletServer()
	 */
	public Server getRestletServer() 
	{
		return this.restServer;
	}


	/**
	 * Sets the {@link Properties} needed to 
	 * configure this dyson instance as the sessions's
	 * {@link Transport}.
	 * 
	 * @param props
	 */
	protected Properties getJavaMailSmtpProps()
	{
		JMailProperties props = 
			JMailProperties.getInstance( System.getProperties() );
		props.putAll( MapUtil.getMap(
				JMailProperties.MAIL_SMTP_HOST, 
				this.smtpServer.getHostName(),
				JMailProperties.MAIL_SMTP_PORT,
				String.valueOf( this.smtpServer.getPort() ) )
		);
		return props;
	}
	
	/**
	 * @see com.emarsys.dyson.Dyson#getStatistics()
	 */
	public DysonStatistics getStatistics() 
	{
		return this;
	}

	/**
	 * @see com.emarsys.dyson.Dyson#getJMailSession()
	 */
	public Session getJMailSession()
	{
		if( this.session == null )
		{
			this.session = Session.getInstance( 
					this.getJavaMailSmtpProps() );
		}
		return this.session;
	}

	/**
	 * @see com.emarsys.dyson.Dyson#isRunning()
	 */
	public synchronized boolean isRunning()
	{
		return this.smtpServer.isRunning() || 
			this.storage.isRunning() ||
			this.restServer.isStarted();
	}

	/**
	 * Starts the dyson server's SMTP and storage components
	 * asynchronously.
	 * 
	 * @see org.subethamail.smtp.server.SMTPServer#start()
	 * @see DysonStorage#start()
	 */
	public synchronized void start() throws DysonException
	{
		try
		{
			this.startTime = CalendarBuilder.getInstance(); 
				
			log.info( "starting dyson server..." );
			this.storage.start();
			log.info( "starting dyson SMTP component..." );
			this.smtpServer.start();
			log.info( "starting REST server..." );
			this.restServer.start();
		}
		catch( Exception ex )
		{
			throw new DysonException( "cannot start dyson server:" + ex, ex );
		}
	}

	/**
	 * <p>
	 * Stops the dyson server's SMTP and storage components
	 * asynchronously.
	 * </p><p>
	 * This methods will not wait (block) for the components 
	 * to be stopped.
	 * </p><p>
	 * It is assured that 
	 * </p>
	 * 
	 * @see org.subethamail.smtp.server.SMTPServer#stop()
	 * @see DysonStorage#stop()
	 */
	public synchronized void stop()
	{
		Runnable smtpStopper, storageStopper, restStopper;

		smtpStopper = new Runnable() 
		{
			public void run()
			{
				log.info( "stopping dyson SMTP component..." );
				DysonServer.this.smtpServer.stop();
			}
		};

		restStopper = new Runnable() 
		{
			public void run() 
			{
				log.info( "stopping dyson REST component" );	
				try
				{
					DysonServer.this.restServer.stop();
				}
				catch( Exception ex )
				{
					throw new DysonException( 
							"error on stopping dyson REST component: " 
							+ ex, ex );
				}
			}
		};
		
		storageStopper = new Runnable()
		{
			public void run()
			{
				DysonServer.this.storage.stop();
			}
		};

		log.info( "stopping dyson server..." );

		this.shouldStop = true;
		Threads.runAsynchronouslyIgnoringRTEs( smtpStopper );
		Threads.runAsynchronouslyIgnoringRTEs( restStopper );
		Threads.runAsynchronouslyIgnoringRTEs( storageStopper );
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		this.start();
		//TODO ugly busy waiting
		while( !this.shouldStop )
		{
			Threads.sleepSilently( Dates.SECOND_IN_MILLIS );
		}
	}

	/**
	 * @see com.emarsys.dyson.Dyson#getRuntimeInformation()
	 */
	public Map<String,String> getRuntimeInformation()
	{
		return MapUtil.getSortedMap(
				"start.time", Dates.timestampToString( this.startTime ),
				"mail.handled.count", this.nbrOfHandledMails.toString(),
				"mail.discarded.count", this.nbrOfDiscardedMails.toString(),
				"mail.incoming.count", this.nbrOfIncomingMails.toString(),
				"mail.processed.count", this.nbrOfProcessedMails.toString()
		);
	}
	
	/**
	 * @see com.emarsys.dyson.DysonStatistics#fire(com.emarsys.dyson.DysonStatistics.MailEvent)
	 */
	public void fire( MailEvent event ) 
	{
		int cnt; 
		//TODO replace with more dynamic event handling
		//this code has to be changed on every change to MailEvent(s) => ugly
		//introduce a registry for listeners in DysonStatistics
		switch( event )
		{
		case MAIL_HANDLED:
			cnt = this.nbrOfHandledMails.getAndIncrement();
			log.debug( "handled mail #{}", cnt );
			break;
		case MAIL_DISCARDED:
			cnt = this.nbrOfDiscardedMails.getAndIncrement();
			log.debug( "discarded mail #{}", cnt );
			break;
		case MAIL_CAME_IN:
			cnt = this.nbrOfIncomingMails.getAndIncrement();
			log.debug( "got incoming mail #{}", cnt );
			break;
		case MAIL_PROCESSED:
			cnt = this.nbrOfProcessedMails.getAndIncrement();
			log.debug( "processed mail #{}", cnt );
			break;
		default:
			log.warn( "ignoring unknown event: " + event );
		}
	}
	
	/**
	 * <p>
	 * Initializes a {@link DysonServer} {@link #start()}s it.
	 * </p><p>
	 * {@link #DysonServer() Creates} a new server instance
	 * and {@link #run() runs} it. Every uncaught exception will
	 * be logged and finally the server will be {@link #stop() stopped}.
	 * </p><p>
	 * The dyson server can be configured using {@link System#getProperties() 
	 * system properties } or a seperate {@link DysonConfig#SERVER_PROPERTIES_FILE
	 * properties file}, see {@link DysonConfig} for further details.
	 * </p>
	 * 
	 * @param args - the cmd line arguments which are ignored
	 * 
	 * @see #DysonServer(String[])
	 * @see #run()
	 */
	public static void main( String[] args )
	{
		DysonServer server = null;
		try
		{
			server = new DysonServer();
			server.run();
		}
		catch( Throwable th )
		{
			log.error( "fatal error in dyson server: " + th, th );
		}
		finally
		{
			if( server != null && server.isRunning() )
			{
				server.stop();
			}
		}
	}
	
}//class DysonServer
