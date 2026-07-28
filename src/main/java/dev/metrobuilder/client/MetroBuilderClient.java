package dev.metrobuilder.client;

import dev.metrobuilder.item.MetroBuilderItems;
import dev.metrobuilder.network.MetroBuilderNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public final class MetroBuilderClient implements ClientModInitializer {
    private static KeyBinding rotateRight;
    private static KeyBinding rotateLeft;

    @Override
    public void onInitializeClient() {
        rotateRight = register("rotate_right", GLFW.GLFW_KEY_RIGHT_BRACKET);
        rotateLeft = register("rotate_left", GLFW.GLFW_KEY_LEFT_BRACKET);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (rotateRight.wasPressed()) sendRotation(client, 1);
            while (rotateLeft.wasPressed()) sendRotation(client, -1);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden ||
                    !client.player.getMainHandStack().isOf(MetroBuilderItems.BUILDER_WAND)) return;
            drawContext.drawTextWithShadow(client.textRenderer,
                    "MetroBuilder Builder Wand  |  [ / ] Rotate  |  Shift + Right-click Cancel",
                    8, 8, 0xFFFFFF);
        });
    }

    private static KeyBinding register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.metrobuilder." + name, InputUtil.Type.KEYSYM, key, "key.category.metrobuilder"));
    }

    private static void sendRotation(MinecraftClient client, int direction) {
        if (client.player == null || !client.player.getMainHandStack().isOf(MetroBuilderItems.BUILDER_WAND)) return;
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(direction);
        ClientPlayNetworking.send(MetroBuilderNetworking.ROTATE_WAND, buf);
    }
}
