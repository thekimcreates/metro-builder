package dev.thekimcreates.curvify.network;

import dev.thekimcreates.curvify.Curvify;
import dev.thekimcreates.curvify.placement.CurvifyPlacementService;
import dev.thekimcreates.curvify.placement.PlacementConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class CurvifyNetworking {
    public static final Identifier REQUEST_CONFIG = id("request_config");
    public static final Identifier CONFIG_SYNC = id("config_sync");
    public static final Identifier SAVE = id("save");
    public static final Identifier DELETE = id("delete");
    private static final int MAX_RAIL_ID_LENGTH = 128;

    private CurvifyNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_CONFIG, (server, player, handler, buffer, sender) -> {
            final String railId = buffer.readString(MAX_RAIL_ID_LENGTH);
            server.execute(() -> sendConfig(player, railId));
        });

        ServerPlayNetworking.registerGlobalReceiver(SAVE, (server, player, handler, buffer, sender) -> {
            final String railId = buffer.readString(MAX_RAIL_ID_LENGTH);
            final PlacementConfig config;
            try {
                config = PlacementConfig.read(buffer);
            } catch (RuntimeException exception) {
                Curvify.LOGGER.warn("Rejected invalid PSD configuration from {}", player.getName().getString());
                return;
            }
            server.execute(() -> CurvifyPlacementService.save(player, railId, config));
        });

        ServerPlayNetworking.registerGlobalReceiver(DELETE, (server, player, handler, buffer, sender) -> {
            final String railId = buffer.readString(MAX_RAIL_ID_LENGTH);
            server.execute(() -> CurvifyPlacementService.delete(player, railId));
        });
    }

    private static void sendConfig(ServerPlayerEntity player, String railId) {
        final PlacementConfig config = CurvifyPlacementService.getConfig(player.getServerWorld(), railId);
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString(railId);
        config.write(buffer);
        ServerPlayNetworking.send(player, CONFIG_SYNC, buffer);
    }

    private static Identifier id(String path) {
        return new Identifier(Curvify.MOD_ID, path);
    }
}
