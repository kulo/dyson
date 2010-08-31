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
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import com.emarsys.dyson.Dyson;
import com.emarsys.dyson.DysonConfig;
import com.emarsys.dyson.DysonException;
import com.emarsys.dyson.MailStorageFileNamingScheme;
import com.emarsys.ecommon.builder.time.CalendarBuilder;
import com.emarsys.ecommon.builder.time.TimeBuilder;
import com.emarsys.ecommon.mail.MessageUtil;
import com.emarsys.ecommon.time.Dates;

/**
 * A flexible {@link MailStorageFileNamingScheme} implementation 
 * that produces storage files according to the {@link Token}s 
 * specified in the setting 
 * {@link DysonConfig#STORAGE_PROCESSED_MAIL_NAMING_SCHEME_TOKENS}.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
public class FlexibleMailStorageNamingScheme 
	extends MailStorageFileNamingScheme
{
	/**
	 * {@link Token}s that define the naming scheme.
	 * 
	 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
	 */
	public enum Token
	{
		/**
		 * 
		 */
		CURRENT_TIMESTAMP
		{
			@Override
			public String toPath( MimeMessage mail )
			{
				return Dates.timestampToString( 
						CalendarBuilder.getInstance() );
			}
		},
		
		/**
		 * 
		 */
		CURRENT_TIME_MILLIS
		{
			@Override
			public String toPath( MimeMessage mail ) 
			{
				return String.valueOf(
						TimeBuilder.getInstance().currentTimeMillis() );
			}
			
		},
		
		/**
		 * 
		 */
		RECIPIENT_NAME
		{
			@Override 
			public String toPath( MimeMessage mail )
			{
				final String rcpt = this.getToRecipient( mail );
				return rcpt == null ? "unknown" : 
					rcpt.substring( 0, rcpt.indexOf( '@' ) );
			}
		},
		
		/**
		 * 
		 */
		RECIPIENT_DOMAIN
		{
			@Override
			public String toPath( MimeMessage mail )
			{
				try
				{
					final String rcpt = this.getToRecipient( mail );
					return rcpt == null ? "unknown" : 
						MessageUtil.getMailDomain( rcpt );
				} 
				catch (AddressException e)
				{
					throw new DysonException( 
							"cannot get recipeint mail domain from mail " +
							MessageUtil.toString( mail ) + ": " + e, e );
				}
			}
		};
		
		
		/**
		 * Turns this token into a part of a storage file path.
		 * 
		 * @param mail
		 */
		public abstract String toPath( MimeMessage mail );
		
		/**
		 * 
		 * @param mail
		 * @return
		 */
		protected String getToRecipient( MimeMessage mail )
		{
			try
			{
				String rcpt = null;
				Address[] toRcpts = mail.getRecipients( RecipientType.TO );
				if( toRcpts != null && toRcpts.length >= 1 )
				{
					rcpt = MessageUtil.getValidEmailAddress( 
							toRcpts[0].toString() );
				}
				return rcpt;
			}
			catch( MessagingException e )
			{
				throw new DysonException( 
						"error on getting to recipient for mail " +
						MessageUtil.toString( mail ) + ": " + e, e );
			}
		}
		
	}//enum Token
	
	/**
	 * 
	 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
	 */
	protected class TokenSequence
	{
		Iterable<Token> tokens;
		InputStream data;
		
		/**
		 * 
		 */
		public TokenSequence( Iterable<Token> tokens, InputStream data )
		{
			this.tokens = tokens;
			this.data = data;
		}
		
		/**
		 * 
		 * @return
		 */
		public String getStoragePath()
		{
			try
			{
				StringBuilder path = new StringBuilder();
				MimeMessage mail = new MimeMessage( session, this.data );

				for( Token token : this.tokens )
				{
					path.append( '/' ).append( token.toPath(mail) );
				}

				return path.toString();
			}
			catch( MessagingException e )
			{
				throw new DysonException( 
						"error on getting filename for mail: " + e, e );
			}
		}
	}//class TokenSequence
	
	
	//cached fields
	protected final Session session = 
		Session.getDefaultInstance( new Properties() );
	protected List<Token> configuredTokens = 
		this.getConfiguredTokens();
	
	
	/**
	 * 
	 * @param dyson
	 */
	public FlexibleMailStorageNamingScheme( Dyson dyson ) 
	{
		super(dyson);
	}

	/**
	 * @see com.emarsys.dyson.MailStorageFileNamingScheme#getMailFile(File, java.io.InputStream)
	 */
	public File getMailFile( File parentDirectory, InputStream data ) throws DysonException
	{
		TokenSequence tokenSequence = 
			new TokenSequence( this.configuredTokens, data );
		
		String relativeStoragePath = 
			tokenSequence.getStoragePath() + "." + 
			this.getDyson().getStorage().getMailFileSuffix();

		return new File( parentDirectory, relativeStoragePath );
	}

	/**
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 */
	protected List<Token> getConfiguredTokens() 
		throws IllegalArgumentException
	{
		List<Token> tokens = new LinkedList<Token>();
		List<String> tokenStrs = 
			this.getDyson().getConfiguration().get(
					DysonConfig.STORAGE_PROCESSED_MAIL_NAMING_SCHEME_TOKENS  
			).getListValues();
		
		for( String tokenStr : tokenStrs )
		{
			tokens.add( Token.valueOf( tokenStr ) );
		}
		
		if( tokens.isEmpty() )
		{
			throw new IllegalStateException(
					"Cannot create naming scheme token list without " +
					"configured tokens!" );
		}
		
		return tokens;
	}

}//class DomainRecipientTimestampNamingScheme
