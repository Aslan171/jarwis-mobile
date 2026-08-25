# Jarwis Mobile

Android client and public download page for Jarwis 1.0.

Download the signed APK from the [latest release](https://github.com/Aslan171/jarwis-mobile/releases/latest/download/Jarwis.apk).

The APK contains no AI model, Telegram token, database credentials or private
Jarwis source code. It connects only to the owner's Jarwis Web server on a
private IPv4 network. The server still requires its temporary pairing code and
signed session cookie.

The phone and the Jarwis PC must currently be connected to the same private
Wi-Fi network. Remote access outside the home network is not part of version
1.0.0.

## Build

The release workflow uses JDK 17, Android SDK 36, Android Gradle Plugin 9.2.0
and Gradle 9.4.1. A signed release requires these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Run the checks locally from an Android Studio terminal with a compatible JDK
and SDK:

```text
gradle :app:testDebugUnitTest :app:assembleDebug
```

The public download page is served from `docs/`. Releases publish the
installable APK as `Jarwis.apk` and its SHA-256 checksum.
