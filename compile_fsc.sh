#!/bin/sh
echo "Compiling..."
fsc -cp libraries/ScissLib.jar:libraries/ScissDSP.jar:libraries/ScalaOSC.jar:libraries/ScalaCollider.jar:libraries/MRJAdapter.jar -d build/classes/ -sourcepath src/ src/de/sciss/trees/*.scala src/de/sciss/kontur/*.scala src/de/sciss/kontur/edit/*.scala src/de/sciss/kontur/gui/*.scala src/de/sciss/kontur/io/*.scala src/de/sciss/kontur/session/*.scala src/de/sciss/kontur/util/*.scala
sh makejar.sh
echo "Done."