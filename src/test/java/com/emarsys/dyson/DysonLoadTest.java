package com.emarsys.dyson;

import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.emarsys.ecommon.concurrent.Threads;
import com.emarsys.ecommon.mail.test.SmtpBomber;
import com.emarsys.ecommon.test.TestData;
import com.emarsys.ecommon.time.Dates;


/**
 * Load testing unit test case.
 * 
 * @author <a href="mailto:kulovits@emarsys.com">Michael "kULO" Kulovits</a>
 */
@Test
public class DysonLoadTest
{
	public static final boolean CLEAN_STORAGE = true;
	
	private static Logger log = LoggerFactory.getLogger( DysonLoadTest.class );

	private DysonServer server;
	private SmtpBomber bomber;
	
	protected void createBomberAndStartDyson( Properties props ) throws IOException
	{
		//overwrite existing system properties with passed props
		System.getProperties().putAll( props );

		this.server = new DysonServer();
		this.bomber = new SmtpBomber( this.server.getJMailSession(), props );
		
		assertNotNull( this.server );
		assertNotNull( this.bomber );
		
		assert !this.server.isRunning();

		this.cleanStorage();
		this.server.start();

		assert this.server.isRunning();
	}
	
	@AfterMethod
	public void stopServer() throws IOException
	{
		if( this.server != null )
		{
			this.cleanStorage();
			this.server.stop();
			//await termination of storage service
			this.server.getStorage().awaitTermination( 5, TimeUnit.SECONDS );
		}
	}
	
	protected void cleanStorage() throws IOException
	{
		if( CLEAN_STORAGE )
		{
			this.server.getStorage().clearIncomingDir();
			this.server.getStorage().clearProcessedDir();
		}
	}
	
	/**
	 * provides {@link Properties} which configure 
	 * an {@link SmtpBomber} as well as a {@link DysonServer}
	 * which should be bombed.
	 */
	@DataProvider( name = "dysonAndBomberProperties" )
	public Object[][] getDysonAndBomberPropertiesData()
	{
		TestData data = new TestData();
		
		Properties props;

		//#1
		props = new Properties();
		props.setProperty( DysonConfig.REST_SERVER_PORT, "11080" );
		props.setProperty( DysonConfig.SMTP_PORT, "11025" );
		props.setProperty( DysonConfig.SMTP_MAX_CONNECTIONS, "101" );
		props.setProperty( DysonConfig.SMTP_DISCARD_RECIPIENTS_ENABLED, "false" );
		props.setProperty( DysonConfig.STORAGE_DIR_INCOMING, "./tmp/1/incoming" );
		props.setProperty( DysonConfig.STORAGE_DIR_PROCESSED, "./tmp/1/processed" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_NBR_OF_MAILS, "100" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_MAIL_TEXT_SIZE, "1000" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_SEND_DELAY_MILLIS, "0" );
		data.addParams( props );
		
		//#2
		props = new Properties();
		props.setProperty( DysonConfig.REST_SERVER_PORT, "12080" );
		props.setProperty( DysonConfig.SMTP_PORT, "12025" );
		props.setProperty( DysonConfig.SMTP_MAX_CONNECTIONS, "10" ); 
		props.setProperty( DysonConfig.SMTP_DISCARD_RECIPIENTS_ENABLED, "false" );
		props.setProperty( DysonConfig.STORAGE_DIR_INCOMING, "./tmp/2/incoming" );
		props.setProperty( DysonConfig.STORAGE_DIR_PROCESSED, "./tmp/2/processed" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_NBR_OF_MAILS, "9" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_MAIL_TEXT_SIZE, "1000" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_SEND_DELAY_MILLIS, "0" );
		data.addParams( props );
		
		//#3 
		props = new Properties();
		props.setProperty( DysonConfig.REST_SERVER_PORT, "13080" );
		props.setProperty( DysonConfig.SMTP_PORT, "13025" );
		props.setProperty( DysonConfig.SMTP_MAX_CONNECTIONS, "10000" ); 
		props.setProperty( DysonConfig.STORAGE_DIR_INCOMING, "./tmp/3/incoming" );
		props.setProperty( DysonConfig.STORAGE_DIR_PROCESSED, "./tmp/3/processed" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_NBR_OF_MAILS, "1000" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_MAIL_TEXT_SIZE, "10" );
		props.setProperty( SmtpBomber.SMTP_BOMBER_SEND_DELAY_MILLIS, "0" );
		data.addParams( props );
		
		return data.toArray();
	}
	
	@Test( dataProvider = "dysonAndBomberProperties", enabled = true,
			invocationCount = 1 )
	public void testLoadScenario( Properties props ) throws IOException
	{
		//initialize dyson server and smtp bomber for this testrun
		this.createBomberAndStartDyson( props );
		
		//send mails and await termination of sending sevice
		this.bomber.sendTestMails( true );

		this.waitForMailsToBeProcessed();
		
		//log runtime information
		log.info( "dyson runtime information:" );
		for( Entry<String, String> entry : 
			this.server.getRuntimeInformation().entrySet() )
		{
			log.info( entry.toString() );
		}
		
		//check number of files
		int processedFileCount = 
			this.server.getStorage().getProcessedMailFiles().size();
		int incomingFileCount = 
			this.server.getStorage().getIncomingMailFiles().size();
		
		log.info( "# incoming files = {}", incomingFileCount );
		log.info( "# processed files = {}", processedFileCount );
		log.info( "# mails produced = {}", this.bomber.getProducedMailCount() );
		log.info( "# mails sent = {}", this.bomber.getSentMailCount() );
		
		int nbrOfMails = this.bomber.getConfiguration().get( 
				SmtpBomber.SMTP_BOMBER_NBR_OF_MAILS ).getIntValue();
		Assert.assertEquals( 
				this.bomber.getProducedMailCount(), nbrOfMails );
		Assert.assertEquals( this.bomber.getSentMailCount(), nbrOfMails);
		Assert.assertEquals( incomingFileCount, 0 );
		Assert.assertEquals( processedFileCount, this.bomber.getSentMailCount() );
	}

	protected void waitForMailsToBeProcessed() 
	{
		//TODO get proper state handling in dyson storage in order to be able to wait for it to finish processing
		int sleepTime = this.bomber.getConfiguration().get(
				SmtpBomber.SMTP_BOMBER_NBR_OF_MAILS ).getIntValue() * 10;
		if( sleepTime / Dates.SECOND_IN_MILLIS <= 3 )
		{
			sleepTime = Dates.SECOND_IN_MILLIS * 3;
		}

		log.info( "waiting {} seconds for mails to be processed", 
				sleepTime / Dates.SECOND_IN_MILLIS );
		Threads.sleepSilently( sleepTime );
	}
	
}//class DysonLoadTest
