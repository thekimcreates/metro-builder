# MetroBuilder PSD Pack Format — Version 1

PSD pack definitions are client resource-pack JSON files stored at:

`assets/<namespace>/psd_packs/<path>.json`

The resulting pack ID is `<namespace>:<path>`.

Example:

`assets/example/psd_packs/airport/bmt.json`

becomes:

`example:airport/bmt`

## Schema

```json
{
  "format_version": 1,
  "name": "TJMetro BMT Default",
  "renderer": "metrobuilder:tjmetro_bmt",
  "required_mod": "tjmetro",
  "opening_sound": "example:psd_open",
  "closing_sound": "example:psd_close",
  "volume": 1.0,
  "pitch": 1.0
}
```

### Fields

- `format_version` — Required pack schema version. Current value: `1`.
- `name` — Display name shown in the PSD properties panel.
- `renderer` — Renderer implementation ID. Beta 1 currently provides `metrobuilder:tjmetro_bmt`.
- `required_mod` — Optional mod ID required by the renderer.
- `opening_sound` — Optional sound event ID reserved for Beta 2.
- `closing_sound` — Optional sound event ID reserved for Beta 2.
- `volume` — Optional sound volume; defaults to `1.0`.
- `pitch` — Optional positive sound pitch; defaults to `1.0`.

Pack IDs are stored with each PSD in world data. If a client lacks a selected pack, MetroBuilder preserves the pack ID and renders its safe fallback instead of deleting or rewriting the PSD.
