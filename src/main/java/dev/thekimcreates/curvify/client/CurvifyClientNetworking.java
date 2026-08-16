package dev.thekimcreates.curvify.client;

import dev.thekimcreates.curvify.network.CurvifyNetworking;
import dev.thekimcreates.curvify.placement.PlacementConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;

public final class CurvifyClientNetworking {
    private CurvifyClientNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(CurvifyNetworking.CONFIG_SYNC, (client, handler, buffer, sender) -> {
            final String railId = buffer.readString(128);
            final PlacementConfig config = PlacementConfig.read(buffer);
            client.execute(() -> {
                if (client.currentScreen instanceof PsdPropertiesScreen screen) {
                    screen.acceptServerConfig(railId, config);
                }
            });
        });
    }

    public static void requestConfig(String railId) {
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString(railId);
        ClientPlayNetworking.send(CurvifyNetworking.REQUEST_CONFIG, buffer);
    }

    public static void save(String railId, PlacementConfig config) {
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString(railId);
        config.write(buffer);
        ClientPlayNetworking.send(CurvifyNetworking.SAVE, buffer);
    }

    public static void delete(String railId) {
        final PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString(railId);
        ClientPlayNetworking.send(CurvifyNetworking.DELETE, buffer);
    }

    public static void returnTo(net.minecraft.client.gui.screen.Screen previous) {
        MinecraftClient.getInstance().setScreen(previous);
    }
}
