# Christmas Dream Launcher

Native Android launcher prototype for phones and Android TV.

## Build
The GitHub Actions workflow builds a debug APK on pushes to `main` and manual workflow dispatch. Open the Actions tab and download the `Christmas-Dream-Launcher-debug` artifact.

## Compatibility
- minSdk 23
- Android SDK 35 compile target
- Phone touch and Android TV remote focus support
- Does not modify firmware, system partitions, or disable the stock launcher.

This is an initial functional foundation. Device-specific validation on Realme Note 50 and TCL/Union TV is still required.
