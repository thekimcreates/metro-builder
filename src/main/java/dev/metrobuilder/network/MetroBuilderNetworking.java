package dev.metrobuilder.network;

import dev.metrobuilder.MetroBuilder;
import dev.metrobuilder.display.DisplayManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class MetroBuilderNetworking {
    public static final Identifier PLACE_DISPLAY = new Identifier(MetroBuilder.MOD_ID, "place_display");

    private MetroBuilderNetworking() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PLACE_DISPLAY, (server, player, handler, buf, responseSender) -> {
            PlaceDisplayPayload payload = PlaceDisplayPayload.read(buf);
            server.execute(() -> DisplayManager.place(player, payload));
        });
    }
}
