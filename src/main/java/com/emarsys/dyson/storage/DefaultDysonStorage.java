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
package com.emarsys.dyson.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emarsys.dyson.Dyson;
import com.emarsys.dyson.DysonConfig;
import com.emarsys.dyson.DysonException;
import com.emarsys.dyson.DysonStorage;
import com.emarsys.dyson.MailStorageFileNamingScheme;
import com.emarsys.dyson.DysonStatistics.MailEvent;
import com.emarsys.ecommon.concurrent.Threads;
import com.emarsys.ecommon.prefs.config.Configuration;
import com.emarsys.ecommon.time.Dates;
import com.emarsys.ecommon.util.Assertions;
import com.emarsys.ecommon.util.StopableRunnable;

/**
 * The default implementation of {@link DysonStorage}.
 * 
 * TODO documentation
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public class DefaultDysonStorage extends DysonStorage 
{
	private static Logger log = LoggerFactory.getLogger( DefaultDysonStorage.class );

	public static final String LOCK_FILE_NAME = ".lock";
	
	/**
	 * Processor for the delivered mails in the 
	 * {@link DysonStorage#incomingDirName incoming folder}.
	 * 
	 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
	 */
	protected class DeliveredMailProcessor implements StopableRunnable
	{
		private volatile boolean shouldStop = false;
		private volatile boolean isRunning = false;

		/**
		 * 
		 * @see com.emarsys.ecommon.util.StopableRunnable#stop()
		 */
		public synchronized void stop()
		{
			this.shouldStop = true;
			log.info( "stopping delivered mail processor..." );
		}

		/**
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				log.info( "started delivered mail processor" );
				this.isRunning = true;

				while( !this.shouldStop )
				{
					moveDeliveredMailsIntoProcessedDir();
					//TODO use blocking (e.g.: wait and notify) instead of sleeping
					Threads.sleepSilently( Dates.SECOND_IN_MILLIS );
				}
			}
			finally
			{
				this.isRunning = false;
				fireDeliveredMailProcessorStopped();
				log.info( "stopped delivered mail processor!" );
			}
		}

		/**
		 * @return the isRunning
		 */
		public boolean isRunning()
		{
			return isRunning;
		}
	}//class DeliveredMailProcessor

	
	//monitors for synchronization
	protected final Object lifecycleMonitor = new Object();
	protected final Object fileCopyMonitor = new Object();

	//worker thread(service)s
	protected ExecutorService storageService;
	protected DeliveredMailProcessor deliveredMailProcessor;

	/**
	 * 
	 * @param dyson
	 */
	public DefaultDysonStorage( Dyson dyson ) 
	{
		super( dyson );
	}

	/**
	 * @see DysonStorage#init()
	 */
	@Override
	protected void init() throws IllegalStateException 
	{
		this.setupFileStorage();
	}



	/**
	 * <p>
	 * Starts the {@link DysonStorage}.
	 * </p><p>
	 * This will {@link #startStorageServices() start} the 
	 * storage services ({@link #storageService}, 
	 * {@link #deliveredMailProcessor}), too.
	 * </p>
	 */
	public void start()
	{
		synchronized( this.lifecycleMonitor )
		{
			if( this.isRunning() )
			{
				throw new IllegalStateException(
						"Cannot (re)start dyson storage which is " +
				"already/still running!");
			}

			log.info( "starting dyson storage component...");
			this.lockStorageDirs();
			this.startStorageServices();
		}
	}

	/**
	 * Checks whether this dyson storage is (still) running.
	 * 
	 * 
	 * @return if both the {@link #deliveredMailProcessor} thread
	 * 		as well as the {@link #storageService} is running
	 * 		(.i.e. {@link ExecutorService#isTerminated() not 
	 * 		(yet)terminated}) 
	 */
	public boolean isRunning()
	{
		synchronized( this.lifecycleMonitor )
		{
			boolean isServiceRunning = false;
			boolean isMailProcessorRunning = false;

			if( this.storageService != null && 
					!this.storageService.isTerminated() )
			{
				isServiceRunning = true;
			}
			if( this.deliveredMailProcessor != null &&
					this.deliveredMailProcessor.isRunning() )
			{
				isMailProcessorRunning = true;
			}

			return isServiceRunning && isMailProcessorRunning;
		}
	}

	/**
	 * Stops the dyson storage.
	 * 
	 * Sends and asynchronous shutdown request to the 
	 * {@link #storageService} as well as to the
	 * {@link #deliveredMailProcessor}.
	 * 
	 */
	public void stop()
	{
		synchronized( this.lifecycleMonitor )
		{
			if( isRunning() )
			{
				log.info("stopping dyson storage component...");
				if( this.storageService != null )
				{
					this.storageService.shutdown();
				}
				if( this.deliveredMailProcessor != null ) 
				{
					this.deliveredMailProcessor.stop();
				}
				this.unlockStorageDirs();
			}
			else
			{
				log.info( "dyson storage was not running - " +
				"nothing to shutdown!");
			}
		}
	}

	/**
	 *
	 */
	protected void setupFileStorage()
	{
		synchronized( this.lifecycleMonitor ) 
		{
			Configuration config = this.getDyson().getConfiguration();
			
			//get directory name for incoming mail
			if( this.incomingDirName == null )
			{
				this.incomingDirName = 
					config.get( DysonConfig.STORAGE_DIR_INCOMING ).getValue();
				this.createDirsIfNotPresent( this.incomingDirName );
			}
			//get directory name for already processed mail
			if( this.processedDirName == null )
			{
				this.processedDirName = 
					config.get( DysonConfig.STORAGE_DIR_PROCESSED ).getValue();
				this.createDirsIfNotPresent( this.processedDirName );
			}
			//get the suffix for mail files
			if( this.mailFileSuffix == null )
			{
				this.mailFileSuffix = 
					config.get( 
							DysonConfig.STORAGE_MAIL_FILE_SUFFIX ).getValue();
			}
			//get the suffix for patial mail files
			if( this.mailPartialFileSuffix == null )
			{
				this.mailPartialFileSuffix = 
					config.get( DysonConfig.STORAGE_MAIL_PARTIAL_FILE_SUFFIX 
					).getValue();
			}
			//setup the naming scheme implementation
			if( this.namingScheme == null )
			{
				this.namingScheme = dyson.newDysonPart(
						DysonConfig.STORAGE_PROCESSED_MAIL_NAMING_SCHEME_CLASS, 
						MailStorageFileNamingScheme.class );
			}
		}
	}

	/**
	 * Creates the directory with the passed filename if it's not
	 * already present.
	 * 
	 * @param pathToDir - the path to the directory
	 * @throws DysonException - if it was not possible to create a
	 * 		writable directory with the passed path.
	 */
	protected void createDirsIfNotPresent( String pathToDir )
		throws DysonException
	{
		File dir = new File( pathToDir );
		if( !dir.exists() )
		{
			log.debug( "creating dir(s) \'{}\'", pathToDir );
			dir.mkdirs();
			dir.setReadable( true ); 
			dir.setWritable( true );
		}

		boolean isWritableDirectoryPresent = 
			dir.exists() && dir.isDirectory() &&
			dir.canRead() && dir.canWrite();

		if( !isWritableDirectoryPresent )
		{
			throw new DysonException( 
					"Was not able to create directory \'" + pathToDir + "\'" );
		}
	}

	protected void lockStorageDirs() throws DysonException
	{
		this.lock( this.getIncomingDirName() );
		this.lock( this.getProcessedDirName() );
	}
	
	protected void unlockStorageDirs()
	{
		this.unlock( this.getIncomingDirName() );
		this.unlock( this.getProcessedDirName() );
	}
	
	protected File getLockFileForStorageDir( String storageDirName )
	{
		Assertions.assertNotNull( storageDirName );
		return new File( storageDirName + File.separator + LOCK_FILE_NAME );
	}

	protected void lock( String storageDirName )
	{
		try 
		{
			log.debug( "locking storage directory " + storageDirName );
			
			File lockFile = this.getLockFileForStorageDir( storageDirName );

			if( lockFile.exists() )
			{
				throw new DysonException( 
						storageDirName + " is already locked by process " + 
						this.getLockingProcess( lockFile ) );
			}
			
			this.writeLockFile( lockFile );
		} 
		catch( IOException ioe ) 
		{
			final String msg = "unable to lock " + storageDirName + ": " + ioe;
			log.error( msg, ioe );
			throw new DysonException( msg, ioe );
		}
	}
	
	protected void unlock( String storagDirName )
	{
		File lockfile = this.getLockFileForStorageDir( storagDirName );
		
		if( lockfile.exists() )
		{
			boolean deleted = lockfile.delete();
			log.debug( "{} lock file {}", 
					deleted ? "successfully deleted" : "could not delete",
					lockfile.getAbsolutePath() );
		}
		else
		{
			log.warn( "cannot remove lock file {} which does not exist!",
					lockfile.getAbsolutePath() );
		}
	}
	
	protected String getLockingProcess( File lockFile ) throws IOException
	{
		if( !lockFile.exists() ) 
		{
			throw new IllegalStateException( 
					"lockfile " + lockFile.getAbsolutePath() + " does not" +
					"(no longer?) exist!" );
		}
		
		List<?> lines = FileUtils.readLines( lockFile );
		return lines.isEmpty() ? "unknown" : lines.get( 0 ).toString();
	}
	
	protected void writeLockFile( File lockFile ) throws IOException
	{
		FileUtils.writeStringToFile( lockFile, this.getPID() );
	}
	
	/**
	 * ugly hack to get the PID, only works in SUN VMs
	 */
	protected String getPID()
	{
		String pid = ManagementFactory.getRuntimeMXBean().getName();
		return pid.substring( 0, pid.indexOf( '@' ) );
	}
	
	/**
	 * Creates and starts the {@link #storageService 
	 * storage's executor service} as well as the 
	 * {@link #deliveredMailProcessor} in its own {@link Thread}.
	 * 
	 */
	protected void startStorageServices()
	{
		if( this.storageService == null  )
		{
			this.storageService = new ThreadPoolExecutor(
					0, Integer.MAX_VALUE,
					5L, TimeUnit.SECONDS,
					new SynchronousQueue<Runnable>());
		}
		if( this.deliveredMailProcessor == null )
		{
			this.deliveredMailProcessor = new DeliveredMailProcessor(); 
		}
		if( !this.deliveredMailProcessor.isRunning() )
		{
			Thread th = new Thread( 
					this.deliveredMailProcessor, "DeliveredMailProcessor" );
			th.start();
		}
	}

	/**
	 *
	 */
	protected void fireDeliveredMailProcessorStopped()
	{
		synchronized( this.lifecycleMonitor )
		{
			this.deliveredMailProcessor = null;
		}
	}

	/**
	 * <p>
	 * {@link #move(File, File) Moves} all already delivered mails 
	 * from the incoming folder to its final storage location in a 
	 * subfolder of the processed directory.
	 * </p><p>
	 * 
	 * </p>
	 * 
	 * @see #move(File, File)
	 */
	protected void moveDeliveredMailsIntoProcessedDir()
	{
		synchronized( this.fileCopyMonitor )
		{
			File processedDir = new File( this.processedDirName );

			for( File mail : this.getIncomingMailFiles() )
			{
				this.move( mail, processedDir );
			}
		}
	}

	/**
	 * 
	 * @param movee
	 * @param toDir
	 */
	protected void move( final File movee, final File toDir )
	{
		Runnable mover = new Runnable()
		{
			public void run()
			{
				boolean successful = false;
				Exception ex = null;
				File targetFile = null;

				try
				{
					log.debug( "moving {} to {}", 
							movee.getAbsolutePath(), toDir.getAbsolutePath() );

					targetFile = namingScheme.getMailFile( 
							toDir, new FileInputStream( movee ) );

					log.debug( "created storage file \'{}\' for \'{}\'",
							targetFile.getAbsolutePath(), movee.getAbsoluteFile() );

					createDirsIfNotPresent( targetFile.getParent() );
					successful = movee.renameTo( targetFile );
				}
				catch( Exception ex2 )
				{
					ex = ex2;
					successful = false;
				}

				final String from = movee.getAbsolutePath();
				final String to = (targetFile == null ) 
						? "null" : targetFile.getAbsolutePath();

				if( successful )
				{
					log.debug( "successfully moved {} to {}", from, to );
					getDyson().getStatistics().fire( MailEvent.MAIL_PROCESSED );
				}
				else
				{
					log.error( "cannot move " + from + "  to " + to +
							" (exception: " + ex + ")", ex );
				}
			}
		};

		this.storageService.submit( mover );
	}

	/**
	 * Removes all files from the incoming directory.
	 * 
	 * TODO implement locking with the {@link IncomingStorageMessageListener}s
	 * @throws IOException 
	 */
	public void clearIncomingDir() throws IOException
	{
		log.info( "cleaning incoming dir \'{}\'", this.incomingDirName );
		FileUtils.cleanDirectory( new File( this.incomingDirName) );
	}

	/**
	 * Removes all files from the processed directory.
	 * @throws IOException 
	 */
	public void clearProcessedDir() throws IOException
	{
		synchronized( this.fileCopyMonitor )
		{
			log.info( "cleaning processed dir \'{}\'", this.processedDirName);
			FileUtils.cleanDirectory( new File( this.processedDirName ) ); 
		}
	}

	/**
	 * @see DysonStorage#awaitTermination(int, TimeUnit)
	 */
	@Override
	public void awaitTermination(int timeOut, TimeUnit unit) 
	{
		Threads.awaitTerminationSilently( this.storageService, timeOut, unit );		
	}

	/**
	 * @see DysonStorage#submitTask(Runnable)
	 */
	@Override
	public void submitTask( Runnable task ) throws IllegalStateException 
	{
		if( !this.isRunning() )
		{
			throw new IllegalStateException( 
					"cannot submit task, storage is not running!" );
		}
		
		this.storageService.submit( task );
	}
	
}//class DefaultDysonStorage
