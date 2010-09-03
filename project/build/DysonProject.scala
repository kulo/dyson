import sbt._
	
	class   DysonProject( info : ProjectInfo ) 
	extends DefaultProject(info)
	with    TestNGTestingProject 
	with    Eclipsify
        with    JarJarable 
        {
	    
	    val emarsysNexus = "emarsys Nexus" at "http://nexus.emarsys.com/content/groups/public"
	    
	    val commons_collections = "commons-collections" % "commons-collections" % "3.2"
	    val commons_lang = "commons-lang" % "commons-lang" % "2.4"
	    val commons_logging = "commons-logging" % "commons-logging" % "1.1.1" 
	    val commons_io = "commons-io" % "commons-io" % "1.4"
	    val mina_filter = "org.apache.mina" % "mina-filter-ssl" % "1.1.7"
	    val mina_core = "org.apache.mina" % "mina-core" % "1.1.7"
	    val mina_integration_jmx = "org.apache.mina" % "mina-integration-jmx" % "1.1.7"
	
	    val mail = "javax.mail" % "mail" % "1.4.2"
	    val activation = "javax.activation" % "activation" % "1.1.1"
	    
	    val slf4j_api = "org.slf4j" % "slf4j-api" % "1.5.5"
	    val slf4j_log4j12 = "org.slf4j" % "slf4j-log4j12" % "1.5.5"
	    val slf4j_jcl = "org.slf4j" % "slf4j-jcl" % "1.5.5"
	
	    val subethasmtp = "org.subethamail" % "subethasmtp" % "2.1.0"
	    
	    val org_restlet = "org.restlet" % "org.restlet" % "1.1.4"
	    val com_noelios_restlet = "com.noelios.restlet" % "com.noelios.restlet" % "1.1.4"  
	    
	    //val testng = "org.testng" % "testng" % "5.8"  % "test" classifier "jdk15" 
}
