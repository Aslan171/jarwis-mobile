# Jarwis Mobile

Android client and public download page for Jarwis 1.0.

The APK contains no AI model, Telegram token, database credentials or private
Jarwis source code. It connects only to the owner's Jarwis Web server on a
private IPv4 network. The server still requires its temporary pairing code and
signed session cookie.

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

The public download page is served from `docs/`. Tagged builds publish the
installable APK as `Jarwis.apk` in GitHub Releases.
