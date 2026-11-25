# Extinguish (Root Edition)

English | [简体中文](README_ZH_CN.md)

Extinguish lets Android apps keep running while the display is truly turned off, instead of simply showing a black overlay. It works with different display technologies, including LCD and OLED.

This project is forked from [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish). The main changes are: **Root instead of Shizuku, Android 17 support, and a single quick-settings tile**.

> [!IMPORTANT]
> This edition requires a rooted device. Phone manufacturers may change display and hidden-API behavior, so results can differ between devices.

## Changes from upstream

| Area | Upstream | This fork |
| --- | --- | --- |
| Privileged startup | Shizuku | libsu 6.0.0 RootService |
| Compile / target SDK | Android 15 / API 35 | Android 17 / API 37 |
| Quick-settings tiles | Two tiles, tapped in order | One tile for starting and toggling |
| Root state recovery | Not applicable | Async cold-start detection; authorized users enter Home directly |
| User interface | Shizuku authorization flow | Root authorization flow |

## Features

- Turns the display off instead of drawing a black overlay.
- Two screen-off solutions:
  - **Root | Set screen power off**: switches display power off; touch input is unavailable while off.
  - **Root | Set screen brightness to -1**: keeps touch input available for waking the screen.
- Floating button with a timer popup.
- Volume-key control.
- Touch-event control.
- Notification controls.
- One quick-settings tile.
- External control via ADB / Intent.

### Using the single tile

1. Add **Extinguish** to quick settings.
2. The first tap checks root access. Once granted, the service starts and turns the screen off.
3. Later taps toggle the screen on and off.
4. There is no longer a separate tile for starting the service.

If the floating button is enabled, the "display over other apps" permission is also required.

## Requirements

- Android 8.0 (API 26) through Android 17 (API 37).
- A rooted device that grants superuser access to this app. Magisk is recommended.
- If an upstream build with a different signature is installed, uninstall it before installing this build.

## Build

Use JDK 17 and an Android SDK containing:

- `platforms;android-37.0`
- `build-tools;35.0.0` or `build-tools;37.0.0`
- `platform-tools`

Create `local.properties` in the project root:

```properties
sdk.dir=/path/to/android-sdk
```

Then run:

```bash
./gradlew :app:assembleDebugSignedDebug
```

The APK is produced at:

```text
app/build/outputs/apk/debugSigned/debug/app-debugSigned-debug.apk
```

It is signed with the standard Android debug key and can be installed directly for testing.

## How Root startup works

Upstream runs `DisplayControlService` and `EventsProviderService` in a privileged process through Shizuku. This fork converts them into libsu `RootService` implementations:

- The main app remains a normal Android process.
- Each `RootService` runs as root and communicates with the main process through Binder/AIDL.
- The hidden-API screen-control logic is unchanged; only the hosting mechanism was replaced.
- On a cold start, libsu may not yet know whether root was granted. The app asynchronously creates a root shell and waits for the result before choosing a destination.
- If root was already granted, reopening the app enters Home directly.

## Privacy and permissions

Extinguish does not upload personal data. Root, overlay, and notification permissions are used only for the features described above. Root is a powerful capability, so install this app only from a source you trust.

## Known limitations

- Some manufacturer ROMs restrict display power, brightness changes, or access to `getevent`.
- Root access is controlled by the device's root manager; if it is revoked, grant it again from within the app.
- A debug-signed build cannot replace a build signed with a different key without uninstalling it first.

## License and credits

This fork keeps the upstream [GPL-3.0 license](LICENSE).

- Original project: [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)
- Root support: [libsu](https://github.com/topjohnwu/libsu) (Apache-2.0).
