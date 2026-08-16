package dev.thekimcreates.curvify.placement;

public final class PatternMath {
    private PatternMath() {
    }

    public static Coverage coverage(double railLength, int patternWidth) {
        if (!Double.isFinite(railLength) || railLength <= 0) {
            throw new IllegalArgumentException("Rail length must be positive and finite");
        }
        if (patternWidth <= 0) {
            throw new IllegalArgumentException("Pattern width must be positive");
        }

        final int repeats = Math.max(1, (int) Math.ceil(railLength / patternWidth));
        final int placedColumns = Math.multiplyExact(repeats, patternWidth);
        final double overflow = placedColumns - railLength;
        return new Coverage(repeats, placedColumns, overflow);
    }

    public record Coverage(int repeats, int placedColumns, double overflow) {
    }
}
