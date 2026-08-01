# MetroBuilder v1.0.0-beta.1 — Milestone 3.1 Testing

Milestone 3.1 adds the first concrete precision-object type: a persistent PSD data object.
There is no PSD renderer or Builder Wand placement interaction yet.

## 1. Build and launch

- GitHub Actions must finish successfully.
- Install the regular MetroBuilder JAR, not the sources JAR.
- Minecraft must reach the main menu and load a world normally.

## 2. Confirm registration

Run:

```mcfunction
/metrobuilder precision status
```

Expected: the precision engine reports READY.

Then run:

```mcfunction
/metrobuilder psd status
```

Expected: `PSD core READY` and `psds=0` in a new world.

## 3. Create persistent PSD data

Stand anywhere and run:

```mcfunction
/metrobuilder psd create
```

Expected:

- A new UUID is printed.
- Pack is `metrobuilder:tjmetro_default`.
- Position matches the player's exact position.
- Yaw matches the player's facing direction.
- No visible model appears yet; rendering begins in a later Milestone 3 commit.

Run `/metrobuilder psd status` again. Expected: `psds=1`.

## 4. Save and reload

- Save and exit the world.
- Reopen the same world.
- Run `/metrobuilder psd status`.

Expected: the count remains unchanged.

## 5. Multiple objects

Run `/metrobuilder psd create` several times from different positions.
Expected: every object receives a unique UUID and the count increases.

## 6. Clear test data

With operator permission, run:

```mcfunction
/metrobuilder psd clear
```

Expected: all PSD objects are removed while the precision engine remains available.

## Not implemented yet

- PSD rendering
- Builder Wand PSD placement
- Selection by looking at a PSD
- Move/rotate/duplicate/delete interactions
- Custom textures and sounds
- MTR train-door synchronization
