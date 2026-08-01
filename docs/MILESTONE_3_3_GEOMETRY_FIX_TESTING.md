# Milestone 3.3 PSD Geometry and Texture Fix

## Expected dimensions
- Width: exactly 2 blocks
- Height: exactly 3 blocks
- Door body: 2 blocks wide by 2 blocks tall
- Header: 2 blocks wide by 1 block tall

## Expected depth
- Door leaves: 0.125 blocks, matching TJMetro's 2/16 model depth
- BMT header: 0.625 blocks deep

## Texture verification
- Each of the four door textures must use its original TJMetro cuboid-atlas UV regions.
- The front and back should no longer display the entire 72 x 36 atlas stretched over one flat quad.
- Door side, top, and bottom faces should be visible.
- The header should use the BMT top and edge textures.

## Test procedure
1. Place one PSD on a flat floor.
2. Count its silhouette against the block grid: 2 blocks wide and 3 blocks tall.
3. Walk around both sides and verify it is a solid-looking 3D object rather than a paper-thin plane.
4. Place additional PSDs at several arbitrary yaw angles.
5. Save and reload the world and verify dimensions and textures remain unchanged.
