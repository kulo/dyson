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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageListener;
import org.subethamail.smtp.TooMuchDataException;

import com.emarsys.dyson.Dyson;
import com.emarsys.dyson.DysonConfig;
import com.emarsys.dyson.DysonMessageListener;
import com.emarsys.dyson.DysonServer;
import com.emarsys.dyson.DysonStorage;
import com.emarsys.dyson.DysonStatistics.MailEvent;
import com.emarsys.ecommon.builder.time.DateBuilder;
import com.emarsys.ecommon.io.IOUtil;
import com.emarsys.ecommon.io.NewlineOutputStream;
import com.emarsys.ecommon.prefs.config.Configuration;
import com.emarsys.ecommon.time.Dates;

/**
 * <p>
 * A {@link MessageListener} for {@link DysonServer#getSmtpServer() 
 * dyson's SMTP component} that stores the incoming mails to the 
 * {@link DysonStorage#getIncomingDirName() incoming-directory} of the
 * {@link DysonStorage}.
 * </p><p>
 * TODO documentation
 * </p>
 * @see DysonStorage
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public class IncomingStorageMessageListener extends DysonMessageListener
{
	private static Logger log = 
		LoggerFactory.getLogger( IncomingStorageMessageListener.class );
	
	protected final Random random;
	protected final int bufferSize;
	/**
	 * 
	 */
	protected final Pattern discardPattern;
	
	/**
	 * 
	 */
	public IncomingStorageMessageListener( Dyson dyson )
	{
		super( dyson );
		
		Configuration config = this.dyson.getConfiguration(); 
		this.random = new Random();
		this.bufferSize = config.get( 
				DysonConfig.STORAGE_FILE_BUFFER_SIZE_BYTES ).getIntValue();
		
		if( config.get( 
				DysonConfig.SMTP_DISCARD_RECIPIENTS_ENABLED ).getBooleanValue() )
		{
			this.discardPattern  = Pattern.compile( config.get( 
					DysonConfig.SMTP_DISCARD_RECIPIENTS_REGEX ).getValue()
			);
		}
		else
		{
			this.discardPattern = null;
		}
	}

	/**
	 * @see org.subethamail.smtp.MessageListener#accept(java.lang.String, java.lang.String)
	 */
	public boolean accept( String from, String recipient )
	{
		this.getDyson().getStatistics().fire( MailEvent.MAIL_HANDLED );
		return true;
	}

	/**
	 * @see org.subethamail.smtp.MessageListener#deliver(java.lang.String, java.lang.String, java.io.InputStream)
	 */
	public void deliver( 
			final String from, final String recipient, final InputStream data )
		throws TooMuchDataException, IOException
	{
		if( this.discardPattern != null && 
				this.discardPattern.matcher( recipient ).matches() )
		{
			log.debug( "discarded mail to \"{}\" from \"{}\"", recipient, from );
			this.getDyson().getStatistics().fire( MailEvent.MAIL_DISCARDED );
			return;
		}

		Runnable storingTask = new Runnable()
		{
			public void run()
			{
				try
				{
					String baseFileName, partFileName, mailFileName;
					
					baseFileName= getBaseFileName( from, recipient );
					partFileName =  baseFileName + 
						getDyson().getStorage().getMailPartialFileSuffix();
					mailFileName = baseFileName + "." +
						getDyson().getStorage().getMailFileSuffix();
					
					writeMailFile( data, partFileName );
					renameMailFile( partFileName, mailFileName );
					getDyson().getStatistics().fire( MailEvent.MAIL_CAME_IN );
				}
				catch( Exception ex )
				{
					log.error( "cannot store mail: from \"" + from + 
							"\", to \"" + recipient + "\": " + ex, ex );
				}
			}
		};

		this.getDyson().getStorage().submitTask( storingTask );
	}
	
	/**
	 * the random number should correlate 
	 * with the number of maximum listener threads
	 * 
	 * @param from
	 * @param recipient
	 * @return
	 */
	protected String getBaseFileName( String from, String recipient )
	{
		int maxNbrOfConcurrentListeners = 
			this.getDyson().getConfiguration().get( 
					DysonConfig.SMTP_MAX_CONNECTIONS ).getIntValue();
		StringBuilder buf = new StringBuilder()
		.append( this.getDyson().getStorage().getIncomingDirName() )
		.append( '/' ).append( Dates.format( 
				DateBuilder.getInstance(), Dates.FORMAT_TIMESTAMP ) )
		.append('_').append( random.nextInt( maxNbrOfConcurrentListeners ) ); 
		return buf.toString();
	}
	
	/**
	 * 
	 * @param data
	 * @param partFileName
	 * @throws IOException
	 */
	protected void writeMailFile( InputStream data, String partFileName ) 
		throws IOException
	{
		log.debug( "writing mail to \'{}\'", partFileName  );
		
		int len;
		byte buf[] = new byte[ bufferSize ];
		
		FileOutputStream fos = 
			new FileOutputStream( partFileName, false );
		NewlineOutputStream nos = 
			new NewlineOutputStream( fos );
		PrintStream ps = new PrintStream( nos );

		while( (len = data.read( buf ) ) > 0 )
		{
			ps.write( buf, 0, len );
		}

		ps.flush();
		IOUtil.silentClose( ps );
	}
	
	/**
	 * 
	 * @param partFileName
	 * @param mailFileName
	 */
	protected void renameMailFile( String partFileName, String mailFileName )
	{
		log.debug( "renaming \'{}\' to \'{}\'", partFileName, mailFileName  );
		
		File partFile = new File( partFileName );
		File mailFile = new File( mailFileName );
		boolean successfullyRenamed = partFile.renameTo( mailFile );
		
		if( !successfullyRenamed )
		{
			log.error( "could not rename file \'{}\' to \'{}\'",
					partFile, mailFile );
		}
	}
	
}//class IncomingStorageMessageListener
