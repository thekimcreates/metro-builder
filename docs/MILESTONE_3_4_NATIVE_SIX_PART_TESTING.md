# Milestone 3.4 Native Six-Part PSD Testing

## Install
Replace the three Java files from the update ZIP, rebuild the normal JAR, remove the previous MetroBuilder JAR, and launch with TJMetro 1.1.1 and MTR 4.0.5.

## Visual checks
1. Place one PSD in a bright station and one in a dim station.
2. Confirm there is no extra flat pane in front of or behind the top section.
3. Confirm the complete assembly is exactly two blocks wide and three blocks tall.
4. Confirm the lower-right quarter uses the right-side texture and geometry.
5. View the assembly from front, back, left, right, above, and below.
6. Place doors at 0°, 15°, 37.5°, 90°, and 179° and confirm all six pieces stay connected.
7. Confirm nearby light sources affect the door and top pieces instead of the model appearing full-bright.

## Expected six-part layout
- Top left: `tjmetro:psd_top_tianjin_bmt[air_left=false,air_right=false,arrow_direction=1,side=left,style=bmt]`
- Top right: `tjmetro:psd_top_tianjin_bmt[air_left=false,air_right=false,arrow_direction=1,side=right,style=bmt]`
- Middle left: `tjmetro:psd_door_tianjin_bmt[end=false,half=upper,side=left,unlocked=true]`
- Middle right: `tjmetro:psd_door_tianjin_bmt[end=false,half=upper,side=right,unlocked=true]`
- Bottom left: `tjmetro:psd_door_tianjin_bmt[end=false,half=lower,side=left,unlocked=true]`
- Bottom right: `tjmetro:psd_door_tianjin_bmt[end=false,half=lower,side=right,unlocked=true]`

The bottom-right side is intentionally `right`; using `left` there would duplicate the left door quarter.
