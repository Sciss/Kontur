resolvers += Resolver.url( "sbt-plugin-releases",
   url( "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/" ))( Resolver.ivyStylePatterns )

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com")

resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"

// addSbtPlugin( "com.github.mpeltonen" % "sbt-idea" % "1.0.0" )

addSbtPlugin( "me.lessis" % "ls-sbt" % "0.1.1" )

addSbtPlugin( "com.eed3si9n" % "sbt-assembly" % "0.8.3" )

addSbtPlugin( "de.sciss" % "sbt-appbundle" % "0.14" )
