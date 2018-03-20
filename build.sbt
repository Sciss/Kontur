name               := "Kontur"
version            := "1.3.0-SNAPSHOT"
organization       := "de.sciss"
scalaVersion       := "2.11.12"
crossScalaVersions := Seq("2.11.12", "2.10.7")
description        := "An extensible multitrack audio editor based on ScalaCollider"
homepage           := Some(url("https://github.com/Sciss/" + name.value))
licenses           := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

libraryDependencies ++= Seq(
  "de.sciss" %% "scalacolliderswing-interpreter" % "1.34.1",
  "de.sciss" %% "span"               % "1.3.3",
  "de.sciss" %% "scissdsp"           % "1.2.3",
  "de.sciss" %  "scisslib"           % "1.1.1",
  "org.scala-lang" % "scala-actors" % scalaVersion.value
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")

// ---- build info ----


enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
  BuildInfoKey.map(homepage) { case (k, opt) => k -> opt.get },
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

// ---- assembly ----
test            in assembly := {}
target          in assembly := baseDirectory.value
assemblyJarName in assembly := s"${name.value}.jar"
assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.last
  case PathList("org", "xmlpull", xs @ _*)              => MergeStrategy.first
  case PathList("org", "w3c", "dom", "events", xs @ _*) => MergeStrategy.first // bloody Apache Batik
  case x =>
    val old = (assemblyMergeStrategy in assembly).value
    old(x)
}

