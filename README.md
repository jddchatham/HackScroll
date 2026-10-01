# HackScroll

HackScroll is a Linux-native and Android reader for Hackaday. Browse the latest hacks, sort by newest/top/random, search Hackaday, and move through articles with scrolling or the on-screen controls.

## Contents

- `linux-native/` — Linux desktop/web app files and launcher
- `source/app/` — original Android app source and resources
- `source/v2/` — Android v2 source and resources

The APK files and compiled bytecode are local build artifacts and are intentionally excluded from Git. If you want to distribute APKs, attach them to a GitHub Release instead.

## Linux

Run the included launcher:

```sh
./linux-native/hackscroll
```

The launcher expects the app files in `linux-native/app/` and uses the system's available web runtime.

## Android

The Android directories contain the recovered Java sources and resources for the two app variants. A Gradle project is not included yet, so rebuilding Android packages requires recreating the Android build configuration around these sources.

## Data and privacy

HackScroll reads public Hackaday content over the network. It does not require an account or API key.

## License

No license has been selected yet. Add a `LICENSE` file before accepting contributions or allowing reuse.
