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

/** Converts a smooth MTR rail into one continuous, cardinal block-grid PSD path. */
public final class RailPlacementPlanner {
    public static final double LATERAL_OFFSET = 2;
    public static final int MAX_CHANGED_BLOCKS = 20_000;
    private static final double CURVE_SAMPLE_STEP = 0.1;
    private static final double TANGENT_SAMPLE = 0.1;

    private RailPlacementPlanner() {
    }

    public static Plan create(Rail rail, PlacementConfig config) {
        final double railLength = rail.railMath.getLength();
        if (!Double.isFinite(railLength) || railLength <= 0) {
            throw new IllegalArgumentException("Rail length must be positive and finite");
        }

        final List<BlockPos> canonicalPath = createCanonicalPath(rail, config.leftSide());
        if (canonicalPath.isEmpty()) {
            throw new IllegalStateException("The rail did not produce a placeable block path");
        }

        final List<BlockPos> placementPath = new ArrayList<>(canonicalPath);
        if (config.startFar()) {
            java.util.Collections.reverse(placementPath);
        }

        final PatternMath.Coverage coverage = PatternMath.coverage(placementPath.size(), config.patternWidth());
        extendForCompletePattern(rail, placementPath, coverage.placedColumns(), config.startFar());

        final List<SectionPlacement> sections = new ArrayList<>();
        final Set<BlockPos> platformPositions = new LinkedHashSet<>();
        final Set<BlockPos> occupiedColumns = new HashSet<>();
        final boolean rightSideOfTraversal = rightSideOfTraversal(config.leftSide(), config.startFar());
        int cursor = 0;

        for (int repeat = 0; repeat < coverage.repeats(); repeat++) {
            for (PsdType type : config.pattern()) {
                final Direction travelDirection = sectionDirection(placementPath, cursor, type.width());
                final int platformY = placementPath.get(cursor).getY();
                final List<BlockPos> bases = new ArrayList<>(type.width());

                for (int column = 0; column < type.width(); column++) {
                    final BlockPos pathPosition = placementPath.get(cursor + column);
                    final BlockPos base = new BlockPos(pathPosition.getX(), platformY + 1, pathPosition.getZ());
                    if (!occupiedColumns.add(base)) {
                        throw new IllegalStateException("The offset curve crosses itself at " + base.toShortString());
                    }
                    bases.add(base);
                    platformPositions.add(base.down());
                }

                final Direction facingTowardTrack = rightSideOfTraversal
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
                railLength,
                canonicalPath.size()
        );
    }

    private static List<BlockPos> createCanonicalPath(Rail rail, boolean leftSide) {
        final double railLength = rail.railMath.getLength();
        final double startDistance = Math.min(0.5, railLength / 2);
        final double endDistance = Math.max(startDistance, railLength - 0.5);
        final boolean rightSide = !leftSide;
        final List<BlockPos> path = new ArrayList<>();
        final Set<Long> visitedHorizontalPositions = new HashSet<>();

        for (double distance = startDistance; distance < endDistance; distance += CURVE_SAMPLE_STEP) {
            appendSample(rail, path, visitedHorizontalPositions, distance, rightSide);
        }
        appendSample(rail, path, visitedHorizontalPositions, endDistance, rightSide);
        return path;
    }

    private static void appendSample(
            Rail rail,
            List<BlockPos> path,
            Set<Long> visitedHorizontalPositions,
            double distance,
            boolean rightSide
    ) {
        final Sample sample = sample(rail, distance, false);
        final double normalX = -sample.tangentZ * (rightSide ? 1 : -1);
        final double normalZ = sample.tangentX * (rightSide ? 1 : -1);
        final BlockPos target = new BlockPos(
                (int) Math.floor(sample.position.x + normalX * LATERAL_OFFSET),
                (int) Math.floor(sample.position.y),
                (int) Math.floor(sample.position.z + normalZ * LATERAL_OFFSET)
        );
        appendConnected(path, visitedHorizontalPositions, target);
        if (path.size() * 4 > MAX_CHANGED_BLOCKS + PlacementConfig.MAX_PATTERN_SLOTS * 4) {
            throw new IllegalStateException("Placement is too large");
        }
    }

    /** Adds a Manhattan/Bresenham bridge so every pair of horizontal cells is cardinally adjacent. */
    private static void appendConnected(List<BlockPos> path, Set<Long> visited, BlockPos target) {
        if (path.isEmpty()) {
            path.add(target);
            visited.add(horizontalKey(target.getX(), target.getZ()));
            return;
        }

        BlockPos current = path.get(path.size() - 1);
        final int totalX = Math.abs(target.getX() - current.getX());
        final int totalZ = Math.abs(target.getZ() - current.getZ());
        final int steps = totalX + totalZ;
        if (steps == 0) {
            return;
        }

        final int targetY = target.getY();
        final int startingY = current.getY();
        final int stepX = Integer.compare(target.getX(), current.getX());
        final int stepZ = Integer.compare(target.getZ(), current.getZ());
        int movedX = 0;
        int movedZ = 0;

        for (int step = 1; step <= steps; step++) {
            final boolean canMoveX = movedX < totalX;
            final boolean canMoveZ = movedZ < totalZ;
            final boolean moveX = canMoveX && (!canMoveZ ||
                    (long) (movedX + 1) * Math.max(1, totalZ) <= (long) (movedZ + 1) * Math.max(1, totalX));
            final int nextX = current.getX() + (moveX ? stepX : 0);
            final int nextZ = current.getZ() + (moveX ? 0 : stepZ);
            if (moveX) {
                movedX++;
            } else {
                movedZ++;
            }

            final int nextY = startingY + (int) Math.round((targetY - startingY) * step / (double) steps);
            final BlockPos next = new BlockPos(nextX, nextY, nextZ);
            final long key = horizontalKey(next.getX(), next.getZ());
            if (visited.add(key)) {
                path.add(next);
            } else if (!sameHorizontal(path.get(path.size() - 1), next)) {
                // Quantized normals on very short or tight curves can momentarily step backwards.
                // Drop that loop and let the next curve sample continue from the last accepted cell.
                return;
            }
            current = next;
        }
    }

    private static void extendForCompletePattern(
            Rail rail,
            List<BlockPos> path,
            int targetColumns,
            boolean reverse
    ) {
        final Set<Long> visited = new HashSet<>();
        path.forEach(position -> visited.add(horizontalKey(position.getX(), position.getZ())));
        final Sample terminalSample = sample(rail, rail.railMath.getLength(), reverse);
        final Direction terminalDirection = cardinal(terminalSample.tangentX, terminalSample.tangentZ);

        while (path.size() < targetColumns) {
            final BlockPos last = path.get(path.size() - 1);
            final Direction discreteDirection = path.size() > 1
                    ? directionBetween(path.get(path.size() - 2), last)
                    : terminalDirection;
            BlockPos next = null;
            for (Direction candidate : List.of(
                    discreteDirection,
                    terminalDirection,
                    terminalDirection.rotateYClockwise(),
                    terminalDirection.rotateYCounterclockwise(),
                    terminalDirection.getOpposite()
            )) {
                final BlockPos candidatePosition = last.offset(candidate);
                if (!visited.contains(horizontalKey(candidatePosition.getX(), candidatePosition.getZ()))) {
                    next = candidatePosition;
                    break;
                }
            }
            if (next == null) {
                throw new IllegalStateException("No clear direction exists for the complete-pattern overflow");
            }
            visited.add(horizontalKey(next.getX(), next.getZ()));
            path.add(next);
        }
    }

    private static Direction sectionDirection(List<BlockPos> path, int cursor, int width) {
        final int firstIndex;
        final int secondIndex;
        if (width > 1 || cursor + 1 < path.size()) {
            firstIndex = cursor;
            secondIndex = cursor + 1;
        } else {
            firstIndex = Math.max(0, cursor - 1);
            secondIndex = cursor;
        }

        return directionBetween(path.get(firstIndex), path.get(secondIndex));
    }

    private static Direction directionBetween(BlockPos first, BlockPos second) {
        final int deltaX = second.getX() - first.getX();
        final int deltaZ = second.getZ() - first.getZ();
        if (Math.abs(deltaX) + Math.abs(deltaZ) != 1) {
            throw new IllegalStateException("PSD columns are not cardinally adjacent");
        }
        if (deltaX > 0) {
            return Direction.EAST;
        } else if (deltaX < 0) {
            return Direction.WEST;
        } else if (deltaZ > 0) {
            return Direction.SOUTH;
        } else {
            return Direction.NORTH;
        }
    }

    private static Sample sample(Rail rail, double distance, boolean reverse) {
        final double length = rail.railMath.getLength();
        final double clamped = Math.max(0, Math.min(length, distance));
        final Vector position = rail.railMath.getPosition(clamped, reverse);
        final double beforeDistance = Math.max(0, clamped - TANGENT_SAMPLE);
        final double afterDistance = Math.min(length, clamped + TANGENT_SAMPLE);
        final Vector before = rail.railMath.getPosition(beforeDistance, reverse);
        final Vector after = rail.railMath.getPosition(afterDistance, reverse);
        final double[] tangent = normalize(after.x - before.x, after.z - before.z);
        return new Sample(position, tangent[0], tangent[1]);
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

    private static boolean sameHorizontal(BlockPos first, BlockPos second) {
        return first.getX() == second.getX() && first.getZ() == second.getZ();
    }

    private static long horizontalKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    static boolean rightSideOfTraversal(boolean leftSide, boolean reverseTraversal) {
        return !leftSide ^ reverseTraversal;
    }

    public record Plan(
            List<SectionPlacement> sections,
            Set<BlockPos> platformPositions,
            PatternMath.Coverage coverage,
            double railLength,
            int pathColumns
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
