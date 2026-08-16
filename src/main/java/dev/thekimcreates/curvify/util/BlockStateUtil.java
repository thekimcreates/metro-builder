package dev.thekimcreates.curvify.util;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

import java.util.Optional;

public final class BlockStateUtil {
    private BlockStateUtil() {
    }

    public static BlockState with(BlockState state, String propertyName, String serializedValue) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return applyParsed(state, property, serializedValue);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyParsed(
            BlockState state,
            Property<T> property,
            String serializedValue
    ) {
        final Optional<T> parsed = property.parse(serializedValue);
        return parsed.map(value -> state.with(property, value)).orElse(state);
    }
}
