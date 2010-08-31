import java.io._
import scala.util.logging.Logged
/** 
 * TODO: we should depend on the emarsys project and use this class from 
 * there 
 */ 	
trait FileExtensions extends Logged {

    /** Throw an IOException, sometimes happens with these file system utils. */
    def ioError( msg : String ) = throw new IOException( msg )
    
    def createTempDirectory : File = {
        val temp = File.createTempFile("temp", System.nanoTime.toString )
        if( ! temp.delete )
                ioError("Could not delete temp file: " + temp.getAbsolutePath )
        if( ! temp.mkdir )
                ioError("Could not create temp directory: " + temp.getAbsolutePath )
        return temp
    }

    class ScriptingFile( file : File ) {

        def write( bytes : Array[ Byte ] ) {
            val output = new BufferedOutputStream( new FileOutputStream( file ) )
            try{ output.write( bytes, 0, bytes.length ) }
            finally { output.close }
        }
    
        def deleteAll {
            def doAllDelete( fsitem : File ) {
                if ( fsitem.isDirectory ) fsitem.listFiles.foreach( doAllDelete )
                fsitem.delete
            }
            doAllDelete( file )
        }

        def copyTo ( target : File ) {
            val bufSize = 131072
            val input = new BufferedInputStream( new FileInputStream( file ) )
            try {
                val output =
	                    new BufferedOutputStream( new FileOutputStream( target ), bufSize )
	                try {
	                    val bytes = new Array[ Byte ]( bufSize )
	                    var read = -1
	                    do {
	                        read = input.read( bytes, 0, bufSize )
	                        if ( read != -1 )
	                            output.write( bytes, 0, read )
	                    } while ( read != -1 )
	                }
	                finally { output.close }
	            }
	            finally { input.close }
	        }
	    
	        lazy val bytes : Array[ Byte ] = {
	            val inputStream = new BufferedInputStream( new FileInputStream( file ) )
	            try {
	                val bytes = new Array[ Byte ]( file.length.toInt )
	                inputStream.read( bytes )
	                bytes
	            }
	            finally { inputStream.close }
	        }
	
	        def text : String =
	            try { scala.io.Source.fromFile( file, "UTF-8" ).getLines.mkString("") }
	            catch { case th : Throwable => log( "Problem loading " + file ); throw th }
	    
	        def - ( rhs : File ) : String = {
	            if ( file.toString.length == rhs.toString.length ) return ""
	            assume( file.toString.length > rhs.toString.length,
	                            rhs + " is not a parent directory of " + file )
	            file.toString.substring( rhs.toString.length )
	        }
	        
	        def extension : String = {
	            val idx = file.getName.lastIndexOf('.')
	            if ( idx == -1 ) file.getName
	            else file.getName.substring( idx )
	        }
	        
	        def basename : String = {
	            val idx = file.getName.lastIndexOf('.')
	            if ( idx == -1 ) file.getName
	            else file.getName.substring( 0, idx )
	        }
	
	        def / ( child : String ) : File = {
	            if ( file.toString.isEmpty ) new File( child )
	            else new File( file, child )
	        }
	    
	        def mirrorTo( targetPath : File ) = new FileMirrorer( file, targetPath )
	    }
	
	    implicit def ScriptingFile( f : File ) = new ScriptingFile( f )
	
	    /**
	        Copy, and preserve the subdirectory of the sourceFile in relation to this 
	        sourceFolder in targetFolder.
	    */              
	    class FileMirrorer( sourceFile : File, targetFolder : File ) {
	
	        def from( sourceFolder : File ) {
	            val subpath = sourceFile - sourceFolder
	            val targetPath = new File( targetFolder, subpath )
	            if ( ! targetPath.getParentFile.exists ) targetPath.getParentFile.mkdirs
	            sourceFile copyTo targetPath
	        }
	    }
	
	    class ScriptingString( string : String ) {
	        def f : File = new File( string )
	        /**
	         * For some reason the java pathSeparatorChar does not work consistently.
	         */
	        def slashes : Int = string.split( '/' ).length - 1
	    }
	    
	    implicit def ScriptingString( s : String ) = new ScriptingString( s )
}