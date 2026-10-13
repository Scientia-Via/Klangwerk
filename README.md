# Klangwerk – Minecraft-Mod (NeoForge 1.21.1)

Spielt MP3-Dateien aus dem Ordner **`Music`** im Spielverzeichnis (root, neben `mods/`, `saves/` …) ab.

## Bauen
1. JDK 21 installieren, Projekt in IntelliJ/Eclipse öffnen oder per Konsole:
   `./gradlew build` (Gradle-Wrapper aus dem NeoForge-MDK 1.21.1 ins Projekt kopieren, falls nicht vorhanden)
2. Fertige Datei: `build/libs/klangwerk-1.0.0.jar` (JLayer-MP3-Decoder ist per Jar-in-Jar eingebettet)
3. Testen: `./gradlew runClient`

## Inhalt (Kreativ-Tab „Klangwerk“, außerdem „Funktionale Blöcke“ / „Werkzeuge“)
| Gegenstand | Funktion | Rezept (Survival) |
|---|---|---|
| Grammophon | spielt ohne Strom, Ton kommt aus dem Trichter | 3 Kupfer, 5 Bretter, 1 Jukebox |
| Plattenspieler | ohne Strom, 6 Ausgänge auf der Rückseite | 3 Eisen, 2 Redstone, 1 Jukebox, 3 Glatter Stein |
| Hochton-/Mittelton-/Tiefton-Box | Filter: Höhen >3,5 kHz, Mitten 250 Hz–3,5 kHz, Bass <250 Hz | 5 Bretter, 2 Eisen, 1 Redstone, Amethyst / Kupfer / Eisenblock |
| Audiokabel | Plattenspieler-Ausgang ↔ Box | 2 Kupfer + 1 Faden |

## Bedienung
* Rechtsklick auf Grammophon/Plattenspieler → Menü: Songliste, Fortschrittsbalken (anklickbar), «« « Play/Pause Stop » »», Wiederholen.
* **Kabel:** Kabel in die Hand → Rechtsklick auf einen der 6 Ausgänge hinten (oben: Links/Mitte/Rechts, unten: Hinten links/Subwoofer/Hinten rechts) → Rechtsklick auf eine Box. Shift+Rechtsklick leert einen Ausgang bzw. trennt eine Box.
* An jeden Ausgang dürfen mehrere Boxen (verschiedene Typen gemischt). Stereo = Links+Rechts, Surround = zusätzlich Mitte/Hinten/Sub.

## Strom / Create
Der Plattenspieler braucht **keinen Strom** und funktioniert ohne Create (keine Abhängigkeit mehr).

## Mehrspieler / Essentials
Nur der Spieler, der einen Titel startet, braucht die MP3 in seinem `Music`-Ordner. Die Datei wird einmal zum Server hochgeladen (Cache: `klangwerk-cache/`, Prüfung per SHA-256) und automatisch an alle Mitspieler verteilt, die sie nicht haben. Funktioniert auf dedizierten Servern und in LAN-/Essential-Welten (der Host ist dann der Server). Maximalgröße pro Song: `maxSongMb` in `config/klangwerk-common.toml` (Standard 32 MB).

## Musikordner in den Einstellungen
Optionen → Musik & Geräusche → Button **„Klangwerk-Musik…“**: zeigt alle Dateien im Ordner `Music` und öffnet den Ordner im Datei-Explorer.
