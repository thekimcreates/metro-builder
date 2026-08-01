# Seoul Metro Bulky White — Sharp Model Fix Testing

1. Extract the update into the project root and replace matching files.
2. Build the regular JAR with GitHub Actions.
3. Replace the previous MetroBuilder JAR in the test instance.
4. Press `F3 + T` after entering a world if testing an existing resource cache.
5. Place a new `Seoul Metro Bulky White` PSD and verify:
   - no giant diagonal or triangular planes;
   - a straight, sharp rectangular header;
   - full-height 1.5-block glass panels on both sides;
   - two one-block-wide centered doors;
   - solid white frame geometry with dark inset frames;
   - caution signs on both doors;
   - platform number on the right door only;
   - fixed side glass and frames visibly in front of the rear sliding-door rail;
   - clear side pockets that accept each moving door leaf without clipping;
   - centered, collision-free header text with no standalone far-left line number;
   - no standalone line-number badge at the far-left side of the header.

The primary rendering correction emits each triangulated OBJ face as a degenerate quad because Minecraft's entity render layers use QUADS. The previous three-vertex emission merged unrelated triangles into giant malformed quads.
