# OFFGRID MESH (Beta))

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
- [ ] Echte BLE-Discovery (Android `BluetoothLeScanner`)
- [ ] `WifiP2pManager`-Anbindung als Fallback/Ergänzung
- [ ] Store-and-Forward-Queue (SQLite/Room)
- [ ] Ende-zu-Ende-Verschlüsselung (X25519 + Double Ratchet + ChaCha20-Poly1305, TOFU-Fingerprint-Abgleich)
- [ ] Adaptive Scan-Intervalle je Akku-Modus
- [ ] SOS-Broadcast mit echtem GPS + Multi-Hop-Relay
- [ ] **Node-Modus (Relay, Variante 1)** – bestehendes Altgerät als Fix-Relay, kein UI-Rendering, Dauer-Discovery, Display aus, Betrieb an Powerbank/Solar. Reine Software-Lösung auf gleicher Codebasis, keine Zusatz-Hardware nötig. Siehe Abschnitt "Reichweite & Node-Modus".
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

## Entwicklung (Cloud, kein PC nötig)

Gleiches Setup wie beim Schwesterprojekt
[Rethink Setup Wizard](https://github.com/RethinkWizard/rethink-wizard):

- **Gitpod:** Repo-URL mit `https://gitpod.io/#` davor öffnen im Browser
- **Codespaces:** Auf GitHub → "Code" → "Codespaces" → "Create codespace on main"

Beide installieren automatisch Android SDK + Gradle.

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
