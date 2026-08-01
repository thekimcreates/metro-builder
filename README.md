# MetroBuilder

MetroBuilder **v1.0.0-beta.1** is a focused Minecraft **1.20.4 Fabric** mod for precision placement and editing of platform screen doors.

## Current features

- Native six-part TJMetro BMT PSD rendering
- Exact 2 × 3 PSD layout with native TJMetro models and textures
- Arbitrary position and rotation outside the Minecraft block grid
- Persistent per-world PSD objects
- Multiplayer synchronization
- Unified Builder Wand
- Pending preview and explicit second-click confirmation
- `[` / `]` one-degree rotation
- Arrow-key precision movement
- Properties panel with permanent deletion
- Data-driven PSD pack catalog and pack selection

## Controls

- **Right-click empty space:** create a pending PSD preview
- **Right-click again:** confirm the preview at its current position
- **Right-click existing PSD:** select it and open properties
- **`[` / `]`:** rotate −1° / +1°
- **Arrow keys:** move by 0.01 blocks relative to the PSD
- **Shift + Arrow:** move by 0.001 blocks
- **Page Up / Page Down:** move vertically
- **`P`:** open properties for the pending or selected PSD

## PSD packs

PSD metadata is loaded from client resource packs under:

`assets/<namespace>/psd_packs/*.json`

The built-in pack is **TJMetro BMT Default**. See `docs/PSD_PACK_FORMAT.md` for the version-1 format. Custom model and texture assets will be added when those resources are supplied; Beta 2 will use the optional opening and closing sound metadata for MTR door synchronization.

## Build

```bash
./gradlew build
```

The regular mod JAR is written to `build/libs/`. Do not install the `-sources.jar`.

## Roadmap

- **Beta 1:** precision PSD editor, pack metadata, custom PSD resources, save/load, multiplayer
- **Beta 2:** MTR train-door synchronization and pack-defined sounds
- **Beta 2/3:** undo and redo
