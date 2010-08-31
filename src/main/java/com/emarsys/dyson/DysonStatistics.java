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
	 * about dysons 
	 * 
	 * @return always a valid {@link Map} instance, never <code>null</code>.
	 */
	Map<String, String> getRuntimeInformation();

}//interface DysonStatistics
