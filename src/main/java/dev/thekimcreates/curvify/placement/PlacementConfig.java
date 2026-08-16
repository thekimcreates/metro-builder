package dev.thekimcreates.curvify.placement;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PlacementConfig(
        List<PsdType> pattern,
        Identifier platformBlockItemId,
        boolean startFar,
        boolean leftSide
) {
    public static final int MAX_PATTERN_SLOTS = 9;
    public static final PlacementConfig DEFAULT = new PlacementConfig(
            List.of(PsdType.DOOR_1, PsdType.GLASS_1),
            new Identifier("minecraft:smooth_stone"),
            false,
            false
    );

    public PlacementConfig {
        pattern = List.copyOf(Objects.requireNonNull(pattern, "pattern"));
        platformBlockItemId = Objects.requireNonNull(platformBlockItemId, "platformBlockItemId");
        if (pattern.isEmpty() || pattern.size() > MAX_PATTERN_SLOTS) {
            throw new IllegalArgumentException("PSD pattern must contain 1 to 9 items");
        }
    }

    public int patternWidth() {
        return pattern.stream().mapToInt(PsdType::width).sum();
    }

    public void write(PacketByteBuf buffer) {
        buffer.writeVarInt(pattern.size());
        pattern.forEach(type -> buffer.writeIdentifier(type.itemId()));
        buffer.writeIdentifier(platformBlockItemId);
        buffer.writeBoolean(startFar);
        buffer.writeBoolean(leftSide);
    }

    public static PlacementConfig read(PacketByteBuf buffer) {
        final int size = buffer.readVarInt();
        if (size < 1 || size > MAX_PATTERN_SLOTS) {
            throw new IllegalArgumentException("Invalid PSD pattern size: " + size);
        }

        final List<PsdType> pattern = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            final Identifier itemId = buffer.readIdentifier();
            pattern.add(PsdType.fromItemId(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported PSD item: " + itemId)));
        }

        return new PlacementConfig(
                pattern,
                buffer.readIdentifier(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }
}
