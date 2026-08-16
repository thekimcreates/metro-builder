package dev.thekimcreates.curvify.placement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PatternMathTest {
    @Test
    void completePatternOverflowsHundredBlockRail() {
        final PatternMath.Coverage coverage = PatternMath.coverage(100, 3);
        assertEquals(34, coverage.repeats());
        assertEquals(102, coverage.placedColumns());
        assertEquals(2, coverage.overflow());
    }

    @Test
    void exactPatternLengthDoesNotOverflow() {
        final PatternMath.Coverage coverage = PatternMath.coverage(99, 3);
        assertEquals(33, coverage.repeats());
        assertEquals(99, coverage.placedColumns());
        assertEquals(0, coverage.overflow());
    }

    @Test
    void invalidLengthsAndWidthsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PatternMath.coverage(0, 3));
        assertThrows(IllegalArgumentException.class, () -> PatternMath.coverage(Double.NaN, 3));
        assertThrows(IllegalArgumentException.class, () -> PatternMath.coverage(100, 0));
    }

    @Test
    void reversingPatternStartKeepsTheSelectedPhysicalSide() {
        assertEquals(true, RailPlacementPlanner.rightSideOfTraversal(false, false));
        assertEquals(false, RailPlacementPlanner.rightSideOfTraversal(false, true));
        assertEquals(false, RailPlacementPlanner.rightSideOfTraversal(true, false));
        assertEquals(true, RailPlacementPlanner.rightSideOfTraversal(true, true));
    }
}
