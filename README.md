# Kontur

[![Flattr this](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=sciss&url=https%3A%2F%2Fgithub.com%2FSciss%2FKontur&title=Kontur%20Multitrack%20Editor&language=Scala&tags=github&category=software)

## statement

Kontur is (C)opyright by 2004&ndash;2014 Hanns Holger. All rights reserved. It is released under the [GNU General Public License](http://github.com/Sciss/Kontur/blob/master/licenses/Kontur-License.txt). Kontur is a multi-track audio editor based on ScalaCollider. It provides programmatic (REPL) access to its session elements, allowing for algorithmic manipulation in the Scala programming language.

__Note:__ This project is now largely superseded by [Mellite](http://github.com/Sciss/Mellite). Almost all of Kontur's functionality is available there. A little utility [ConvertKonturToMellite](https://github.com/Sciss/ConvertKonturToMellite) should make it easy to convert Kontur sessions to Mellite workspaces.

## requirements / installation

Builds with sbt 0.13 and compiles against Scala 2.11 and 2.10. Depends on ScalaCollider-Swing. sbt should be able to pull these dependencies from the net or your local ivy2 folder. The MRJAdapter library is included as binary in the `lib` folder for simplicity.

`sbt compile` will then build the project, and `sbt appbundle` will update the OS X application bundle, while `sbt assembly` creates a self-contained double-clickable jar (which you can use on platforms other than OS X)

The create the API documentation, run `sbt doc`.

## download

The current version can be downloaded from [github.com/Sciss/Kontur](http://github.com/Sciss/Kontur).

## documentation

A short screencast is available on [Vimeo](https://vimeo.com/86199640).
