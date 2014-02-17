import AssemblyKeys._

name           := "Kontur"

version        := "1.2.1"

organization   := "de.sciss"

scalaVersion   := "2.10.3"

description    := "An extensible multitrack audio editor based on ScalaCollider"

homepage       := Some(url("https://github.com/Sciss/" + name.value))

licenses       := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

libraryDependencies ++= Seq(
  "de.sciss" %% "scalacolliderswing" % "1.13.+",
  "de.sciss" %% "span"               % "1.2.+",
  "de.sciss" %% "scissdsp"           % "1.2.+",
  "de.sciss" %  "scisslib"           % "1.0.0",
  "org.scala-lang" % "scala-actors" % scalaVersion.value
)

retrieveManaged := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

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

publishTo <<= version { (v: String) =>
  Some( if( v.endsWith( "-SNAPSHOT" ))
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra <<= name { n =>
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

test in assembly := {}

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

// ---- ls.implicit.ly ----

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) := Seq("audio", "multitrack", "music", "daw")

(LsKeys.ghUser in LsKeys.lsync) := Some("Sciss")

(LsKeys.ghRepo in LsKeys.lsync) <<= name(Some(_))

