# Milestone 5 — PSD Pack Catalog Testing

## Build and launch

1. Run the GitHub Actions build.
2. Install the regular MetroBuilder JAR, Fabric API, and TJMetro.
3. Launch Minecraft 1.20.4 and open a Creative world.

## Built-in pack

1. Hold the Builder Wand.
2. Right-click once to create a pending PSD.
3. Press `P`.
4. Confirm the properties panel contains a **PSD Pack** button showing:

   `TJMetro BMT Default`

5. Save and place the PSD.
6. Right-click the placed PSD and confirm the same pack is shown.
7. Save and reload the world. The pack must remain unchanged.

## Resource reload

1. Press `F3 + T` to reload resources.
2. Confirm the game does not crash.
3. Check `latest.log` for a line similar to:

   `Loaded 1 PSD pack definition(s)`

4. Reopen PSD properties and confirm the built-in pack remains selectable.

## Custom catalog smoke test

Create a temporary resource pack containing:

`assets/example/psd_packs/test.json`

```json
{
  "format_version": 1,
  "name": "Catalog Test",
  "renderer": "metrobuilder:tjmetro_bmt",
  "required_mod": "tjmetro",
  "volume": 1.0,
  "pitch": 1.0
}
```

Enable the resource pack and reload resources. The properties pack button should cycle between:

- TJMetro BMT Default
- Catalog Test

Place a PSD using **Catalog Test**, reload the world, and verify that the selection persists.

## Current boundary

Milestone 5 loads and persists PSD pack metadata. The only renderer currently implemented is `metrobuilder:tjmetro_bmt`, so test packs using that renderer intentionally reuse TJMetro's native BMT appearance. Custom texture/model rendering is added when custom PSD assets are supplied. Opening and closing sounds are reserved for Beta 2 door synchronization.
