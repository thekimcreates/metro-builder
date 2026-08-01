# Milestone 3.2 Testing — Builder Wand Placement

Milestone 3.2 adds server-authoritative precision PSD placement. PSD rendering is not included yet.

## Build and launch

1. Build the regular JAR through GitHub Actions.
2. Install the regular JAR, not the sources JAR.
3. Launch Minecraft 1.20.4 with Fabric API.
4. Create or load a Creative Mode world.

## Placement tests

1. Obtain the Builder Wand from the MetroBuilder creative tab.
2. Right-click the top, side, and bottom faces of blocks.
3. Confirm each action-bar message contains an exact three-decimal XYZ position and yaw.
4. Right-click air and confirm a PSD data object is created roughly three blocks in front of the camera.
5. Turn to several non-cardinal angles and place more PSDs.
6. Run `/metrobuilder psd status` and confirm the count matches the number placed.

## Persistence test

1. Place at least three PSDs.
2. Save and exit the world.
3. Reopen the world.
4. Run `/metrobuilder psd status`.
5. Confirm the same PSD count remains.

## Selection/network test

Each newly placed PSD automatically becomes the player's active precision selection.

1. Place one PSD.
2. Confirm no disconnect or networking error occurs.
3. On a multiplayer server, have two players place PSDs and confirm both clients remain connected.
4. Confirm `/metrobuilder precision status` reports the combined object count.

## Survival safety test

1. Switch to Survival Mode.
2. Right-click a block and right-click air with the Builder Wand.
3. Confirm no PSD is created and the action bar says Creative Mode is required.

## Expected limitation

PSD objects do not render during Milestone 3.2. Visible TJMetro-backed rendering is the next milestone.
