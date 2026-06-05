# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Notruftrainer** — Android-App, die ein Samsung Galaxy A20e in ein Übungs-Handy für Kinder verwandelt. Kinder tippen z.B. `112` auf einer realistischen Dialer-Oberfläche, der echte Anruf geht aber an eine hinterlegte Trainings-Nummer (Elternteil auf zweitem Gerät, spielt die Rolle).

- Package: `koeln.paffrath.notruftrainer`
- Min SDK 30 (A20e ist auf Android 11 / One UI 3.1, dem letzten offiziellen Samsung-Update)
- Compose UI mit Material3, Kotlin DSL Gradle build
- Gewählter Architekturpfad ist **Pfad A**: A20e als dediziertes Trainingsgerät — kein PIN, Notruftrainer ist Default-Launcher, Kiosk via Android Device-Owner-Modus
- Andere Pfade (B = Notification-on-Lockscreen, C = QS-Tile, D = Root + Magisk-Modul für Emergency-Dialer-Replacement) wurden verworfen

## Architecture

Vier Kotlin-Dateien im `app/src/main/java/koeln/paffrath/notruftrainer/` Package, alle relativ flach:

- **`MainActivity.kt`** — einzige Activity, Compose-Entry-Point. Ruft `KioskManager.applyKioskPolicies(this)` in `onResume()` auf, damit die DPM-Policies bei jedem Resume frisch durchgesetzt werden (z.B. nach einem Anruf, der den Lock-Task-Modus unterbricht).
- **`DialerScreen.kt`** — komplette UI: Number-Display, 3×4-Keypad, grüner Call-Button (deaktiviert bis Eingabe gemappt wird), Backspace. Permission-Handling für `CALL_PHONE` via `ActivityResultContracts.RequestPermission`. Anruf via `Intent.ACTION_CALL` mit `tel:`-URI.
- **`NumberMapping.kt`** — `data class NumberMapping(dialedNumber, actualTarget, displayName)` und hardcoded `DEFAULT_MAPPINGS`-Liste. Ein UI zum Bearbeiten gibt es noch nicht — Mappings ändern = Code editieren + Rebuild.
- **`KioskManager.kt`** — kapselt die ganze DPM-Logik. Tut nichts, wenn die App nicht Device-Owner ist (= sicher zum Testen ohne Kiosk). Wenn Device-Owner: setzt Lock-Task-Packages-Whitelist (inkl. Samsung-Dialer/Telecom-Pakete, damit Anrufe trotz Lock-Task durchgehen), setzt Lock-Task-Features (`SYSTEM_INFO` für Statusbar + `KEYGUARD` damit der Sperrbildschirm noch erscheint), registriert die App als persistente Default-Home-Activity einmalig, und startet Lock-Task-Mode wenn noch nicht aktiv.
- **`AdminReceiver.kt`** — leere `DeviceAdminReceiver`-Subklasse, nur als Receiver-Komponente nötig für DPM.

Im Manifest:
- `CALL_PHONE` Permission
- `MainActivity` hat `CATEGORY_LAUNCHER` + `CATEGORY_HOME` + `CATEGORY_DEFAULT` (damit als Home registrierbar)
- `<receiver>`-Block für `AdminReceiver` mit `BIND_DEVICE_ADMIN` Permission + `device_admin` Metadata, das auf `res/xml/device_admin_policies.xml` zeigt (aktuell leere `uses-policies`).

## Build & Install Commands

```bash
# Debug-APK bauen
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Aufs verbundene A20e installieren (-r = reinstall, keep data)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# App starten
adb shell am start -n koeln.paffrath.notruftrainer/.MainActivity
```

ADB-Verbindung prüfen mit `adb devices` — das A20e zeigt sich als `RF8M62842CH`.

## Device-Owner Setup (einmaliger Initialprozess)

Device-Owner kann nur auf einem Gerät ohne User-Accounts gesetzt werden. Schritte:

1. A20e: Factory Reset, im Setup-Wizard alle Accounts überspringen, kein PIN
2. Entwickleroptionen + USB-Debugging aktivieren
3. `adb install app/build/outputs/apk/debug/app-debug.apk`
4. `adb shell dpm set-device-owner koeln.paffrath.notruftrainer/.AdminReceiver`
5. `adb shell am start -n koeln.paffrath.notruftrainer/.MainActivity`

Device-Owner-Status entfernen (Notausgang):
```bash
adb shell dpm remove-active-admin koeln.paffrath.notruftrainer/.AdminReceiver
adb uninstall koeln.paffrath.notruftrainer
```
Schlimmster Fall: erneuter Factory Reset.

## Known gotchas

- **`setKeyguardDisabled` ist persistent.** Wenn jemals `setKeyguardDisabled(admin, true)` aufgerufen wurde, bleibt der Lockscreen weg über Reboots hinweg — bis explizit `setKeyguardDisabled(admin, false)` gerufen wird. Das ist der Grund für den expliziten False-Call im `KioskManager`.
- **Lock-Task-Modus blendet den Keyguard standardmäßig aus.** Damit der Swipe-to-unlock-Sperrbildschirm trotz Lock-Task erscheint, muss `LOCK_TASK_FEATURE_KEYGUARD` in den Lock-Task-Features sein.
- **Anrufe brauchen Telecom-Pakete in der Lock-Task-Whitelist.** `com.android.phone`, `com.android.server.telecom`, `com.samsung.android.dialer`, `com.samsung.android.incallui` sind whitelisted, sonst kann die System-Anrufoberfläche während Lock-Task nicht erscheinen.
- **Der System-Anrufbildschirm zeigt die echte Zielnummer** (`+4915165259649`), nicht den `displayName` aus dem Mapping. `Intent.ACTION_CALL` übergibt nur die `tel:`-URI; das Display hängt am System-Dialer. Saubere Lösungen wären (a) Kontakt mit displayName als Name anlegen, oder (b) selbst Default-Dialer via `RoleManager.ROLE_DIALER` + `InCallService` werden.

## Status & open items

MVP für Pfad A ist komplett (Stand 2026-05-21):
- Dialer-UI ✓
- Mapping `112 → +4915165259649` ✓
- Echter Anruf wird platziert ✓
- App als Default-Home auf A20e ✓
- Kein PIN/Lockscreen-PIN, Swipe-to-unlock ✓
- Lock-Task-Modus aktiv, Home/Recents tun nichts ✓
- Boot direkt in App ✓

Noch offen / nice-to-have:
- Anrufbildschirm zeigt displayName statt echter Nummer (Kontakt-Eintrag oder Default-Dialer-Implementierung)
- Mehr Mappings (Papa, Mama, Arzt etc.)
- Eltern-Config-UI (versteckte Bedienung, um Mappings ohne Rebuild zu pflegen)
