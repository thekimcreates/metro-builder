package dev.thekimcreates.metrobuilder.precision;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative, non-persistent precision-object selections. */
public final class PrecisionSelectionManager {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private PrecisionSelectionManager() {
    }

    public static boolean select(ServerPlayerEntity player, UUID objectId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(objectId, "objectId");

        final ServerWorld world = player.getServerWorld();
        if (!PrecisionSaveData.get(world).manager().contains(objectId)) {
            clear(player);
            return false;
        }

        SELECTIONS.put(
                player.getUuid(),
                new Selection(world.getRegistryKey().getValue(), objectId)
        );
        return true;
    }

    public static void clear(ServerPlayerEntity player) {
        Objects.requireNonNull(player, "player");
        SELECTIONS.remove(player.getUuid());
    }

    public static Optional<Selection> current(ServerPlayerEntity player) {
        Objects.requireNonNull(player, "player");
        final Selection selection = SELECTIONS.get(player.getUuid());
        if (selection == null) {
            return Optional.empty();
        }

        final ServerWorld world = player.getServerWorld();
        if (!selection.dimensionId().equals(world.getRegistryKey().getValue())
                || !PrecisionSaveData.get(world).manager().contains(selection.objectId())) {
            SELECTIONS.remove(player.getUuid());
            return Optional.empty();
        }

        return Optional.of(selection);
    }

    public static void clearAll() {
        SELECTIONS.clear();
    }

    public record Selection(Identifier dimensionId, UUID objectId) {
        public Selection {
            Objects.requireNonNull(dimensionId, "dimensionId");
            Objects.requireNonNull(objectId, "objectId");
        }
    }
}
