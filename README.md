# My Ride — Android Native

Native Android starter project for POCO F4 GT / Android 14. It is an independent receipt organizer inspired by common ride-hailing app navigation patterns; it is not an official Gojek app.

## Included in this build
- Native Kotlin Android Activity and native Android widgets (not a WebView/PWA UI).
- Local SQLite receipt database.
- Import image/screenshot and PDF using Android document picker.
- On-device ML Kit text recognition for image imports; extracted text pre-fills some fields, and the user reviews the values before saving.
- Receipt list, basic search and service filters.
- Receipt detail and PDF export using Android's Save As dialog.
- OpenStreetMap map view with a clearly labeled sample historical route.

## Important limitations of v1
- OCR is a helper, not guaranteed extraction. The user must verify and complete fields.
- PDF import is attached to the record but its text is not OCR-parsed in this version.
- Map screen currently displays a sample Jakarta route. It does not infer actual trip coordinates from addresses or simulate a vehicle moving along a saved route yet.
- This app does not book rides, contact drivers, make payments, or connect to Gojek accounts.
- OpenStreetMap tiles require internet. Stored receipts remain local on the phone.

## Build APK with Android Studio
This environment does not have Android SDK/Gradle installed, so an APK could not be compiled here. To create the installable APK:
1. Install Android Studio on a Windows/Mac/Linux computer.
2. Open this `MyRideNative` folder as an existing Android Studio project.
3. Allow Gradle sync and install Android SDK Platform 35 if prompted.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfer that APK to the POCO F4 GT, open it, and allow **Install unknown apps** for the file manager/browser used to open it.

Minimum Android version: 8.0 (API 26). Target SDK: 35. Package: `com.myride.app`.

## Build APK tanpa Android Studio
Project ini menyertakan workflow GitHub Actions di `.github/workflows/build-apk.yml`. Ikuti panduan `BUILD_TANPA_ANDROID_STUDIO.md`. GitHub akan mengompilasi APK di cloud dan menyediakan artifact `MyRide-debug-apk` untuk diunduh.
