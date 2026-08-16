package dev.thekimcreates.curvify.placement;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.mtr.core.data.Rail;
import org.mtr.core.tool.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Converts a smooth MTR rail into a cardinal, block-grid PSD placement plan. */
public final class RailPlacementPlanner {
    public static final double LATERAL_OFFSET = 2;
    public static final int MAX_CHANGED_BLOCKS = 20_000;
    private static final double TANGENT_SAMPLE = 0.1;

    private RailPlacementPlanner() {
    }

    public static Plan create(Rail rail, PlacementConfig config) {
        final double railLength = rail.railMath.getLength();
        final PatternMath.Coverage coverage = PatternMath.coverage(railLength, config.patternWidth());
        final List<SectionPlacement> sections = new ArrayList<>();
        final Set<BlockPos> platformPositions = new LinkedHashSet<>();
        final Set<BlockPos> occupiedColumns = new HashSet<>();
        double cursor = 0;

        for (int repeat = 0; repeat < coverage.repeats(); repeat++) {
            for (PsdType type : config.pattern()) {
                final double midpoint = cursor + type.width() / 2.0;
                final Sample sample = sample(rail, midpoint, config.startFar());
                final Direction travelDirection = cardinal(sample.tangentX, sample.tangentZ);
                // Reversing the pattern start must not also swap physical sides of the rail.
                final boolean rightSide = rightSideOfTraversal(config.leftSide(), config.startFar());
                final double normalX = -sample.tangentZ * (rightSide ? 1 : -1);
                final double normalZ = sample.tangentX * (rightSide ? 1 : -1);
                final double centerX = sample.position.x + normalX * LATERAL_OFFSET;
                final double centerZ = sample.position.z + normalZ * LATERAL_OFFSET;
                final double firstCenterX = centerX - travelDirection.getOffsetX() * (type.width() - 1) / 2.0;
                final double firstCenterZ = centerZ - travelDirection.getOffsetZ() * (type.width() - 1) / 2.0;
                final int platformY = (int) Math.floor(sample.position.y);
                final BlockPos firstBase = new BlockPos(
                        (int) Math.floor(firstCenterX),
                        platformY + 1,
                        (int) Math.floor(firstCenterZ)
                );

                final List<BlockPos> bases = new ArrayList<>(type.width());
                for (int column = 0; column < type.width(); column++) {
                    final BlockPos base = firstBase.offset(travelDirection, column);
                    if (!occupiedColumns.add(base)) {
                        throw new IllegalStateException("The curved placement overlaps itself at " + base.toShortString());
                    }
                    bases.add(base);
                    platformPositions.add(base.down());
                }

                final Direction facingTowardTrack = rightSide
                        ? travelDirection.rotateYCounterclockwise()
                        : travelDirection.rotateYClockwise();
                sections.add(new SectionPlacement(type, List.copyOf(bases), facingTowardTrack, travelDirection));
                cursor += type.width();
            }
        }

        final int changedBlocks = platformPositions.size() + occupiedColumns.size() * 3;
        if (changedBlocks > MAX_CHANGED_BLOCKS) {
            throw new IllegalStateException("Placement is too large: " + changedBlocks + " blocks");
        }

        return new Plan(
                List.copyOf(sections),
                Set.copyOf(platformPositions),
                coverage,
                railLength
        );
    }

    private static Sample sample(Rail rail, double distance, boolean reverse) {
        final double length = rail.railMath.getLength();
        final double clamped = Math.max(0, Math.min(length, distance));
        final Vector position;
        final double tangentX;
        final double tangentZ;

        if (distance <= length) {
            position = rail.railMath.getPosition(clamped, reverse);
            final double beforeDistance = Math.max(0, clamped - TANGENT_SAMPLE);
            final double afterDistance = Math.min(length, clamped + TANGENT_SAMPLE);
            final Vector before = rail.railMath.getPosition(beforeDistance, reverse);
            final Vector after = rail.railMath.getPosition(afterDistance, reverse);
            final double[] tangent = normalize(after.x - before.x, after.z - before.z);
            tangentX = tangent[0];
            tangentZ = tangent[1];
        } else {
            final Vector end = rail.railMath.getPosition(length, reverse);
            final Vector before = rail.railMath.getPosition(Math.max(0, length - TANGENT_SAMPLE), reverse);
            final double[] tangent = normalize(end.x - before.x, end.z - before.z);
            tangentX = tangent[0];
            tangentZ = tangent[1];
            final double overflow = distance - length;
            position = new Vector(end.x + tangentX * overflow, end.y, end.z + tangentZ * overflow);
        }

        return new Sample(position, tangentX, tangentZ);
    }

    private static double[] normalize(double x, double z) {
        final double length = Math.hypot(x, z);
        if (length < 1.0E-6) {
            return new double[]{1, 0};
        }
        return new double[]{x / length, z / length};
    }

    private static Direction cardinal(double x, double z) {
        if (Math.abs(x) >= Math.abs(z)) {
            return x >= 0 ? Direction.EAST : Direction.WEST;
        }
        return z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    static boolean rightSideOfTraversal(boolean leftSide, boolean reverseTraversal) {
        return !leftSide ^ reverseTraversal;
    }

    public record Plan(
            List<SectionPlacement> sections,
            Set<BlockPos> platformPositions,
            PatternMath.Coverage coverage,
            double railLength
    ) {
        public Set<BlockPos> allPositions() {
            final Set<BlockPos> positions = new LinkedHashSet<>(platformPositions);
            sections.forEach(section -> section.bases.forEach(base -> {
                positions.add(base);
                positions.add(base.up());
                positions.add(base.up(2));
            }));
            return positions;
        }
    }

    public record SectionPlacement(
            PsdType type,
            List<BlockPos> bases,
            Direction facing,
            Direction travelDirection
    ) {
        public String sideValue(int column) {
            if (type.width() == 1) {
                return "single";
            }
            final boolean clockwiseMatchesTravel = facing.rotateYClockwise() == travelDirection;
            if (clockwiseMatchesTravel) {
                return column == 0 ? "left" : "right";
            }
            return column == 0 ? "right" : "left";
        }
    }

    private record Sample(Vector position, double tangentX, double tangentZ) {
    }
}
