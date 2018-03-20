# Kontur

## statement

Kontur is (C)opyright by 2004&ndash;2018 Hanns Holger. All rights reserved. It is released under the [GNU General Public License](http://github.com/Sciss/Kontur/blob/master/licenses/Kontur-License.txt). Kontur is a multi-track audio editor based on ScalaCollider. It provides programmatic (REPL) access to its session elements, allowing for algorithmic manipulation in the Scala programming language.

__Note:__ This project is now largely superseded by [Mellite](http://github.com/Sciss/Mellite). Almost all of Kontur's functionality is available there. A little utility [ConvertKonturToMellite](https://github.com/Sciss/ConvertKonturToMellite) should make it easy to convert Kontur sessions to Mellite workspaces.

## requirements / installation

Builds with sbt 0.13 and compiles against Scala 2.11 and 2.10. Requires JDK 8. Depends on ScalaCollider-Swing. sbt should be able to pull these dependencies from the net or your local ivy2 folder. The MRJAdapter library is included as binary in the `lib` folder for simplicity.

`sbt compile` will then build the project, and `sbt appbundle` will update the OS X application bundle, while `sbt assembly` creates a self-contained double-clickable jar (which you can use on platforms other than OS X)

The create the API documentation, run `sbt doc`.

## download

The current version can be downloaded from [github.com/Sciss/Kontur](http://github.com/Sciss/Kontur).

## documentation

A short screencast is available on [Vimeo](https://vimeo.com/86199640).
