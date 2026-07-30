MetroBuilder Precision PSD Builder update

Replace these files:
- src/main/java/dev/metrobuilder/display/PrecisionPSDManager.java
- src/main/java/dev/metrobuilder/item/PrecisionPSDBuilderItem.java

Changes:
- mtr:psd_top is rendered above each mtr:psd_glass assembly.
- Right-click an already placed PSD/display while looking at it to select the entire grouped assembly.
- Selected assemblies glow and can be rotated/nudged with the existing controls.
- Right-click again saves the edited assembly.
- Invisible, invulnerable shulker collision columns prevent players from walking through placed assemblies.

Collision note:
Minecraft entity collision boxes remain axis-aligned. The invisible collision column follows position but cannot itself rotate to an arbitrary angle. It provides a solid practical barrier, but at steep custom angles its physical outline will be an approximation.
