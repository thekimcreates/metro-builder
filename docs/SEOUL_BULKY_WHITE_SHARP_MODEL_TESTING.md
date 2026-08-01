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
   - the current station number immediately left of the current station name;
   - two-block-tall doors and glass from the configured Y level, with a one-block header above;
   - caution labels raised by 0.25 blocks and a thinner gray platform-number label beneath;
   - clear, untinted glass and a borderless half-block-wide door indicator;
   - no standalone line-number badge at the far-left side of the header.

The primary rendering correction emits each triangulated OBJ face as a degenerate quad because Minecraft's entity render layers use QUADS. The previous three-vertex emission merged unrelated triangles into giant malformed quads.
