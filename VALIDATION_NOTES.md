# Validation notes — V0.2 source

Status in this environment:
- Source/manifest privacy contract reviewed.
- No INTERNET permission is declared.
- Notification listener filters exactly `com.zing.zalo` before persistence.
- Export is user-triggered via Android ACTION_CREATE_DOCUMENT.
- Report uses per-install salted SHA-256 fingerprints for sensitive strings/identifiers.
- No Accessibility service, no Zalo API, no network client, no WebView.

Build limitation:
- The current execution environment does not contain Android SDK / build-tools / Gradle cache and cannot download Android SDK binaries, therefore an APK cannot be truthfully compiled or device-tested here.
- Project targets compile/target SDK 35, min SDK 26, Java 17, Android Gradle Plugin 8.7.3.
