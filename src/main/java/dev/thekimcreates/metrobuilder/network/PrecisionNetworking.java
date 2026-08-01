package dev.thekimcreates.metrobuilder.network;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.item.MetroBuilderItems;
import dev.thekimcreates.metrobuilder.precision.PrecisionSaveData;
import dev.thekimcreates.metrobuilder.precision.PrecisionSelectionManager;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDManager;
import dev.thekimcreates.metrobuilder.psd.PSDObject;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;

/** Network channels shared by the precision engine and unified Builder Wand. */
public final class PrecisionNetworking {
    public static final Identifier SNAPSHOT = MetroBuilder.id("precision_snapshot");
    public static final Identifier SELECTION_REQUEST = MetroBuilder.id("precision_selection_request");
    public static final Identifier SELECTION_STATE = MetroBuilder.id("precision_selection_state");
    public static final Identifier PSD_PLACE = MetroBuilder.id("psd_place");
    public static final Identifier PSD_UPDATE_TRANSFORM = MetroBuilder.id("psd_update_transform");
    public static final Identifier PSD_DELETE = MetroBuilder.id("psd_delete");

    private static final double MAX_EDIT_DISTANCE_SQUARED = 32.0D * 32.0D;
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
                        if (!canUseWand(player)) {
                            PrecisionSelectionManager.clear(player);
                        } else if (requestedObjectId == null) {
                            PrecisionSelectionManager.clear(player);
                        } else {
                            PSDManager.find(player.getServerWorld(), requestedObjectId)
                                    .filter(psd -> isWithinEditDistance(player, psd.transform()))
                                    .ifPresentOrElse(
                                            psd -> PrecisionSelectionManager.select(player, psd.id()),
                                            () -> PrecisionSelectionManager.clear(player)
                                    );
                        }
                        sendSelectionState(player);
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PSD_PLACE,
                (server, player, handler, buffer, responseSender) -> {
                    final Identifier packId = buffer.readIdentifier();
                    final PrecisionTransform transform = readTransform(buffer);
                    server.execute(() -> {
                        if (!canUseWand(player)
                                || !isWithinEditDistance(player, transform)) {
                            return;
                        }

                        final ServerWorld world = player.getServerWorld();
                        final PSDObject psd = PSDManager.create(world, transform, packId);
                        PrecisionSelectionManager.select(player, psd.id());
                        broadcastSnapshot(world);
                        sendSelectionState(player);
                        player.sendMessage(Text.literal("PSD placed"), true);
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PSD_UPDATE_TRANSFORM,
                (server, player, handler, buffer, responseSender) -> {
                    final UUID objectId = buffer.readUuid();
                    final PrecisionTransform transform = readTransform(buffer);
                    server.execute(() -> {
                        if (!canUseWand(player)
                                || !isWithinEditDistance(player, transform)) {
                            return;
                        }

                        final Optional<PrecisionSelectionManager.Selection> selection =
                                PrecisionSelectionManager.current(player);
                        if (selection.isEmpty()
                                || !selection.get().objectId().equals(objectId)) {
                            return;
                        }

                        final ServerWorld world = player.getServerWorld();
                        if (PSDManager.updateTransform(world, objectId, transform)) {
                            broadcastSnapshot(world);
                        }
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PSD_DELETE,
                (server, player, handler, buffer, responseSender) -> {
                    final UUID objectId = buffer.readUuid();
                    server.execute(() -> {
                        if (!canUseWand(player)) {
                            return;
                        }

                        final Optional<PrecisionSelectionManager.Selection> selection =
                                PrecisionSelectionManager.current(player);
                        if (selection.isEmpty()
                                || !selection.get().objectId().equals(objectId)) {
                            return;
                        }

                        final ServerWorld world = player.getServerWorld();
                        final Optional<PSDObject> psd = PSDManager.find(world, objectId);
                        if (psd.isEmpty()
                                || !isWithinEditDistance(player, psd.get().transform())) {
                            return;
                        }

                        if (PSDManager.remove(world, objectId)) {
                            PrecisionSelectionManager.clear(player);
                            broadcastSnapshot(world);
                            sendSelectionState(player);
                            player.sendMessage(Text.literal("PSD deleted"), true);
                        }
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

    public static void writeTransform(PacketByteBuf buffer, PrecisionTransform transform) {
        buffer.writeDouble(transform.x());
        buffer.writeDouble(transform.y());
        buffer.writeDouble(transform.z());
        buffer.writeFloat(transform.pitch());
        buffer.writeFloat(transform.yaw());
        buffer.writeFloat(transform.roll());
        buffer.writeFloat(transform.scaleX());
        buffer.writeFloat(transform.scaleY());
        buffer.writeFloat(transform.scaleZ());
    }

    public static PrecisionTransform readTransform(PacketByteBuf buffer) {
        return new PrecisionTransform(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    private static boolean canUseWand(ServerPlayerEntity player) {
        return player.isCreative()
                && (player.getMainHandStack().isOf(MetroBuilderItems.BUILDER_WAND)
                || player.getOffHandStack().isOf(MetroBuilderItems.BUILDER_WAND));
    }

    private static boolean isWithinEditDistance(
            ServerPlayerEntity player,
            PrecisionTransform transform
    ) {
        return transform.squaredDistanceTo(player.getEyePos()) <= MAX_EDIT_DISTANCE_SQUARED;
    }
}
