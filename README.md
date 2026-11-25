# Extinguish (Root / Shizuku dual-mode)

English | [简体中文](README_ZH_CN.md)

Extinguish lets Android apps keep running while the display is truly turned off, instead of simply showing a black overlay. It works with different display technologies, including LCD and OLED.

This project is forked from [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish). The main changes are: **Root and Shizuku are both supported and switchable at any time, Android 17 support, and a single quick-settings tile**.

> [!IMPORTANT]
> This edition requires either a rooted device (e.g. Magisk) or an installed and authorized Shizuku. Phone manufacturers may change display and hidden-API behavior, so results can differ between devices.

## Changes from upstream

| Area | Upstream | This fork |
| --- | --- | --- |
| Privileged startup | Shizuku only | **Root and Shizuku coexist, switchable at any time** |
| Compile / target SDK | Android 15 / API 35 | Android 17 / API 37 |
| Quick-settings tiles | Two tiles, tapped in order | One tile for starting and toggling |
| Privilege state recovery | Not applicable | Async cold-start detection; authorized users enter Home directly |
| User interface | Shizuku authorization flow | Root or Shizuku authorization flow per selection |

## Features

- Turns the display off instead of drawing a black overlay.
- Two screen-off solutions:
  - **Set screen power off**: switches display power off; touch input is unavailable while off.
  - **Set screen brightness to -1**: keeps touch input available for waking the screen.
- Floating button with a timer popup.
- Volume-key control.
- Touch-event control.
- Notification controls.
- One quick-settings tile.
- External control via ADB / Intent.

## Privilege method (Root / Shizuku)

Choose the privilege method in **Settings → Solutions** or during the first-run guide:

- **Root**: run privileged services as root via [libsu](https://github.com/topjohnwu/libsu); requires a rooted device (Magisk recommended).
- **Shizuku**: run privileged services via [Shizuku](https://github.com/RikkaApps/Shizuku); works on rooted devices or through wireless debugging without modifying the system.

Both methods share the same AIDL interface and hidden-API logic, so the feature set is identical; just complete the corresponding authorization after switching. No reinstall is needed.

### Using the single tile

1. Add **Extinguish** to quick settings.
2. The first tap checks the selected privilege method. Once granted, the service starts and turns the screen off.
3. Later taps toggle the screen on and off.
4. There is no longer a separate tile for starting the service.

If the floating button is enabled, the "display over other apps" permission is also required.

## Requirements

- Android 8.0 (API 26) through Android 17 (API 37).
- Either a rooted device that grants superuser access to this app, or Shizuku installed and authorized for this app.
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

## How it works

Upstream runs `DisplayControlService` and `EventsProviderService` in a privileged process through Shizuku. This fork keeps both hosting implementations behind one AIDL interface:

- The main app remains a normal Android process.
- **Root mode**: the `root-service` module uses libsu `RootService` to run the services as root.
- **Shizuku mode**: the `shizuku-service` module runs the services as Shizuku user services.
- Both modules implement one AIDL interface (defined in `root-service`), so the main process depends on a single binder type and binds to the selected target.
- The hidden-API screen-control logic is identical in both implementations.
- On a cold start, the app asynchronously checks the selected method's state; if already granted, reopening the app enters Home directly.

## Privacy and permissions

Extinguish does not upload personal data. Root/Shizuku, overlay, and notification permissions are used only for the features described above. Root/Shizuku are powerful capabilities, so install this app only from a source you trust.

## Known limitations

- Some manufacturer ROMs restrict display power, brightness changes, or access to `getevent`.
- Root access is controlled by the device's root manager; Shizuku needs its service running and granted. If revoked, grant it again from within the app.
- A debug-signed build cannot replace a build signed with a different key without uninstalling it first.

## License and credits

This fork keeps the upstream [GPL-3.0 license](LICENSE).

- Original project: [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)
- Root support: [libsu](https://github.com/topjohnwu/libsu) (Apache-2.0).
- Shizuku support: [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) (MIT).
