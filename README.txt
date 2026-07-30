MetroBuilder Alpha 5 - Precision PSD Builder

Copy these files into the project, preserving paths.

Controls:
- Right-click: create preview; right-click again: confirm placement
- [ and ]: rotate 1 degree
- Hold Shift with controls: 0.25 degree rotation / 0.001 block movement
- Arrow keys: nudge horizontally
- Page Up / Page Down: nudge vertically
- G: toggle Tianjin BMT PSD door / MTR PSD glass
- X: cancel preview

Required mods at runtime:
- MTR 4.0.5+ for mtr:psd_glass
- Tianjin Metro 1.1.1+ for tjmetro:psd_door_tianjin_bmt

Technical note:
This first implementation uses Minecraft Block Display entities to support arbitrary yaw and sub-block positioning. It renders the registered block state. The Tianjin door's custom block-entity animation may not appear because the original renderer expects a real TJMetro block entity; this update is the placement/rendering foundation for the compatibility renderer.
