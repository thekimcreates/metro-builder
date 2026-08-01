# Seoul Metro Bulky White Mesh Renderer Test

1. Build the regular MetroBuilder JAR and replace the previous JAR in the instance.
2. Launch Minecraft 1.20.4 with Fabric API.
3. Place a new PSD and select **Seoul Metro Bulky White** in its properties.
4. Confirm the assembly is five blocks wide and rotates around the center between the two doors.
5. Check that the side glass panels are clean and that both door leaves have a dark inset frame.
6. Confirm warning panels appear on both doors and the platform number appears only on the right door.
7. Test several rotations, including non-cardinal angles, and check the front, back, top, and side depth.
8. Press `F3 + T`; the OBJ assets should reload without disappearing.
9. Save and reopen the world and confirm the PSD properties and transform remain unchanged.

The new mesh resources are under:

`assets/metrobuilder/models/psd/seoul_bulky_white/`

The authored model-generation script is:

`tools/generate_seoul_bulky_white_models.py`
