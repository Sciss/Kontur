THIS PROJECT IS COVERED BY THE GNU GENERAL PUBLIC LICENSE V2+

PREREQUISITES

Scala 2.8 RC1 / Java 1.6 / the following:

Libraries:
- create a new folder "libraries"
- install into that folder
    ScissLib.jar (http://sourceforge.net/projects/scisslib)
    ScalaOSC.jar (http://github.com/Sciss/ScalaOSC)
    ScalaCollider.jar (http://github.com/Sciss/ScalaCollider)
    MRJAdapter.jar (https://mrjadapter.dev.java.net/)
    jsyntaxpane-0.9.5-b29.jar (http://code.google.com/p/jsyntaxpane/)

Open the project in IntellJ IDEA Community Edition 9

alternatively, running "compile_fsc.sh" should work.

MAC OS X APP BUNDLE

do something like this:

$ mkdir Kontur.app/Contents/Resources/Java
for each library:
$ ln -s library/... Kontur.app/Contents/Resources/Java/...
$ ./makejar.sh
$ ln -s Kontur.jar Kontur.app/Contents/Resources/Java/Kontur.jar
