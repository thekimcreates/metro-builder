# MetroBuilder

MetroBuilder **v1.0.0-beta.1** is a clean rebuild for Minecraft **1.20.4 Fabric**.
Its focused purpose is precision placement and editing of custom platform screen doors,
plus a Builder Wand capable of rotating selected blocks beyond the normal Minecraft grid.

## Current milestone

This foundation milestone contains:

- Clean Fabric 1.20.4 project
- Java 17 toolchain
- MetroBuilder common and client entry points
- Builder Wand item
- MetroBuilder Creative inventory tab
- `/metrobuilder version` diagnostic command
- GitHub Actions build workflow
- No inherited alpha code, mixins, networking, display entities, or platform generator

The Builder Wand is registered but its edit modes are intentionally not implemented in this
milestone. The next milestone begins the precision object model and selection system.

## Build

```bash
./gradlew build
```

The output JAR is written to `build/libs/`.

## In-game verification

1. Install Fabric Loader and Fabric API for Minecraft 1.20.4.
2. Put the built MetroBuilder JAR in the `mods` folder.
3. Launch Minecraft and open Creative inventory.
4. Find the **MetroBuilder** tab and confirm the **Builder Wand** appears.
5. Run `/metrobuilder version` and confirm it reports `1.0.0-beta.1`.

## Roadmap

- **Beta 1:** precision PSD editor, custom PSD resources, save/load, multiplayer, Builder Wand
- **Beta 2:** MTR train-door synchronization
