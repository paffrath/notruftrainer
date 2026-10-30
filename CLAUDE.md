# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Notruftrainer** — Android-App, die ein Samsung Galaxy A20e in ein Übungs-Handy für Kinder verwandelt. Das Kind sieht einen simulierten PIN-Sperrbildschirm, tippt auf „Notruf“, wählt z.B. `112` auf einer realistischen Dialer-Oberfläche — der echte Anruf geht aber an eine hinterlegte Trainings-Nummer (sipgate-Festnetznummer, auf der Gegenseite nimmt eine Asterisk-Instanz den Anruf an).

- Package: `koeln.paffrath.notruftrainer`
- Min SDK 30 (A20e ist auf Android 11 / One UI 3.1, dem letzten offiziellen Samsung-Update), Target SDK 36
- Compose UI mit Material3, Kotlin DSL Gradle build
- Gewählter Architekturpfad ist **Pfad A**: A20e als dediziertes Trainingsgerät — kein echter PIN, Notruftrainer ist Default-Launcher, Kiosk via Android Device-Owner-Modus
- Andere Pfade (B = Notification-on-Lockscreen, C = QS-Tile, D = Root + Magisk-Modul für Emergency-Dialer-Replacement) wurden verworfen

## Architecture

Alle Kotlin-Dateien liegen flach in `app/src/main/java/koeln/paffrath/notruftrainer/` (Theme unter `ui/theme/`):

- **`MainActivity.kt`** — einzige Activity, Compose-Entry-Point. Navigation: `PinScreen` → (Notruf-Button) → `DialerScreen`. Startet in `onCreate()` den `PowerButtonDetectorService`. Ruft in `onResume()` `KioskManager.applyKioskPolicies(this)` auf (DPM-Policies bei jedem Resume frisch durchsetzen, z.B. nach einem Anruf) und erhöht einen `resumeCounter` — dadurch springt die App nach Anruf-Ende automatisch zurück zum PIN-Screen.
- **`PinScreen.kt`** — simulierter Samsung-One-UI-Sperrbildschirm. Jede PIN wird mit „Falsche PIN“ abgewiesen, außer `EXIT_PIN = "0000"`: die verlässt den Kiosk (`KioskManager.exitKiosk`). Notruf-Button öffnet den Dialer.
- **`DialerScreen.kt`** — Notruf-Dialer: Number-Display, Keypad, grüner Call-Button, Backspace. Permission-Handling für `CALL_PHONE` via `ActivityResultContracts.RequestPermission`. Anruf via `Intent.ACTION_CALL` mit `tel:`-URI. Kein Back-Button zurück zum PIN-Screen.
- **`NumberMapping.kt`** — `data class NumberMapping(dialedNumber, actualTarget, displayName)` und `DEFAULT_MAPPINGS` (aktuell nur `112`). Die Zielnummer kommt aus `BuildConfig.TRAINING_NUMBER`.
- **`PowerButtonDetectorService.kt`** — Foreground-Service (`specialUse`), zählt `SCREEN_ON`/`SCREEN_OFF`-Broadcasts: 5 Events innerhalb 3 s → Anruf auf das `112`-Mapping (simuliert Samsungs 5×-Power-Notruf).
- **`BootReceiver.kt`** — startet den `PowerButtonDetectorService` nach `BOOT_COMPLETED`.
- **`KioskManager.kt`** — kapselt die DPM-Logik. Tut nichts, wenn die App nicht Device-Owner ist (= sicher zum Testen ohne Kiosk). Als Device-Owner: Lock-Task-Packages-Whitelist (inkl. Dialer/Telecom/InCallUI, damit Anrufe trotz Lock-Task durchgehen), Lock-Task-Features `SYSTEM_INFO` + `KEYGUARD`, `setKeyguardDisabled(false)`, App einmalig als persistente Default-Home-Activity, Lock-Task starten. `exitKiosk()` macht das rückgängig (Service stoppen, Lock-Task beenden, Home-Präferenz löschen, Samsung-Launcher starten).
- **`AdminReceiver.kt`** — leere `DeviceAdminReceiver`-Subklasse, nur als Receiver-Komponente nötig für DPM.

Im Manifest:
- Permissions: `CALL_PHONE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`
- `MainActivity` hat `CATEGORY_LAUNCHER` + `CATEGORY_HOME` + `CATEGORY_DEFAULT` (damit als Home registrierbar)
- `PowerButtonDetectorService`, `BootReceiver`, und `AdminReceiver` mit `BIND_DEVICE_ADMIN` + `device_admin`-Metadata (`res/xml/device_admin_policies.xml`, leere `uses-policies`)

## Configuration

Die Trainings-Zielnummer steht **nicht im Repo**, sondern in `local.properties` (gitignored):

```properties
training.number=+49...
```

`app/build.gradle.kts` macht daraus `BuildConfig.TRAINING_NUMBER`. Fehlt der Eintrag, ist die Nummer leer und Anrufe gehen ins Leere. Aktuell ist das die sipgate-Festnetznummer der Asterisk-Gegenseite.

## Build & Install Commands

```bash
# Debug-APK bauen
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Aufs verbundene Gerät installieren (-r = reinstall, keep data)
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk

# App starten
adb -s <serial> shell am start -n koeln.paffrath.notruftrainer/.MainActivity
```

Geräte (beide Samsung Galaxy A20e, SM-A202F, Android 11):

| Serial | Gerät |
|---|---|
| `RF8M62842CH` | Erstgerät |
| `R58N35Z6K2X` | Zweitgerät (eingerichtet 2026-10-30, Telekom-SIM) |

Play Store und Galaxy Store sind auf den Geräten entfernt — App-Updates nur per `adb install -r`.

## Neues Gerät einrichten

Device-Owner kann nur auf einem Gerät ohne User-Accounts gesetzt werden.

1. Factory Reset, im Setup-Wizard alle Accounts überspringen, keinen PIN setzen
2. Entwickleroptionen + USB-Debugging aktivieren
3. Prüfen: `adb shell dumpsys account` (keine Accounts), `adb shell pm list users` (nur User 0)
4. APK bauen und installieren (siehe oben)
5. `adb shell dpm set-device-owner koeln.paffrath.notruftrainer/.AdminReceiver`
6. `adb shell pm grant koeln.paffrath.notruftrainer android.permission.CALL_PHONE`
7. **Kontakt-Trick** (damit der Anrufbildschirm „Notruf 112“ statt der echten Nummer zeigt): lokalen Samsung-Kontakt anlegen
   ```bash
   adb shell content insert --uri content://com.android.contacts/raw_contacts \
     --bind account_type:s:vnd.sec.contact.phone --bind account_name:s:vnd.sec.contact.phone
   # neue _id ermitteln:
   adb shell content query --uri content://com.android.contacts/raw_contacts --projection _id:account_type
   adb shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:<id> \
     --bind mimetype:s:vnd.android.cursor.item/name --bind 'data1:s:Notruf\ 112'
   adb shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:<id> \
     --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:<training.number> --bind data2:i:2
   # prüfen:
   adb shell content query --uri content://com.android.contacts/phone_lookup/<training.number> --projection display_name
   ```
8. **Lockscreen-Shortcut links** auf den Notruftrainer (Format `<links-an>;<komp-links>;<rechts-an>;<komp-rechts>;`, rechts aus, weil die Kamera entfernt wird):
   ```bash
   adb shell settings put system lock_application_shortcut \
     '1\;koeln.paffrath.notruftrainer/koeln.paffrath.notruftrainer.MainActivity\;0\;com.sec.android.app.camera/com.sec.android.app.camera.Camera\;'
   ```
9. **Mobile Daten aus:** `adb shell svc data disable` (WLAN bleibt verfügbar, Telefonie geht weiter)
10. **Bloatware entfernen** per `adb shell pm uninstall -k --user 0 <pkg>` (rückgängig: `cmd package install-existing <pkg>`). Standardliste: Facebook (`com.facebook.katana`, `.appmanager`, `.services`, `.system`), `com.microsoft.skydrive`, Netflix (`com.netflix.mediaclient`, `com.netflix.partner.activation`), `com.google.android.youtube`, `com.google.android.apps.maps`, `com.google.android.gm`, `com.google.android.googlequicksearchbox`, `com.android.chrome`, `com.android.vending`, `com.sec.android.app.samsungapps`, `com.sec.android.app.fm`, `com.samsung.android.calendar`, `com.sec.android.gallery3d`, `com.sec.android.app.myfiles`, `com.sec.android.app.camera`. **Nicht anfassen:** Einstellungen, Dialer, Kontakte (Kontakt-Trick!), SIM-Toolkit, Telecom/InCallUI/SystemUI, Play Services/gsf/webview/permissioncontroller.
11. `adb shell am start -n koeln.paffrath.notruftrainer/.MainActivity`, dann prüfen: `adb shell dumpsys activity activities | grep mLockTaskModeState` → `LOCKED`
12. Reboot und prüfen, dass das Gerät direkt in der App landet und der `PowerButtonDetectorService` läuft (`dumpsys activity services koeln.paffrath.notruftrainer`)

Device-Owner-Status entfernen (Notausgang): am Gerät PIN `0000` eingeben (verlässt den Kiosk), oder per adb:
```bash
adb shell dpm remove-active-admin koeln.paffrath.notruftrainer/.AdminReceiver
adb uninstall koeln.paffrath.notruftrainer
```
Schlimmster Fall: erneuter Factory Reset.

## Known gotchas

- **`setKeyguardDisabled` ist persistent.** Wenn jemals `setKeyguardDisabled(admin, true)` aufgerufen wurde, bleibt der Lockscreen weg über Reboots hinweg — bis explizit `setKeyguardDisabled(admin, false)` gerufen wird. Das ist der Grund für den expliziten False-Call im `KioskManager`.
- **Lock-Task-Modus blendet den Keyguard standardmäßig aus.** Damit der Swipe-to-unlock-Sperrbildschirm trotz Lock-Task erscheint, muss `LOCK_TASK_FEATURE_KEYGUARD` in den Lock-Task-Features sein.
- **Anrufe brauchen Telecom-Pakete in der Lock-Task-Whitelist.** `com.android.phone`, `com.android.server.telecom`, `com.android.dialer`, `com.samsung.android.dialer`, `com.samsung.android.incallui` sind whitelisted, sonst kann die System-Anrufoberfläche während Lock-Task nicht erscheinen.
- **Der System-Anrufbildschirm zeigt den Kontaktnamen, nicht den `displayName` aus dem Mapping.** `Intent.ACTION_CALL` übergibt nur die `tel:`-URI. Ohne den Kontakt-Trick (Einrichtungsschritt 7) steht dort die echte Zielnummer. Jedes neue Mapping braucht einen eigenen Kontakt.
- **`pm uninstall --user 0` kann den Lock-Task beenden** (Launcher springt kurz nach vorne). Danach immer `am start ... MainActivity` und `mLockTaskModeState` prüfen.
- **5×-Power-Notruf beim Debuggen:** wiederholte `input keyevent KEYCODE_WAKEUP`/Screen-Toggles per adb können den `PowerButtonDetectorService` auslösen → echter Trainingsanruf.
- **Escaping über `adb shell`:** Leerzeichen in `content --bind`-Werten brauchen `\ `, Semikolons in `settings put` brauchen `\;` (lokale Shell → adb → Remote-sh).

## Status & open items

Pfad A ist auf beiden Geräten produktiv (Stand 2026-10-30): PIN-Screen-Simulation, Notruf-Dialer, Anruf an die Trainings-Nummer mit Kontaktnamen im Anrufbildschirm, 5×-Power-Notruf, Device-Owner-Kiosk mit Lock-Task, Boot direkt in die App, Kiosk-Exit per PIN `0000`.

Noch offen / nice-to-have:
- Back-Button im Dialer (aktuell kein Weg zurück zum PIN-Screen ohne Anruf)
- Mehr Mappings (Papa, Mama, Arzt etc.) — jeweils mit eigenem Kontakt
- Eltern-Config-UI (versteckte Bedienung, um Mappings ohne Rebuild zu pflegen)
