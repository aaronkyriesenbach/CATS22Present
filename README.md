# CATS22Present

Displays content on the secondary (external) screen of the CAT S22 flip phone. Targets devices running **LineageOS 20** (Android 13) with root access. May be compatible with other rooted Android 11+ devices that have a secondary display.

This app is aimed at aftermarket ROMs — there is a [lite version](https://github.com/B-CyberFunker/S22Present-Lite) for stock ROMs that is significantly simpler and more reliable.

> **Note:** This is a fork of [B-CyberFunker/CATS22Present](https://github.com/B-CyberFunker/CATS22Present), which is no longer maintained.

## Features

- Display the time and date on the secondary display
- Display notification title and text on the secondary display
- Display an audio visualizer when music is playing
- Customizable background, visualizer style, and font with built-in presets

## How It Works

The app uses Android's [Presentation](https://developer.android.com/reference/android/app/Presentation) API to render content on the secondary display. It detects lid open/close events via the proximity sensor and manages display power states through root-level access to `SurfaceControl`.

### Architecture

The app is split across several components that communicate via IPC:

- **MainActivity** — Entry point. Detects the secondary display via `DisplayManager`, provides buttons for granting notification access and opening settings.
- **ListenerService** — A foreground `Service` that shows the `PresentationHandler` on the secondary display, registers broadcast receivers for screen state changes (screen on/off, power connect/disconnect, lid events), and uses the proximity sensor to detect lid open/close. Communicates with `ScreenService` via `Messenger` IPC.
- **ScreenService** — A `RootService` (via [libsu](https://github.com/topjohnwu/libsu)) that performs privileged operations: controlling per-display power state through `SurfaceControl.setDisplayPowerMode()` and waking the device.
- **NotificationService** — A `NotificationListenerService` that receives system notification events and updates the Presentation's text fields. Tracks a separate music notification for the visualizer.
- **PresentationHandler** — Extends `Presentation` and renders the actual UI on the secondary display: a clock, date, notification title/content, and an audio visualizer (bar or square style). Applies style and font presets.
- **Globals** — Companion object holding shared state: `SurfaceControl` display tokens, UI element references, and current style/font settings.
- **SettingsActivity** — Allows the user to select style and font presets, persisted to a simple flat file (`settings`).

### Dependencies

- [libsu](https://github.com/topjohnwu/libsu) — Root access and `RootService` IPC
- [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) — Access to hidden Android framework APIs (`SurfaceControl`)
- [AudioVisualizer](https://github.com/nicchongwb/AudioVisualizer) — Bar and square audio visualizer widgets

## Known Issues

This app has several known limitations that are being actively worked on. See the [open issues](https://github.com/aaronkyriesenbach/CATS22Present/issues) for details and progress.

## Enabling the Secondary Display

On LineageOS and other GSI-based ROMs, the secondary display is disabled by default. To enable it automatically on boot, create a Magisk module with a `system.prop` file and a boot script. Magisk loads `system.prop` via `resetprop` early in the boot process, which can override read-only vendor properties. A boot script then restarts the hardware composer to pick up the changes and enables hidden API access.

**Step 1:** Create the Magisk module:

```bash
adb shell su -c 'mkdir -p /data/adb/modules/s22-display && \

cat > /data/adb/modules/s22-display/module.prop << "PROP"
id=s22-display
name=CAT S22 Secondary Display
version=1.0
versionCode=1
author=aaronkyriesenbach
description=Enables secondary display on LineageOS/GSI ROMs
PROP

cat > /data/adb/modules/s22-display/system.prop << "PROPS"
ro.vendor.gsi.image_running=false
ro.hdmi.enable=true
ro.qualcomm.cabl=2
ro.vendor.display.cabl=2
vendor.display.disable_skip_validate=1
tunnel.decode=true
PROPS

cat > /data/adb/modules/s22-display/service.sh << "SCRIPT"
#!/system/bin/sh
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done
settings put global hidden_api_policy 1
setprop ctl.restart vendor.hwcomposer-2-1
SCRIPT
chmod 755 /data/adb/modules/s22-display/service.sh'
```

**Step 2:** Reboot:

```bash
adb reboot
```

Magisk loads the properties from `system.prop` during early boot. The `service.sh` script runs after boot completes to enable hidden API access and restart the hardware composer, which activates the secondary display. This will appear as a restart shortly after initial boot - this is normal.

To verify the module is active after reboot:

```bash
adb shell getprop ro.vendor.gsi.image_running
```

The module can be toggled on/off in the Magisk app. To remove it entirely:

```bash
adb shell su -c 'rm -rf /data/adb/modules/s22-display'
adb reboot
```

> **Note:** This overlays the entire `vendor/build.prop`. If you update your ROM, re-run step 1 to pick up any new properties from the updated vendor image.

**Step 2:** Create a boot script for the hidden API policy setting (requires `system_server`, so it can't go in `build.prop`):

```bash
adb shell su -c 'cat > /data/adb/service.d/s22-hidden-api.sh << "SCRIPT"
#!/system/bin/sh
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done
settings put global hidden_api_policy 1
SCRIPT
chmod 755 /data/adb/service.d/s22-hidden-api.sh'
```

**Step 3:** Reboot:

```bash
adb reboot
```

To verify the module is active after reboot:

```bash
adb shell su -c 'cat /data/adb/modules/s22-display/vendor/build.prop | grep s22'
adb shell getprop ro.vendor.gsi.image_running
```

To remove everything:

```bash
adb shell su -c 'rm -rf /data/adb/modules/s22-display /data/adb/service.d/s22-hidden-api.sh'
adb reboot
```

## Requirements

- CAT S22 (or another device with a secondary display)
- LineageOS 20 (Android 13) or compatible aftermarket ROM
- Root access (Magisk recommended)
- Notification listener permission (granted via the in-app button)
