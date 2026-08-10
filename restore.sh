#!/usr/bin/env bash
# Baut den kompletten OFFGRID MESH v0.4-beta Projektstand neu auf.
# Ausführen im leeren Repo-Ordner: bash restore.sh
set -e

mkdir -p '.devcontainer'
cat > '.devcontainer/devcontainer.json' << 'OFFGRID_EOF_MARKER'
{
  "name": "OFFGRID MESH – Android Dev",
  "image": "mcr.microsoft.com/devcontainers/java:17",
  "postCreateCommand": "bash .devcontainer/install-android-sdk.sh",
  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "adelphes.android-dev-ext"
      ]
    }
  }
}
OFFGRID_EOF_MARKER

mkdir -p '.devcontainer'
cat > '.devcontainer/install-android-sdk.sh' << 'OFFGRID_EOF_MARKER'
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
OFFGRID_EOF_MARKER

mkdir -p '.'
cat > '.gitignore' << 'OFFGRID_EOF_MARKER'
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build/
/captures/
.externalNativeBuild/
.cxx/
local.properties
app/build/
OFFGRID_EOF_MARKER

mkdir -p '.'
cat > 'LICENSE' << 'OFFGRID_EOF_MARKER'
MIT License

Copyright (c) 2026 Dragons Chain

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
OFFGRID_EOF_MARKER

mkdir -p '.'
cat > 'README.md' << 'OFFGRID_EOF_MARKER'
# OFFGRID MESH (Beta)

Eigenständige Notfall-Kommunikations-App — funktioniert **komplett offline**,
ohne Server, ohne Account, ohne Internet zu irgendeinem Zeitpunkt. Kommunikation
läuft direkt zwischen Smartphones per Bluetooth LE / WiFi-Direct (Mesh,
Multi-Hop-Relay).

Konzept siehe `OFFGRID-MESH-Konzept.docx` (separates Dokument).

## Screens (v0.1 — UI-Grundgerüst)

1. **Karte** – eigene Position + gesehene Nodes (Mock-Daten im Prototyp)
2. **SOS** – Button 3 Sek. gedrückt halten → Notfall-Broadcast (noch ohne echtes Mesh-Backend)
3. **Kanäle** – Familie / Team / Broadcast-Allgemein
4. **Status** – Akku-Modus (Normal/Spar/Ultra-Spar), Node-Anzahl, Reichweiten-Info

## Status: früher Prototyp

- [x] UI-Grundgerüst (Compose, 4 Screens, Bottom Navigation)
- [x] Datenmodelle (Node, Channel, Message)
- [x] `MeshManager`-Interface (Mock-Implementierung, liefert Test-Nodes)
- [x] Echte BLE-Discovery (Android `BluetoothLeScanner` + `BluetoothLeAdvertiser`, `BleMeshManager`)
- [x] Adaptive Scan-Intervalle je Akku-Modus (Normal = Dauerscan, Spar = Balanced, Ultra-Spar = 10s Scan / 75s Pause)
- [ ] `WifiP2pManager`-Anbindung als Fallback/Ergänzung
- [ ] Store-and-Forward-Queue (SQLite/Room)
- [ ] **Routing-Algorithmus: Epidemic Routing** (siehe Abschnitt "Routing über mehrere Nodes")
- [ ] Ende-zu-Ende-Verschlüsselung (X25519 + Double Ratchet + ChaCha20-Poly1305, TOFU-Fingerprint-Abgleich)
- [ ] SOS-Broadcast mit echtem GPS + Multi-Hop-Relay
- [ ] **Node-Modus (Relay, Variante 1)** – bestehendes Altgerät als Fix-Relay, kein UI-Rendering, Dauer-Discovery, Display aus, Betrieb an Powerbank/Solar. Reine Software-Lösung auf gleicher Codebasis, keine Zusatz-Hardware nötig. Siehe Abschnitt "Reichweite & Node-Modus".
- [ ] **App-Verbreitung per QR + WiFi-Direct** (siehe Abschnitt "App-Verbreitung ohne Internet")
- [ ] Echtes App-Icon
- [ ] F-Droid Metadata

## Reichweite & Node-Modus

**Grundsatz: jeder soll mit vorhandenem Equipment mitmachen können.** Kein
Zwang zu Zusatz-Hardware. Wer ein Fix-Relay aufbauen will, nutzt ein
bestehendes Altgerät (Phone/Tablet) mit der gleichen App im **Node-Modus**:

- Kein UI-Rendering, Display aus — spart am meisten Akku
- Discovery läuft dauerhaft, kein Batteriesparmodus (das ist der einzige Job des Node)
- Betrieb an Powerbank oder kleiner Solar-Zelle möglich
- Verschlüsselung bleibt Ende-zu-Ende zwischen den eigentlichen Nutzern —
  der Node leitet nur verschlüsselte Pakete weiter, kann sie nicht mitlesen

**Zusatz-Hardware (z. B. ESP32 + LoRa für km-Reichweite) ist bewusst
optional** und kommt frühestens in einer späteren Systemversion als
eigenständige Erweiterung — nicht Voraussetzung, um mitzumachen.

## Routing über mehrere Nodes

**Kein Nameserver-/DNS-Prinzip** — das würde einen dauerhaft erreichbaren,
zentralen Knoten voraussetzen, der die komplette Netz-Topologie kennt.
Genau das haben wir nicht: kein Node ist dauerhaft erreichbar, niemand
kennt das Gesamtnetz, die Topologie ändert sich im Minutentakt.

**Stattdessen: Epidemic Routing (Flooding mit TTL)** — Standardansatz bei
Delay Tolerant Networking (DTN), gleiches Grundprinzip wie bei
Bridgefy/Briar:

- Jeder Node hat eine **stabile ID** (geplant: der öffentliche X25519-Key,
  nicht die Bluetooth-MAC — die wird von Android periodisch randomisiert)
- Jede Nachricht bekommt eine eindeutige **Message-ID** + **TTL/Hop-Counter**
  (max. 5–8 Hops)
- Ein Node, der eine neue Nachricht empfängt, speichert sie lokal und
  reicht sie an jeden Node weiter, dem er künftig begegnet — außer an
  Nodes, die sie laut eigener Bestätigung schon haben
- Duplikat-Erkennung über die Message-ID verhindert Endlosschleifen
- Die Nachricht verbreitet sich wie eine Epidemie durchs Netz, bis sie
  ankommt oder die TTL abläuft

**Warum das hier passt statt Overkill zu sein:** reine Textnachrichten
brauchen kaum Bandbreite, Zuverlässigkeit schlägt Effizienz im
Notfall-Szenario, und der geplante Node-Modus (Relay) wird dadurch
richtig wertvoll — ein Fix-Relay mit Dauerbetrieb wirkt im Flooding wie
ein Sammelpunkt und erhöht die Zustellwahrscheinlichkeit deutlich.

Baustein dafür: `RoutingEngine` (Message-ID-Cache, TTL-Verwaltung,
Anbindung an die Store-and-Forward-Queue) — kommt zusammen mit Punkt
"Store-and-Forward-Queue" in der Roadmap, da beides eng zusammenhängt.

## App-Verbreitung ohne Internet

Epidemie lebt von Ansteckung — das gilt auch für die App-Verbreitung
selbst, nicht nur für Nachrichten. Kein Play Store, kein Internet nötig:

- Ein Node startet einen kleinen lokalen HTTP-Server, erreichbar nur im
  eigenen WiFi-Direct-Netz
- QR-Code kodiert die WiFi-Direct-Verbindungsdaten (SSID + Passwort)
- Zweites Gerät scannt den QR-Code → verbindet sich automatisch per
  WiFi-Direct → lädt die APK direkt vom lokalen Server
- Nutzt die gleiche `WifiP2pManager`-Schicht, die ohnehin für's Mesh
  ansteht — kein Fremdkörper, sondern Zweitverwertung

Kurzfristiger Workaround bis dahin: APK per Android-Bordmitteln
(Dateimanager → Teilen → Bluetooth/Nearby Share) weitergeben,
funktioniert schon heute komplett offline.

## Entwicklung (Cloud, kein PC nötig)

Gleiches Setup wie beim Schwesterprojekt
[Rethink Setup Wizard](https://github.com/RethinkWizard/rethink-wizard):

- **Gitpod:** Repo-URL mit `https://gitpod.io/#` davor öffnen im Browser
- **Codespaces:** Auf GitHub → "Code" → "Codespaces" → "Create codespace on main"

Beide installieren automatisch Android SDK + Gradle. Der Gradle-Wrapper
(`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) liegt bereits im Repo —
kein zusätzlicher Setup-Schritt nötig.

### Build & APK erzeugen

```
./gradlew assembleDebug
```

APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

## Architektur-Kurzfassung

- **UI:** Jetpack Compose, Material3, 4 Screens, Bottom Navigation
- **Mesh-Schicht:** `mesh/MeshManager.kt` — Interface, aktuell Mock. Echte
  Implementierung folgt als `BleWifiDirectMeshManager` (BLE + WifiP2pManager
  parallel, automatischer Wechsel je nach Reichweite)
- **Daten:** `data/Models.kt` — `Node`, `Channel`, `Message`. Persistenz via
  Room/SQLite folgt in v0.2
- **Kein Google Play Services, kein Firebase** — F-Droid-kompatibel wie
  Rethink Wizard

## Lizenz

MIT – siehe `LICENSE`.
OFFGRID_EOF_MARKER

mkdir -p 'app'
cat > 'app/build.gradle.kts' << 'OFFGRID_EOF_MARKER'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.dragonschain.offgridmesh"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.dragonschain.offgridmesh"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "0.4-beta"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
}
OFFGRID_EOF_MARKER

mkdir -p 'app/src/main'
cat > 'app/src/main/AndroidManifest.xml' << 'OFFGRID_EOF_MARKER'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Bluetooth Mesh -->
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation"
        tools:targetApi="31" xmlns:tools="http://schemas.android.com/tools" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <!-- Für Android <12 (API <31): alte Bluetooth-Permissions, keine Laufzeit-Abfrage nötig -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />

    <!-- WiFi Direct -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />

    <!-- Standort für SOS + Karte (rein lokal, nie an Server) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.OffgridMesh"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.OffgridMesh">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
OFFGRID_EOF_MARKER

mkdir -p 'app/src/main/java/de/dragonschain/offgridmesh'
cat > 'app/src/main/java/de/dragonschain/offgridmesh/MainActivity.kt' << 'OFFGRID_EOF_MARKER'
package de.dragonschain.offgridmesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import de.dragonschain.offgridmesh.mesh.BleMeshManager
import de.dragonschain.offgridmesh.ui.nav.AppNav
import de.dragonschain.offgridmesh.ui.theme.OffgridMeshTheme

/**
 * Kein Onboarding, kein Login, kein Setup-Wizard.
 * App öffnen -> Berechtigungen abfragen (nur beim ersten Mal) -> direkt
 * einsatzbereit.
 */
class MainActivity : ComponentActivity() {

    private val meshManager by lazy { BleMeshManager(this) }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                ensureBluetoothEnabledThenStart()
            }
            // Bei Ablehnung: App bleibt nutzbar, Karte zeigt einfach keine
            // Nodes. Kein aufdringlicher erneuter Dialog (TODO: Hinweis auf
            // Status-Screen "Berechtigungen fehlen" statt stillschweigend).
        }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (meshManager.isBluetoothReady()) {
                meshManager.startDiscovery()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OffgridMeshTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(meshManager = meshManager)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasAllRequiredPermissions()) {
            ensureBluetoothEnabledThenStart()
        } else {
            requestPermissions.launch(requiredPermissions())
        }
    }

    override fun onStop() {
        super.onStop()
        meshManager.stopDiscovery()
    }

    private fun ensureBluetoothEnabledThenStart() {
        if (meshManager.isBluetoothReady()) {
            meshManager.startDiscovery()
        } else {
            requestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            // Vor Android 12 braucht BLE-Scan Standortberechtigung
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun hasAllRequiredPermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
}
OFFGRID_EOF_MARKER

mkdir -p 'app/src/main/java/de/dragonschain/offgridmesh/data'
cat > 'app/src/main/java/de/dragonschain/offgridmesh/data/Models.kt' << 'OFFGRID_EOF_MARKER'
package de.dragonschain.offgridmesh.data

/**
 * Ein anderes Gerät, das im Mesh gesehen wurde.
 * lastSeenMillis = System.currentTimeMillis() beim letzten Kontakt.
 * lat/lon = letzte bekannte Position, sofern per SOS/Karte geteilt.
 */
data class Node(
    val id: String,
    val displayName: String,
    val lastSeenMillis: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val hops: Int = 1, // 1 = direkter Kontakt, >1 = über Relay erreicht
)

enum class ChannelType { FAMILY, TEAM, BROADCAST }

data class Channel(
    val id: String,
    val name: String,
    val type: ChannelType,
)

data class Message(
    val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestampMillis: Long,
    val isSos: Boolean = false,
    val relayed: Boolean = false, // true = über andere Nodes weitergeleitet, nicht direkt empfangen
)

enum class BatteryMode { NORMAL, SAVER, ULTRA_SAVER }
OFFGRID_EOF_MARKER

mkdir -p 'app/src/main/java/de/dragonschain/offgridmesh/mesh'
cat > 'app/src/main/java/de/dragonschain/offgridmesh/mesh/BleMeshManager.kt' << 'OFFGRID_EOF_MARKER'
package de.dragonschain.offgridmesh.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import de.dragonschain.offgridmesh.data.BatteryMode
import de.dragonschain.offgridmesh.data.Message
import de.dragonschain.offgridmesh.data.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "BleMeshManager"

// Fester, eigener Service-UUID für OFFGRID MESH — nur Geräte mit dieser
// UUID im Advertising werden als Mesh-Node erkannt, kein wildes BLE-Rauschen.
private val SERVICE_UUID: UUID = UUID.fromString("8f5a1e2c-8d3b-4c9a-9e1f-6b7c2d4a5e10")

// Nach dieser Zeit ohne neues Advertisement gilt ein Node als "weg"
// und wird aus der Liste entfernt.
private const val NODE_TIMEOUT_MS = 5 * 60_000L
private const val CLEANUP_INTERVAL_MS = 30_000L

/**
 * Echte BLE-Discovery über BluetoothLeScanner + BluetoothLeAdvertiser.
 *
 * Aktueller Umfang (v0.3): Geräte finden sich gegenseitig, Node-Liste ist
 * live und echt. Nachrichtenversand (`sendMessage`, `broadcastSos`) läuft
 * noch lokal — echte Datenübertragun