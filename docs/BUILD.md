# Building OnlyFlows from Source

## Requirements

| Tool | Version |
|---|---|
| Android Studio | Hedgehog or later |
| JDK | 17 (bundled with Android Studio JBR) |
| Android SDK | API 35 |
| Gradle | 8.9 (wrapper included) |

---

## Clone

```bash
git clone https://github.com/Iurika-N-T-R/OnlyFlows.git
cd OnlyFlows
```

---

## Build

### Debug APK (development)

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (production)

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

> **Note:** The release build requires a signing key. The signing config reads four Gradle
> properties — `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — via
> `project.findProperty()` (set them in `~/.gradle/gradle.properties` or as
> `ORG_GRADLE_PROJECT_*` environment variables, never in the repo). If you don't have a
> keystore, use the debug build.

### Full build (all variants)

```bash
./gradlew build
```

### Clean

```bash
./gradlew clean
```

---

## Lint

```bash
./gradlew lint
```

---

## Install on device

```bash
./gradlew installDebug
```

Or drag the APK onto an Android device / emulator.

---

## Android Studio

1. Open Android Studio → **Open** → select the `OnlyFlows` folder
2. Wait for Gradle sync to complete
3. Select a device or emulator
4. Click **Run** (▶)

---

## App Configuration

No API keys or secrets are required. OnlyFlows is fully local-first:
- All data is stored in Room (SQLite) on the device
- Sync is handled by the built-in LAN sync (optional, configured in Settings → LAN Sync)
- No network permissions are required except `ACCESS_NETWORK_STATE` for the sync status monitor

---

## Min/Target SDK

```
minSdk    = 26   (Android 8.0)
targetSdk = 35   (Android 15)
```
