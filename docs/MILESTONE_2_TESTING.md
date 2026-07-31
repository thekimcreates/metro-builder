# Milestone 2 Testing

Milestone 2 is infrastructure-only. It does not place or render PSDs yet.

1. Launch Minecraft with Fabric API and MetroBuilder.
2. Open or create a world.
3. Run `/metrobuilder version` and confirm `1.0.0-beta.1`.
4. Run `/metrobuilder precision status`.
5. Confirm the response reports `Precision engine READY` and `objects=0`.
6. Save, quit, and reopen the world. Run the status command again.
7. Test once with only Fabric API, then with the normal MTR/TJMetro profile.
8. For multiplayer, install the same JAR on client and server, join, and run both commands.

Expected: no crash, no registry mismatch, and no visible PSD behavior yet.
