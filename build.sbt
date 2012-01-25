import AssemblyKeys._

name           := "Kontur"

version        := "0.17-SNAPSHOT"

organization   := "de.sciss"

scalaVersion   := "2.9.1"

libraryDependencies ++= Seq(
   "de.sciss" %% "scalacolliderswing" % "0.32-SNAPSHOT"
// crappy sbt kriegt es nicht gebacken
//   "de.sciss" % "scissdsp" % "0.11" from "https://github.com/downloads/Sciss/ScissDSP/scissdsp-0.11.jar"
)

retrieveManaged := true

scalacOptions ++= Seq( "-deprecation", "-unchecked" )

// ---- publishing ----

publishTo <<= version { (v: String) =>
   Some( "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/".+(
      if( v.endsWith( "-SNAPSHOT")) "snapshots/" else "releases/"
   ))
}

pomExtra :=
<licenses>
  <license>
    <name>GPL v2+</name>
    <url>http://www.gnu.org/licenses/gpl-2.0.txt</url>
    <distribution>repo</distribution>
  </license>
</licenses>

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// ---- packaging ----

seq( assemblySettings: _* )

test in assembly := {}

seq( appbundle.settings: _* )

appbundle.icon := Some( file( "application.icns" ))

appbundle.javaOptions += "-ea"
