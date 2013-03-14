import AssemblyKeys._

name           := "Kontur"

version        := "1.2.0-SNAPSHOT"

organization   := "de.sciss"

scalaVersion   := "2.10.0"

description    := "An extensible multitrack audio editor based on ScalaCollider"

homepage      <<= name { n => Some(url("https://github.com/Sciss/" + n)) }

licenses       := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

libraryDependencies ++= Seq(
  "de.sciss" %% "scalacolliderswing" % "1.5.+",
  "de.sciss" %% "span" % "1.1.+",
  "de.sciss" %% "scissdsp" % "1.1.+"
//  "de.sciss" % "scisslib" % "0.15"
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

appbundle.target <<= baseDirectory

// ---- ls.implicit.ly ----

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) := Seq("audio", "multitrack", "music", "daw")

(LsKeys.ghUser in LsKeys.lsync) := Some("Sciss")

(LsKeys.ghRepo in LsKeys.lsync) <<= name(Some(_))

