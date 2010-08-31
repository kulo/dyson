import sbt._
import sbt.Process._

/**
 * Includes the <code>testng</code> task into the project, which depends upon the 
 * testng.xml file in the current working directory.
 *
 * Note that this continues to have problems when finding files out on the class path.
 */
trait TestNGTestingProject extends DefaultProject
{

    val test_ng = "org.testng" % "testng" % "5.9" % "test->default" intransitive()

    lazy val testng = task {
        
        import java.io.File
        import sbt.FileUtilities._
        
        val pathElements = {
            configurationPath( Configurations.Compile ).descendentsExcept( "*.jar", ".svn" ).getPaths ++
            configurationPath( Configurations.Test ).descendentsExcept( "*.jar", ".svn" ).getPaths ++
            List(
                outputPath / "classes",
                outputPath / "test-classes",
                scalaLibraryJar.getPath,
                testResourcesPath
            )
        }
        val classpath = pathElements.mkString( ":" )
        val testDir = outputPath / "testng"

        createDirectory( testDir, log )
        
        log.debug( "java -cp " + classpath + " org.testng.TestNG -d " + testDir / "test-output" + " testng.xml" )
        
        var code = (
            ( "java -cp " + classpath + " org.testng.TestNG -d " + testDir / "test-output" + " testng.xml" ! log ) +
            ( "ant -f src/test/resources/build.xml -Dbasedir=" + testDir ! log )
        )
        
        if ( code > 0 ) Some( code.toString ) else None
        
    } dependsOn( testCompile )
}