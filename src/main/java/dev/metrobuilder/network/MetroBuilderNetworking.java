package dev.metrobuilder.network;

import dev.metrobuilder.MetroBuilder;
import dev.metrobuilder.display.DisplayManager;
import dev.metrobuilder.item.BuilderWandItem;
import dev.metrobuilder.item.MetroBuilderItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public final class MetroBuilderNetworking {
    public static final Identifier ROTATE_WAND = new Identifier(MetroBuilder.MOD_ID, "rotate_wand");

    private MetroBuilderNetworking() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ROTATE_WAND, (server, player, handler, buf, responseSender) -> {
            int direction = buf.readInt();
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!stack.isOf(MetroBuilderItems.BUILDER_WAND)) return;

                float current = stack.getOrCreateNbt().getFloat(BuilderWandItem.ROTATION_KEY);
                float next = (current + direction * 45.0f) % 360.0f;
                if (next < 0) next += 360.0f;
                stack.getOrCreateNbt().putFloat(BuilderWandItem.ROTATION_KEY, next);
                DisplayManager.rotatePreview(player, next);
                player.sendMessage(net.minecraft.text.Text.literal("Rotation: " + (int) next + "°"), true);
            });
        });
    }
}
