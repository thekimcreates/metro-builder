package dev.metrobuilder.client;

import dev.metrobuilder.client.screen.PlatformBuilderScreen;
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

import java.util.ArrayList;
import java.util.List;

public final class MetroBuilderClient implements ClientModInitializer {
    private static KeyBinding rotateRight;
    private static KeyBinding rotateLeft;
    private static KeyBinding psdToggle, psdCancel, psdForward, psdBack, psdLeft, psdRight, psdUp, psdDown;

    @Override
    public void onInitializeClient() {
        rotateRight = register("rotate_right", GLFW.GLFW_KEY_RIGHT_BRACKET);
        rotateLeft = register("rotate_left", GLFW.GLFW_KEY_LEFT_BRACKET);
        psdToggle = register("psd_toggle", GLFW.GLFW_KEY_G);
        psdCancel = register("psd_cancel", GLFW.GLFW_KEY_X);
        psdForward = register("psd_forward", GLFW.GLFW_KEY_UP);
        psdBack = register("psd_back", GLFW.GLFW_KEY_DOWN);
        psdLeft = register("psd_left", GLFW.GLFW_KEY_LEFT);
        psdRight = register("psd_right", GLFW.GLFW_KEY_RIGHT);
        psdUp = register("psd_up", GLFW.GLFW_KEY_PAGE_UP);
        psdDown = register("psd_down", GLFW.GLFW_KEY_PAGE_DOWN);

        ClientPlayNetworking.registerGlobalReceiver(MetroBuilderNetworking.OPEN_PLATFORM_BUILDER,
                (client, handler, buf, responseSender) -> {
                    int count = buf.readVarInt();
                    List<String> rows = new ArrayList<>();
                    for (int i = 0; i < count; i++) rows.add(buf.readString(128));
                    client.execute(() -> client.setScreen(new PlatformBuilderScreen(rows)));
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (rotateRight.wasPressed()) {
                if (client.player != null && client.player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER)) sendPsd(client, "rotate_right");
                else sendRotation(client, 1);
            }
            while (rotateLeft.wasPressed()) {
                if (client.player != null && client.player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER)) sendPsd(client, "rotate_left");
                else sendRotation(client, -1);
            }
            while (psdToggle.wasPressed()) sendPsd(client, "toggle");
            while (psdCancel.wasPressed()) sendPsd(client, "cancel");
            while (psdForward.wasPressed()) sendPsd(client, "forward");
            while (psdBack.wasPressed()) sendPsd(client, "back");
            while (psdLeft.wasPressed()) sendPsd(client, "left");
            while (psdRight.wasPressed()) sendPsd(client, "right");
            while (psdUp.wasPressed()) sendPsd(client, "up");
            while (psdDown.wasPressed()) sendPsd(client, "down");
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;
            if (client.player.getMainHandStack().isOf(MetroBuilderItems.BUILDER_WAND)) {
                drawContext.drawTextWithShadow(client.textRenderer,
                        "MetroBuilder Builder Wand  |  [ / ] Rotate  |  Shift + Right-click Cancel",
                        8, 8, 0xFFFFFF);
            } else if (client.player.getMainHandStack().isOf(MetroBuilderItems.PLATFORM_BUILDER)) {
                drawContext.drawTextWithShadow(client.textRenderer,
                        "Platform Builder  |  Right-click Air: Menu  |  Left-click Block: Pos 1  |  Right-click Block: Pos 2",
                        8, 8, 0xFFFFFF);
            } else if (client.player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER)) {
                drawContext.drawTextWithShadow(client.textRenderer,
                        "Precision PSD Builder | Right-click: Preview/Place | [ ] Rotate | Arrows/PageUp/PageDown Nudge | G Toggle | X Cancel | Shift Fine",
                        8, 8, 0xFFFFFF);
            }
        });
    }

    private static KeyBinding register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.metrobuilder." + name, InputUtil.Type.KEYSYM, key, "key.category.metrobuilder"));
    }

    private static void sendPsd(MinecraftClient client, String action) {
        if (client.player == null || !client.player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER)) return;
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeString(action);
        buf.writeBoolean(client.options.sneakKey.isPressed());
        ClientPlayNetworking.send(MetroBuilderNetworking.PSD_CONTROL, buf);
    }

    private static void sendRotation(MinecraftClient client, int direction) {
        if (client.player == null || !client.player.getMainHandStack().isOf(MetroBuilderItems.BUILDER_WAND)) return;
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(direction);
        ClientPlayNetworking.send(MetroBuilderNetworking.ROTATE_WAND, buf);
    }
}
