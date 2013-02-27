## Kontur

### statement

Kontur is (C)opyright by 2004-2013 Hanns Holger. All rights reserved. It is released under the [GNU General Public License](http://github.com/Sciss/Kontur/blob/master/licenses/Kontur-License.txt). Kontur is a multitrack audio editor based on ScalaCollider. It provides programmatic (REPL) access to its session elements, allowing for algorithmic manipulation in the Scala programming language.

### requirements / installation

Builds with sbt 0.12 and compiles against Scala 2.10 and Java 1.6. Depends on ScalaCollider-Swing. sbt should be able to pull these dependencies from the net or your local ivy2 folder. Three libraries (ScissDSP, ScissLib and MRJAdapter) are now included as binaries in `lib` for simplicity.

`sbt compile` will then build the project, and `sbt appbundle` will update the OS X application bundle, while `sbt assembly` creates a self-contained double-clickable jar (which you can use on platforms other than OS X)

The create the API documentation, run `sbt doc`.

### creating an IntelliJ IDEA project

To develop the sources, if you haven't globally installed the sbt-idea plugin yet, create the following contents in `~/.sbt/plugins/build.sbt`:

    addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")

Then to create the IDEA project, run `sbt gen-idea`.

### download

The current version can be downloaded from [github.com/Sciss/Kontur](http://github.com/Sciss/Kontur).

