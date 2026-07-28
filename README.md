# MetroBuilder

**Build beyond the grid.**

MetroBuilder is a Fabric construction toolkit intended for Minecraft Transit Railway station builders.

## Current development controls

- `]` — next tool
- `[` — previous tool
- `P` — place a white-concrete block display at the crosshair

These controls are temporary development defaults and can be changed through Minecraft's Controls menu.

## Requirements

- Minecraft 1.20.4
- Fabric Loader 0.15.11 or newer
- Fabric API
- Java 17 or newer
- Minecraft Transit Railway is recommended for the intended use case

## Build

```bash
./gradlew build
```

The release JAR will appear under `build/libs/`.

## Status

This is an alpha foundation. Selection, transforms, history, rail snapping, curved platforms, and PSD generation are planned but are not represented as complete features.
