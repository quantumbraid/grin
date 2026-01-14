# GRIN Dedicated Camera (Android)

## Overview
The Dedicated Camera project is a standalone Android build derived from the GRIN demo. It focuses only on the posterized grid camera preview and capture review workflow, removing the gallery/editor/demo playback screens.

## Modules
- `:demo` — the dedicated camera application module.

## Build
From the `Dedicated Camera` directory:

```bash
./gradlew :demo:assembleDebug
```

## Run
Open the `Dedicated Camera` folder in Android Studio and run the `demo` configuration on a device or emulator with a camera.
