import AssemblyKeys._

name           := "Kontur"

version        := "1.3.0-SNAPSHOT"

organization   := "de.sciss"

scalaVersion   := "2.11.12"

crossScalaVersions := Seq("2.11.12", "2.10.7")

description    := "An extensible multitrack audio editor based on ScalaCollider"

homepage       := Some(url("https://github.com/Sciss/" + name.value))

licenses       := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

libraryDependencies ++= Seq(
  "de.sciss" %% "scalacolliderswing-interpreter" % "1.34.1",
  "de.sciss" %% "span"               % "1.3.3",
  "de.sciss" %% "scissdsp"           % "1.2.3",
  "de.sciss" %  "scisslib"           % "1.1.1",
//  "de.sciss" %% "swingplus"          % "0.2.4",
  "org.scala-lang" % "scala-actors" % scalaVersion.value
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")

// ---- build info ----

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
   BuildInfoKey.map(homepage) { case (k, opt)             => k -> opt.get },
   BuildInfoKey.map(licenses) { case (_, Seq( (lic, _) )) => "license" -> lic }
)

buildInfoPackage := "de.sciss.kontur"

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some( if( version.value endsWith "-SNAPSHOT" )
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
}

// ---- packaging ----

seq(assemblySettings: _*)

test in assembly := ()

seq(appbundle.settings: _*)

appbundle.icon := Some(file("application.icns"))

appbundle.javaOptions ++= Seq("-ea", "-Xmx2048m")

appbundle.target := baseDirectory.value

target in assembly := baseDirectory.value

jarName in assembly := s"${name.value}.jar"

//mergeStrategy in assembly <<= (mergeStrategy in assembly) { old => {
//  case PathList("AddAction.class") => MergeStrategy.first   // problem with that ScalaCollider version
//  case x => old(x)
//}}

