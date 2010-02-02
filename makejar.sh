#!/bin/sh
echo "Archiving..."
jar cfm Kontur.jar ManualManifest.mf -C build/classes/ .
jar uf Kontur.jar -C resources/ .
echo "Done."