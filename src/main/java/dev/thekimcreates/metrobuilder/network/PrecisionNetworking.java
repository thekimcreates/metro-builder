package dev.thekimcreates.metrobuilder.network;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.precision.PrecisionSaveData;
import dev.thekimcreates.metrobuilder.precision.PrecisionSelectionManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;

/** Network channels shared by the precision engine. */
public final class PrecisionNetworking {
    public static final Identifier SNAPSHOT = MetroBuilder.id("precision_snapshot");
    public static final Identifier SELECTION_REQUEST = MetroBuilder.id("precision_selection_request");
    public static final Identifier SELECTION_STATE = MetroBuilder.id("precision_selection_state");

    private static boolean initialized;

    private PrecisionNetworking() {
    }

    public static synchronized void initializeServer() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerPlayNetworking.registerGlobalReceiver(
                SELECTION_REQUEST,
                (server, player, handler, buffer, responseSender) -> {
                    final boolean hasRequestedSelection = buffer.readBoolean();
                    final UUID requestedObjectId = hasRequestedSelection ? buffer.readUuid() : null;

                    server.execute(() -> {
                        if (requestedObjectId == null) {
                            PrecisionSelectionManager.clear(player);
                        } else {
                            PrecisionSelectionManager.select(player, requestedObjectId);
                        }
                        sendSelectionState(player);
                    });
                }
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendSnapshot(handler.getPlayer());
            sendSelectionState(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> PrecisionSelectionManager.clear(handler.getPlayer())
        );

        MetroBuilder.LOGGER.info("Precision networking initialized");
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        final ServerWorld world = player.getServerWorld();
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeIdentifier(world.getRegistryKey().getValue());
        buffer.writeNbt(PrecisionSaveData.get(world).manager().createSnapshot());
        ServerPlayNetworking.send(player, SNAPSHOT, buffer);
    }

    public static void broadcastSnapshot(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            sendSnapshot(player);
        }
    }

    public static void sendSelectionState(ServerPlayerEntity player) {
        final Optional<PrecisionSelectionManager.Selection> selection = PrecisionSelectionManager.current(player);
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(selection.isPresent());
        selection.ifPresent(value -> {
            buffer.writeIdentifier(value.dimensionId());
            buffer.writeUuid(value.objectId());
        });
        ServerPlayNetworking.send(player, SELECTION_STATE, buffer);
    }
}
