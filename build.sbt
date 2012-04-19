import AssemblyKeys._

name           := "Kontur"

version        := "0.18"

organization   := "de.sciss"

scalaVersion   := "2.9.1"

description := "An extensible multitrack audio editor based on ScalaCollider"

homepage := Some( url( "https://github.com/Sciss/Kontur" ))

licenses := Seq( "GPL v2+" -> url( "http://www.gnu.org/licenses/gpl-2.0.txt" ))

resolvers += "Clojars Repository" at "http://clojars.org/repo"  // for jsyntaxpane

libraryDependencies ++= Seq(
   "de.sciss" %% "scalacolliderswing" % "0.32",
   "de.sciss" % "scissdsp" % "0.11" from "http://scala-tools.org/repo-releases/de/sciss/scissdsp/0.11/scissdsp-0.11.jar"
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

appbundle.javaOptions ++= Seq( "-ea", "-Xmx2048m" )

// ---- disable scaladoc generation during development phase ----

// publishArtifact in (Compile, packageDoc) := false

// ---- ls.implicit.ly ----

seq( lsSettings :_* )

(LsKeys.tags in LsKeys.lsync) := Seq( "audio", "multitrack", "music", "daw" )

(LsKeys.ghUser in LsKeys.lsync) := Some( "Sciss" )

(LsKeys.ghRepo in LsKeys.lsync) := Some( "Kontur" )

// bug in ls -- doesn't find the licenses from global scope
(licenses in LsKeys.lsync) := Seq( "GPL v2+" -> url( "http://www.gnu.org/licenses/gpl-2.0.txt" ))
