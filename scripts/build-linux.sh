#!/bin/bash

set -e

mvn clean package

jpackage \
  --type deb \
  --name AndroidToolkit \
  --app-version 1.0.0 \
  --input target \
  --main-jar android-toolkit-1.0-SNAPSHOT.jar \
  --main-class app.androidtoolkit.Launcher \
  --dest dist \
  --linux-shortcut \
  --linux-menu-group "Development" \
  --java-options "--enable-native-access=ALL-UNNAMED"