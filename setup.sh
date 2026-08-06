#!/usr/bin/env bash
# Einmalig in Codespaces/Gitpod ausführen, falls gradlew/gradlew.bat fehlen.
set -e

if ! command -v gradle &> /dev/null; then
  echo "Gradle nicht gefunden – wird von Codespaces/Gitpod-Image bereitgestellt."
  exit 1
fi

gradle wrapper --gradle-version 8.7
chmod +x gradlew
echo "Gradle Wrapper erzeugt. Build starten mit: ./gradlew assembleDebug"
