#!/usr/bin/env bash
# Wird automatisch von devcontainer.json (postCreateCommand) ausgeführt.
# Installiert Android SDK Command-Line-Tools + benötigte Pakete,
# macht gradlew ausführbar, legt local.properties an.
set -e

echo "== gradlew ausführbar machen =="
chmod +x gradlew

echo "== Android SDK Command-Line-Tools installieren =="
export ANDROID_HOME="$HOME/android-sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  curl -fsSL -o /tmp/cmdline-tools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm /tmp/cmdline-tools.zip
fi

echo "== Lizenzen akzeptieren =="
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null

echo "== SDK-Pakete installieren (platform-tools, android-34, build-tools 34.0.0) =="
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null

echo "== local.properties anlegen =="
echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "== Dauerhaft in ~/.bashrc eintragen =="
if ! grep -q "ANDROID_HOME" "$HOME/.bashrc" 2>/dev/null; then
  echo "export ANDROID_HOME=$ANDROID_HOME" >> "$HOME/.bashrc"
  echo "export PATH=\$PATH:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/cmdline-tools/latest/bin" >> "$HOME/.bashrc"
fi

echo "== Fertig. Build testen mit: ./gradlew assembleDebug --no-daemon =="
