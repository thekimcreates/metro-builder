package dev.metrobuilder.precision;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral, server-authoritative selection state. Selections are intentionally
 * not written to the world save.
 */
public final class PrecisionSelectionManager {
    private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

    private PrecisionSelectionManager() {
    }

    public static boolean select(ServerPlayerEntity player, UUID objectId) {
        PrecisionObjectManager manager = PrecisionObjectManager.get(player.getServerWorld());
        if (manager.get(objectId).isEmpty()) {
            return false;
        }
        SELECTIONS.put(player.getUuid(), new Selection(
                player.getServerWorld().getRegistryKey(),
                objectId,
                player.getServerWorld().getTime()
        ));
        return true;
    }

    public static Optional<Selection> getSelection(ServerPlayerEntity player) {
        Selection selection = SELECTIONS.get(player.getUuid());
        if (selection == null) {
            return Optional.empty();
        }
        if (!selection.world().equals(player.getServerWorld().getRegistryKey())) {
            SELECTIONS.remove(player.getUuid(), selection);
            return Optional.empty();
        }
        if (PrecisionObjectManager.get(player.getServerWorld()).get(selection.objectId()).isEmpty()) {
            SELECTIONS.remove(player.getUuid(), selection);
            return Optional.empty();
        }
        return Optional.of(selection);
    }

    public static Optional<PrecisionObject> getSelectedObject(ServerPlayerEntity player) {
        return getSelection(player).flatMap(selection ->
                PrecisionObjectManager.get(player.getServerWorld()).get(selection.objectId())
        );
    }

    public static void clear(ServerPlayerEntity player) {
        SELECTIONS.remove(player.getUuid());
    }

    public static void clearPlayer(UUID playerId) {
        SELECTIONS.remove(playerId);
    }

    public static void clearObject(UUID objectId) {
        SELECTIONS.entrySet().removeIf(entry -> entry.getValue().objectId().equals(objectId));
    }

    public static void clearWorld(RegistryKey<World> world) {
        SELECTIONS.entrySet().removeIf(entry -> entry.getValue().world().equals(world));
    }

    public static void clearAll() {
        SELECTIONS.clear();
    }

    public record Selection(RegistryKey<World> world, UUID objectId, long selectedAtWorldTime) {
    }
}
