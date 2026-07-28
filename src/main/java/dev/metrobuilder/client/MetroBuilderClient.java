package dev.metrobuilder.client;

import dev.metrobuilder.network.MetroBuilderNetworking;
import dev.metrobuilder.network.PlaceDisplayPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class MetroBuilderClient implements ClientModInitializer {
    private static KeyBinding nextTool;
    private static KeyBinding previousTool;
    private static KeyBinding placeDisplay;

    @Override
    public void onInitializeClient() {
        nextTool = register("next_tool", GLFW.GLFW_KEY_RIGHT_BRACKET);
        previousTool = register("previous_tool", GLFW.GLFW_KEY_LEFT_BRACKET);
        placeDisplay = register("place_display", GLFW.GLFW_KEY_P);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (nextTool.wasPressed()) ClientState.setToolMode(ClientState.toolMode().next());
            while (previousTool.wasPressed()) ClientState.setToolMode(ClientState.toolMode().previous());
            while (placeDisplay.wasPressed()) sendPlacement(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;
            drawContext.drawTextWithShadow(client.textRenderer,
                    "MetroBuilder  |  " + ClientState.toolMode().displayName() + "  |  " + ClientState.blockId(),
                    8, 8, 0xFFFFFF);
        });
    }

    private static KeyBinding register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.metrobuilder." + name,
                InputUtil.Type.KEYSYM,
                key,
                "key.category.metrobuilder"
        ));
    }

    private static void sendPlacement(MinecraftClient client) {
        if (client.player == null || client.world == null || client.crosshairTarget == null ||
                client.crosshairTarget.getType() == HitResult.Type.MISS) return;

        Vec3d position = client.crosshairTarget.getPos().add(0, 0.01, 0);
        PlaceDisplayPayload payload = new PlaceDisplayPayload(position, ClientState.rotationDegrees(), ClientState.blockId());
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        payload.write(buf);
        ClientPlayNetworking.send(MetroBuilderNetworking.PLACE_DISPLAY, buf);
    }
}
