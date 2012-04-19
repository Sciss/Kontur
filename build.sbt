import AssemblyKeys._

name           := "Kontur"

version        := "0.19"

organization   := "de.sciss"

scalaVersion   := "2.9.2"

description := "An extensible multitrack audio editor based on ScalaCollider"

homepage := Some( url( "https://github.com/Sciss/Kontur" ))

licenses := Seq( "GPL v2+" -> url( "http://www.gnu.org/licenses/gpl-2.0.txt" ))

resolvers += "Clojars Repository" at "http://clojars.org/repo"  // for jsyntaxpane

libraryDependencies ++= Seq(
   "de.sciss" %% "scalacolliderswing" % "0.34",
   "de.sciss" % "scissdsp" % "0.11", // from "http://scala-tools.org/repo-releases/de/sciss/scissdsp/0.11/scissdsp-0.11.jar"
   "de.sciss" % "scisslib" % "0.15"
)

retrieveManaged := true

scalacOptions ++= Seq( "-deprecation", "-unchecked" )

// ---- publishing ----

publishMavenStyle := true

publishTo <<= version { (v: String) =>
   Some( if( v.endsWith( "-SNAPSHOT" ))
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
<scm>
  <url>git@github.com:Sciss/Kontur.git</url>
  <connection>scm:git:git@github.com:Sciss/Kontur.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>

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
