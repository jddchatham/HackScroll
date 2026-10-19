# HackScroll V1

TikTok-style scroller for Hackaday articles (frozen v1).

## Features
RSS from hackaday.com/feed/ · vertical snap pager · offline cache

## Build (fully inside Termux, no Android Studio)

Toolchain: `aapt2`, `ecj`, `dx`, `zipalign`, `apksigner` (Termux packages), plus an API-33 `android.jar`.

```sh
./build.sh
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (debug-signed; a keystore is auto-generated on first build).

Pure framework Java, no dependencies, no Gradle, minSdk 21.
