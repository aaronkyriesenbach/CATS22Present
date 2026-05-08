# AGENTS.md

## Project Overview

Android app (Kotlin + Java) that displays content on the CAT S22 flip phone's secondary screen. Single Gradle module (`app`) containing both the main application and LSPosed/Xposed hooks. Requires root access; targets rooted Android 11+ devices with a secondary display (LineageOS 20 on CAT S22 is the primary target).

## Build & Run

```bash
# Build debug APK (uses just task runner)
just build            # ./gradlew assembleDebug

# Build and install to connected device
just install          # assembleDebug + adb install

# Release build
just build-release    # ./gradlew assembleRelease
```

- Gradle 9.5.0, AGP 9.2.0, Kotlin compiled via AGP's built-in support (no separate Kotlin Gradle plugin)
- Java source compatibility 17
- compileSdk 36, targetSdk 36, minSdk 30 (Android 11+)
- Android SDK expected at `/opt/android-sdk` (see `local.properties`, gitignored)
- No CI, no test suite beyond default stubs, no linter config beyond `lint-baseline.xml`

## Source Layout

All code lives in a single `:app` module (package `com.android.s22present`). There is no separate xposed-module — the Xposed hooks are bundled into the app.

```
app/src/main/java/com/android/s22present/
  MainActivity.kt          # Entry point, display detection, permissions
  ListenerService.kt       # Foreground service + NotificationService (bottom of file)
  ScreenService.kt         # RootService (libsu), display power via SurfaceControl reflection
  PresentationHandler.kt   # Renders UI on secondary display (clock, notifs, visualizer)
  Globals.kt               # Companion object: shared mutable state, prefs keys, migration
  SettingsActivity.kt      # Style/font/debounce settings UI
  BootReceiver.kt          # Starts ListenerService on boot if secondary display exists
  WakeReceiver.kt          # Broadcast receiver for WAKE_DISPLAY intent
  xposed/
    KeyguardDisplayHook.java   # Hooks SystemUI to prevent keyguard on secondary display
    KeyDebounceHook.java       # Hooks PhoneWindowManager.interceptKeyBeforeQueueing for key debounce
```

### Xposed Integration

- Two hooks registered in `resources/META-INF/xposed/java_init.list`
- Scope: `com.android.systemui` + `android` (system server) — see `scope.list`
- Uses libxposed API 101 (`compileOnly` dependency)
- `KeyDebounceHook` reads debounce config via `getRemotePreferences("s22present_module")`

### Key Architectural Details

- All app components run in process `S22Present.App` (set at `<application>` level in manifest)
- `NotificationService` class lives inside `ListenerService.kt`, not in a separate file
- `ScreenService` runs as root via libsu `RootService` with `Messenger` IPC
- Display tokens obtained via reflection: `SurfaceControl.getPhysicalDisplayIds()` → `sfids[0]` = main, `sfids[1]` = secondary
- `HiddenApiBypass` required for `SurfaceControl` reflection; needs `hidden_api_policy=1` at boot
- Settings stored in `SharedPreferences` (groups: `s22present_settings`, `s22present_module`); old flat-file settings auto-migrate via `Globals.migrateSettingsIfNeeded()`

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| libsu (core + service) | 5.2.2 | Root access, `RootService` IPC |
| HiddenApiBypass | 2.0 | `SurfaceControl` reflection on restricted APIs |
| AudioVisualizer | 2.2.5 | Bar/square visualizer widgets |
| libxposed API | 101.0.1 | Xposed hook API (`compileOnly`) |

- View Binding is **disabled** (`viewBinding = false`)
- JitPack repo enabled for AudioVisualizer dependency

## Conventions

- Log tags: `S22Pres*` prefix (e.g., `S22PresListServ`, `S22PresScreenServInit`, `S22PresNotifServ`)
- Commit messages: conventional style — `feat:`, `fix:`, `refactor:`
- No emulator support — hardware-specific display control requires a rooted device with secondary display

## Testing on Device

- Requires rooted Android device with secondary display (CAT S22 or similar)
- Magisk module needed to enable secondary display on LineageOS (see README)
- SELinux policy required for sysfs backlight writes
