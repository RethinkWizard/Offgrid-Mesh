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
