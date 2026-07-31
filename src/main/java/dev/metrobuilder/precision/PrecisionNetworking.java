package dev.metrobuilder.precision;

import dev.metrobuilder.MetroBuilder;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Network foundation for the precision engine. Stage 1 exposes read-only world
 * snapshots and server-authoritative selection without depending on MTR.
 */
public final class PrecisionNetworking {
    public static final Identifier REQUEST_SNAPSHOT = new Identifier(MetroBuilder.MOD_ID, "precision_request_snapshot");
    public static final Identifier SYNC_SNAPSHOT = new Identifier(MetroBuilder.MOD_ID, "precision_sync_snapshot");
    public static final Identifier SELECT_OBJECT = new Identifier(MetroBuilder.MOD_ID, "precision_select_object");
    public static final Identifier CLEAR_SELECTION = new Identifier(MetroBuilder.MOD_ID, "precision_clear_selection");
    public static final Identifier SYNC_SELECTION = new Identifier(MetroBuilder.MOD_ID, "precision_sync_selection");

    private static final double MAX_SELECTION_DISTANCE = 64.0;
    private static final int MAX_SNAPSHOT_OBJECTS = 16_384;
    private static boolean registered;

    private PrecisionNetworking() {
    }

    public static synchronized void registerServerReceivers() {
        if (registered) {
            return;
        }
        registered = true;

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_SNAPSHOT, (server, player, handler, buf, responseSender) ->
                server.execute(() -> sendSnapshot(player))
        );

        ServerPlayNetworking.registerGlobalReceiver(SELECT_OBJECT, (server, player, handler, buf, responseSender) -> {
            UUID objectId = buf.readUuid();
            server.execute(() -> selectObject(player, objectId));
        });

        ServerPlayNetworking.registerGlobalReceiver(CLEAR_SELECTION, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    PrecisionSelectionManager.clear(player);
                    sendSelection(player);
                })
        );
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        List<PrecisionObject> objects = PrecisionObjectManager.get(player.getServerWorld()).getAll();
        int count = Math.min(objects.size(), MAX_SNAPSHOT_OBJECTS);

        PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
        out.writeIdentifier(player.getServerWorld().getRegistryKey().getValue());
        out.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            out.writeNbt(objects.get(index).toNbt());
        }
        ServerPlayNetworking.send(player, SYNC_SNAPSHOT, out);
    }

    public static void sendSelection(ServerPlayerEntity player) {
        PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
        Optional<PrecisionSelectionManager.Selection> selection = PrecisionSelectionManager.getSelection(player);
        out.writeBoolean(selection.isPresent());
        if (selection.isPresent()) {
            out.writeIdentifier(selection.get().world().getValue());
            out.writeUuid(selection.get().objectId());
        }
        ServerPlayNetworking.send(player, SYNC_SELECTION, out);
    }

    public static NbtCompound readObjectNbt(PacketByteBuf buf) {
        NbtCompound nbt = buf.readNbt();
        if (nbt == null) {
            throw new IllegalArgumentException("Precision object packet did not contain NBT");
        }
        return nbt;
    }

    private static void selectObject(ServerPlayerEntity player, UUID objectId) {
        Optional<PrecisionObject> object = PrecisionObjectManager.get(player.getServerWorld()).get(objectId);
        if (object.isEmpty() || object.get().squaredDistanceTo(player.getPos()) > MAX_SELECTION_DISTANCE * MAX_SELECTION_DISTANCE) {
            PrecisionSelectionManager.clear(player);
        } else {
            PrecisionSelectionManager.select(player, objectId);
        }
        sendSelection(player);
    }
}
