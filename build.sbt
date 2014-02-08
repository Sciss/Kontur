import AssemblyKeys._

name           := "Kontur"

version        := "1.2.0-SNAPSHOT"

organization   := "de.sciss"

scalaVersion   := "2.10.3"

description    := "An extensible multi-track audio editor based on ScalaCollider"

homepage       := Some(url("https://github.com/Sciss/" + name.value))

licenses       := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

libraryDependencies ++= Seq(
  "de.sciss" %% "scalacolliderswing" % "1.13.+",
  "de.sciss" %% "span"               % "1.2.+",
  "de.sciss" %% "sonogramoverview"   % "1.7.+",
  "de.sciss" %% "desktop-mac"        % "0.4.+"
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

publishTo :=
  Some(if (version.value endsWith "-SNAPSHOT")
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

test in assembly := ()

seq(appbundle.settings: _*)

appbundle.icon := {
  val base      = (resourceDirectory in Compile).value
  val sub       = organization.value.split('.')
  val n         = name.value.toLowerCase
  val iconFile  = (base /: sub)(_ / _) / n / "application.png"
  Some(iconFile)
}

appbundle.javaOptions ++= Seq("-ea", "-Xmx2048m")

appbundle.target   := baseDirectory.value

appbundle.documents += {
  // XXX TODO: DRY
  val base      = (resourceDirectory in Compile).value
  val sub       = organization.value.split('.')
  val n         = name.value
  val nl        = n.toLowerCase
  val iconFile  = (base /: sub)(_ / _) / nl / "document.png"
  appbundle.Document(
    name       = s"$n Document",
    role       = appbundle.Document.Editor,
    icon       = Some(iconFile),
    extensions = Seq(nl),
    isPackage  = true
  )
}

target in assembly := baseDirectory.value

jarName in assembly := s"${name.value}.jar"

// ---- ls.implicit.ly ----

seq(lsSettings :_*)

(LsKeys.tags   in LsKeys.lsync) := Seq("audio", "multitrack", "music", "daw")

(LsKeys.ghUser in LsKeys.lsync) := Some("Sciss")

(LsKeys.ghRepo in LsKeys.lsync) := Some(name.value)

