# AGENTS.md

## Project Overview

Android app (Kotlin) that displays content on the CAT S22 flip phone's secondary screen. Two Gradle modules: `app` (main application) and `xposed-module` (LSPosed/Xposed hook). Requires root access and targets LineageOS 20 / Android 13+.

## Build & Run

```bash
# Build debug APK (uses just task runner)
just build            # ./gradlew assembleDebug

# Build and install to connected device
just install          # assembleDebug + adb install

# Release build
just build-release    # ./gradlew assembleRelease
```

- Gradle 9.5.0, AGP 9.2.0, Kotlin not applied at top level (Java source compatibility 17)
- Android SDK expected at `/opt/android-sdk` (see `local.properties`)
- No CI, no test suite beyond default stubs, no linter config beyond baseline

## Module Structure

| Module | Package | Language | Purpose |
|--------|---------|----------|---------|
| `app` | `com.android.s22present` | Kotlin | Main app — services, presentation, UI |
| `xposed-module` | `com.android.s22present.xposed` | Java | LSPosed module that hooks `KeyguardDisplayManager` in SystemUI |

### `app` Key Components

- `ListenerService` — foreground service, proximity sensor, IPC to ScreenService
- `ScreenService` — `RootService` (libsu), controls display power via reflection on `SurfaceControl`
- `PresentationHandler` / `presentation.kt` — renders UI on secondary display
- `NotificationService` — `NotificationListenerService`, updates presentation text
- `Globals` — companion object with shared mutable state (display tokens, UI refs, settings)
- `SettingsActivity` — persists style/font to flat file

### `xposed-module`

- Single class `KeyguardDisplayHook` hooks `showPresentation` in SystemUI to prevent keyguard on secondary display
- Uses libxposed API (stub jar in `xposed-module/libs/`)
- Scope: `com.android.systemui` (see `resources/META-INF/xposed/scope.list`)

## Dependencies & Quirks

- **libsu 5.2.2** — `RootService` IPC via `Messenger`. `ScreenService` runs as root.
- **HiddenApiBypass** — required for `SurfaceControl` reflection; needs `hidden_api_policy=1` set at boot
- **AudioVisualizer** (`io.github.gautamchibde:audiovisualizer:2.2.5`) — bar/square visualizer widgets
- **View Binding** enabled (`buildFeatures.viewBinding = true`)
- No Kotlin Gradle plugin applied — source files are `.kt` but compiled via AGP's built-in Kotlin support (AGP 9+)
- `lint-baseline.xml` present in `app/`; lint issues are baselined, not clean

## Conventions

- All app components run in process `S22Present.App` (set in manifest)
- Log tags follow `S22Pres*` prefix (e.g., `S22PresScreenServ`, `S22PresScreenServInit`)
- Display tokens obtained via reflection: `sfids[0]` = main display, `sfids[1]` = secondary display
- Commit messages use conventional style: `feat:`, `fix:`, `refactor:`

## Testing on Device

- Requires rooted Android device with secondary display (CAT S22 or similar)
- Magisk module needed to enable secondary display on LineageOS (see README)
- SELinux policy required for sysfs backlight writes
- No emulator support — hardware-specific display control
