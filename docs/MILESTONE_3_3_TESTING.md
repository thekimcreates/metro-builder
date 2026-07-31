# Milestone 3.3 Testing — First Visible Precision PSD

## Required mods

- Fabric Loader
- Fabric API
- MetroBuilder Milestone 3.3
- Tianjin Metro is recommended for the intended textures

## Tests

1. Launch Minecraft and enter a Creative world.
2. Obtain the Builder Wand from the MetroBuilder creative tab.
3. Right-click blocks and air to place multiple PSD objects.
4. Confirm that each PSD becomes visible immediately.
5. Face several different directions before placing PSDs and confirm that their yaw is not snapped to 90-degree angles.
6. Run `/metrobuilder psd status` and confirm its count matches the visible PSDs.
7. Exit and reopen the world. Confirm every PSD returns at the same position and rotation.
8. Test with Tianjin Metro installed. The renderer should use the installed TJMetro door textures.
9. Test once without Tianjin Metro. PSDs should use the iron-block fallback texture and the game should not crash.
10. Place PSDs farther than 192 blocks away and confirm they stop rendering at long range.

## Expected limitations

- The PSD is currently a static, flat double-sided model.
- Selection outlines, movement, rotation controls, duplication, deletion, animation, and sounds are not included yet.
- The renderer references TJMetro textures at runtime; it does not redistribute TJMetro texture files.
