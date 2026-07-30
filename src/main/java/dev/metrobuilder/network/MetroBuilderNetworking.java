package dev.metrobuilder.network;

import dev.metrobuilder.MetroBuilder;
import dev.metrobuilder.display.DisplayManager;
import dev.metrobuilder.display.PrecisionPSDManager;
import dev.metrobuilder.item.BuilderWandItem;
import dev.metrobuilder.item.MetroBuilderItems;
import dev.metrobuilder.platform.PlatformDesignManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class MetroBuilderNetworking {
    public static final Identifier ROTATE_WAND = new Identifier(MetroBuilder.MOD_ID, "rotate_wand");
    public static final Identifier OPEN_PLATFORM_BUILDER = new Identifier(MetroBuilder.MOD_ID, "open_platform_builder");
    public static final Identifier SAVE_PLATFORM_DESIGN = new Identifier(MetroBuilder.MOD_ID, "save_platform_design");
    public static final Identifier PSD_CONTROL = new Identifier(MetroBuilder.MOD_ID, "psd_control");

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

        ServerPlayNetworking.registerGlobalReceiver(SAVE_PLATFORM_DESIGN, (server, player, handler, buf, responseSender) -> {
            int count = Math.min(buf.readVarInt(), PlatformDesignManager.MAX_ROWS);
            List<String> rows = new ArrayList<>();
            for (int i = 0; i < count; i++) rows.add(buf.readString(128));
            server.execute(() -> {
                PlatformDesignManager.get(player).setRows(rows);
                player.sendMessage(net.minecraft.text.Text.literal("Platform Builder design saved for this world session"), true);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PSD_CONTROL, (server, player, handler, buf, responseSender) -> {
            String action = buf.readString(32);
            boolean fine = buf.readBoolean();
            server.execute(() -> {
                if (!player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER)) return;
                double step = fine ? 0.001 : 0.01;
                switch (action) {
                    case "rotate_left" -> PrecisionPSDManager.rotate(player, fine ? -0.25f : -1.0f);
                    case "rotate_right" -> PrecisionPSDManager.rotate(player, fine ? 0.25f : 1.0f);
                    case "left" -> PrecisionPSDManager.nudge(player, -step, 0, 0);
                    case "right" -> PrecisionPSDManager.nudge(player, step, 0, 0);
                    case "forward" -> PrecisionPSDManager.nudge(player, 0, 0, -step);
                    case "back" -> PrecisionPSDManager.nudge(player, 0, 0, step);
                    case "up" -> PrecisionPSDManager.nudge(player, 0, step, 0);
                    case "down" -> PrecisionPSDManager.nudge(player, 0, -step, 0);
                    case "toggle" -> PrecisionPSDManager.toggleType(player);
                    case "cancel" -> PrecisionPSDManager.cancel(player);
                }
            });
        });
    }
}
