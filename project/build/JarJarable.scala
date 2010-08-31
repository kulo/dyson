import java.io.File
import sbt._
import Process._

/**
 * TODO: we should depend on the emarsys project and use this class from 
 * there
 */
trait JarJarable extends BasicScalaProject {
    
    val toolsConfig = config("tools")
    val jarjarjarjar = "jarjar" % "jarjar" % "1.0" % "tools->default"
 
    def jarjarOutputFile = "jj-" + jarPath.asFile.getName

    val jarjarClasspathString = Path.makeString( managedClasspath( toolsConfig ).get )
 
    def includeScalaInJarjar = false
 
    /**
     * By default, we're going to take everything in the compile dependencies, plus
     * the scala library. We have to filter out some libraries due to manifest signing
     * problems, however.
     */
    def jarjarZipFilesets : Seq[ File ] = {

        // Filter out target folders and Bouncycastle Jars.
        def safeJarFile( f : File ) : Boolean = {
            f.getName.endsWith("ar") && ! f.getName.startsWith("bcpg-") &&
            ! f.getName.startsWith("bcprov-")
        }
        
        val deps = compileClasspath.getFiles.filter( safeJarFile )
        
        var files = deps.toList
        // files = jarPath.asFile :: files
        if ( includeScalaInJarjar )
            files = mainDependencies.scalaLibrary.get.elements.next.asFile :: files
        log.debug( Console.GREEN + "files = " + files + Console.RESET )

        files
    }
    
    /**
     * Basically, the parts of the compile dependencies that are not jar files. This
     * should likely be everything built locally.
     */
    def jarjarDirs : Seq[ File ] = {
        compileClasspath.getFiles.filter( _.isDirectory ).toList ::: 
        mainResources.getFiles.filter( _.isDirectory ).toList
    }
    
    /**
     * Constructs a default build.xml for running the jarjar task in an external
     * command. We just use the "zipfileset" to concatenate all dependencies together.
     */
    def jarjarBuildXML = {
        <project name="jarjar" default="jar" basedir=".">
            <target name="jar">
                <taskdef name="jarjar"
                    classname="com.tonicsystems.jarjar.JarJarTask"
                    classpath={ jarjarClasspathString }/>
                <jarjar jarfile={ jarjarOutputFile }>
                    { jarjarDirs.map( d => <fileset dir={ d.toString } /> ) }
                    { jarjarZipFilesets.map( f => <zipfileset src={ f.toString } /> ) }
                </jarjar>
            </target>
        </project>
    }
 
    private class JarjarCommandBuilder extends Runnable with FileExtensions {

        def outputJarjarPath = outputPath / "jarjar"

        var command : ProcessBuilder = _

        def run {
            val outputJarjarFile = outputJarjarPath.asFile
            if ( ! outputJarjarFile.exists ) outputJarjarFile.mkdirs
            ( outputJarjarPath / "build.xml" ).asFile.write(
                jarjarBuildXML.toString.getBytes("UTF-8")
            )
            val processBuilder = new java.lang.ProcessBuilder( "ant" )
            command = processBuilder.directory( outputJarjarPath.asFile )
        }
    }
 
    /**
     * How does one jarjar? First, you run the package task. The "jarjar" task should
     * run after that task.
     *
     * <ul>
     * <li> Create a target/jarjar directory </li>
     * <li> Generate a <code>build.xml</code> in that directory that references all the
     *    build file. </li>
     * <li> Call ant in that directory. </li>
     * </ul>
     */
    lazy val jarjar = jarjarAction

    def jarjarAction = task {
        val builder = new JarjarCommandBuilder
        builder.run
        val exitCode = builder.command ! log
        if ( exitCode == 0 ) None else Some( exitCode.toString )
    }
}