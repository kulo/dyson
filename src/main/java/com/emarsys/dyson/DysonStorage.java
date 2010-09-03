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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emarsys.ecommon.util.Assertions;
import com.emarsys.ecommon.util.CollectionToStringBuilder;

/**
 * <p>
 * The {@link DysonStorage} is the service responsible for 
 * persistently storing messages delivered to dyson. 
 * </p><p>
 * {@link DysonStorage} serves as an abstract super class for all
 * possible implemenations.
 * </p><p>
 * Where the {@link DysonStorage} finally puts the mail files is 
 * up to its concrete implementation as well as its associated 
 * {@link MailStorageFileNamingScheme}.
 * <br/>
 * It's mandatory for all {@link DysonStorage} implementations to 
 * separate the locations where incoming mails are stored/buffered/cached 
 * from the place where the files are finally stored persistently.
 * Said locations are called the {@link #getIncomingDirName() incoming} 
 * and {@link #getProcessedDirName() processed} directories.
 * </p><p>
 * Like all {@link GenericDysonPart dyson parts} every implementation of
 * {@link DysonStorage} has to provide a public default constructor taking 
 * a {@link Dyson} instance.
 * </p><p>
 * 
 * </p>
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public abstract class DysonStorage extends GenericDysonPart
{
	private static final Logger log = 
		LoggerFactory.getLogger( DysonStorage.class );
	
	/**
	 * TODO implement an overall state for the dysonstorage
	 * possible states (still to discuss):
	 *  - accepting/idle //running, nothing to do
	 *  - accepting/processing //running, currentyl (still processing mails)
	 *  - finishing_up //still processing after a stop request, but not accepting more mails
	 *  - stopped/not running
	 */
	public enum State
	{
		
	}//enum State
	
	
	/**
	 * factory method.
	 * 
	 * hooks for concrete subclasses: 
	 * 	- {@link #init()} <br/>
	 *  - {@link #checkInitialization()}
	 * 
	 */
	public static DysonStorage getInstance( Dyson dyson )
	{
		DysonStorage storage = dyson.newDysonPart( 
				DysonConfig.STORAGE_CLASS, DysonStorage.class ); 
		storage.init();
		storage.checkInitialization();
		return storage;
	}
	
	//cached settings
	protected String incomingDirName;
	protected String processedDirName;
	protected String mailFileSuffix;
	protected String mailPartialFileSuffix;

	//file system storage
	protected MailStorageFileNamingScheme namingScheme;
	
	/**
	 * Default constructor.
	 * 
	 * All subclasses of {@link DysonStorage} must have a 
	 * default public defaut constructor taking a {@link Dyson} instance.
	 */
	public DysonStorage( Dyson dyson )
	{
		super( dyson );
	}
	
	/**
	 * <p>
	 * initializes this storage instance after it's creation.
	 * </p><p>
	 * if this methods is redefined in subclasses then it should
	 * call it's super implementation as first statement unless
	 * there's a good reason not to do so.
	 * </p>
	 */
	protected abstract void init();
	
	/**
	 * <p>
	 * initializes this storage instance after it's creation.
	 * </p><p>
	 * if this methods is redefined in subclasses then it should
	 * call it's super implementation as first statement unless
	 * there's a good reason not to do so.
	 * </p>
	 * 
	 * @throws IllegalStateException
	 */
	protected void checkInitialization() throws IllegalStateException
	{
		try
		{
			Assertions.assertNotEmpty( this.incomingDirName );
			Assertions.assertNotEmpty( this.processedDirName );
			Assertions.assertNotEmpty( this.mailFileSuffix );
			Assertions.assertNotEmpty( this.mailPartialFileSuffix );
			Assertions.assertNotNull( this.namingScheme );
		}
		//TODO refactor "not empty checks" in order to omit this exception wrapping
		catch( AssertionError ae )  
		{
			throw new IllegalStateException( 
					"" + ae.getMessage(), ae );
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public String getIncomingDirName()
	{
		return this.incomingDirName;
	}
	
	public String getProcessedDirName()
	{
		return this.processedDirName;
	}

	public Collection<File> getIncomingMailFiles()
	{
		return this.getMailFiles( this.getIncomingDirName() );
	}
	
	public Collection<File> getProcessedMailFiles()
	{
		return this.getMailFiles( this.getProcessedDirName() );
	}
	
	@SuppressWarnings("unchecked")
	protected Collection<File> getMailFiles( String dirName )
	{
		File dir = new File( dirName );

		Collection<File> mailFiles = FileUtils.listFiles( 
				dir, new String[] { this.getMailFileSuffix() }, true );

		if( log.isTraceEnabled() )
		{
			log.trace( 
					"got mail files in \'{}\': {}",
					dir.getAbsolutePath(),
					new CollectionToStringBuilder().appendAll( 
							Arrays.asList( mailFiles ) ) 
			);
		}
		
		return mailFiles;
	}

	/**
	 * @return the mailFileSuffix
	 */
	public String getMailFileSuffix()
	{
		return this.mailFileSuffix;
	}

	/**
	 * @return the mailPartialFileSuffix
	 */
	public String getMailPartialFileSuffix()
	{
		return this.mailPartialFileSuffix;
	}

	/**
	 * 
	 * @return
	 */
	public MailStorageFileNamingScheme getProcessedMailFileNamingScheme()
	{
		return this.namingScheme;
	}

	/**
	 * TODO documentation
	 * @return
	 */
	public abstract boolean isRunning();

	/**
	 * TODO documentation
	 */
	public abstract void start();
	
	/**
	 * TODO documentation
	 */
	public abstract void stop();

	
	/**
	 * waits for this dysonstorage to terminate until the timeout exceeded
	 * 
	 * 
	 * TODO proper specification
	 * 
	 * @param i
	 * @param seconds
	 */
	public abstract void awaitTermination( int timeOut, TimeUnit unit );

	/**
	 * deletes all files in the {@link #getIncomingDirName() 
	 * incoming directory}
	 * @throws IOException 
	 */
	public abstract void clearIncomingDir() throws IOException;

	/**
	 * deletes all files in the {@link #getProcessedDirName()
	 * processed directory}
	 * @throws IOException 
	 */
	public abstract void clearProcessedDir() throws IOException;

	/**
	 * <p>
	 * Submits a task as {@link Runnable} to be executed asynchronously 
	 * by the {@link DysonStorage}.
	 * </p>
	 * @param task
	 * @throws IllegalStateException - if the storage is not 
	 * 	{@link #isRunning() running}
	 */
	public abstract void submitTask( Runnable task )
		throws IllegalStateException;
	
}//class DysonStorage
