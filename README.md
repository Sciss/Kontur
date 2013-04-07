# Kontur

## statement

Kontur is (C)opyright by 2004-2013 Hanns Holger. All rights reserved. It is released under the [GNU General Public License](http://github.com/Sciss/Kontur/blob/master/licenses/Kontur-License.txt). Kontur is a multitrack audio editor based on ScalaCollider. It provides programmatic (REPL) access to its session elements, allowing for algorithmic manipulation in the Scala programming language.

## requirements / installation

Builds with sbt 0.12 and compiles against Scala 2.10 and Java 1.6.

`sbt compile` will then build the project, and `sbt appbundle` will update the OS X application bundle, while `sbt assembly` creates a self-contained double-clickable jar (which you can use on platforms other than OS X)

The create the API documentation, run `sbt doc`.
