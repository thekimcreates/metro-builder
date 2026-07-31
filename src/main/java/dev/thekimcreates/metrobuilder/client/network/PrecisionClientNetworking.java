package dev.thekimcreates.metrobuilder.client.network;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.network.PrecisionNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Registers client receivers for precision snapshots and selection state. */
public final class PrecisionClientNetworking {
    private static boolean initialized;

    private PrecisionClientNetworking() {
    }

    public static synchronized void initializeClient() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientPlayNetworking.registerGlobalReceiver(
                PrecisionNetworking.SNAPSHOT,
                (client, handler, buffer, responseSender) -> {
                    final Identifier dimensionId = buffer.readIdentifier();
                    final NbtCompound receivedSnapshot = buffer.readNbt();
                    final NbtCompound safeSnapshot = receivedSnapshot == null
                            ? new NbtCompound()
                            : receivedSnapshot.copy();
                    client.execute(() -> ClientPrecisionState.applySnapshot(dimensionId, safeSnapshot));
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PrecisionNetworking.SELECTION_STATE,
                (client, handler, buffer, responseSender) -> {
                    final boolean hasSelection = buffer.readBoolean();
                    final Identifier dimensionId = hasSelection ? buffer.readIdentifier() : null;
                    final UUID objectId = hasSelection ? buffer.readUuid() : null;
                    client.execute(() -> {
                        if (dimensionId == null || objectId == null) {
                            ClientPrecisionState.clearSelection();
                        } else {
                            ClientPrecisionState.applySelection(dimensionId, objectId);
                        }
                    });
                }
        );

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientPrecisionState.reset()
        );

        MetroBuilder.LOGGER.info("Precision client networking initialized");
    }

    public static void requestSelection(UUID objectId) {
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(true);
        buffer.writeUuid(objectId);
        ClientPlayNetworking.send(PrecisionNetworking.SELECTION_REQUEST, buffer);
    }

    public static void requestSelectionClear() {
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(false);
        ClientPlayNetworking.send(PrecisionNetworking.SELECTION_REQUEST, buffer);
    }
}
