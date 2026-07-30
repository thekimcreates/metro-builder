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
    public static final Identifier PRECISION_PSD_ACTION = new Identifier(MetroBuilder.MOD_ID, "precision_psd_action");
    public static final Identifier OPEN_PSD_PROPERTIES = new Identifier(MetroBuilder.MOD_ID, "open_psd_properties");
    public static final Identifier SHOW_PSD_PROPERTIES = new Identifier(MetroBuilder.MOD_ID, "show_psd_properties");
    public static final Identifier APPLY_PSD_PROPERTIES = new Identifier(MetroBuilder.MOD_ID, "apply_psd_properties");

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

        ServerPlayNetworking.registerGlobalReceiver(PRECISION_PSD_ACTION, (server, player, handler, buf, responseSender) -> {
            String action = buf.readString(32);
            double amount = buf.readDouble();
            server.execute(() -> {
                if (!player.getMainHandStack().isOf(MetroBuilderItems.PRECISION_PSD_BUILDER)) return;
                switch (action) {
                    case "rotate" -> PrecisionPSDManager.rotate(player, (float) amount);
                    case "x" -> PrecisionPSDManager.nudge(player, amount, 0, 0);
                    case "z" -> PrecisionPSDManager.nudge(player, 0, 0, amount);
                    case "y" -> PrecisionPSDManager.nudge(player, 0, amount, 0);
                    case "type" -> PrecisionPSDManager.toggleType(player);
                    case "cancel" -> PrecisionPSDManager.cancel(player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(OPEN_PSD_PROPERTIES, (server, player, handler, buf, responseSender) -> server.execute(() -> {
            PrecisionPSDManager.PropertiesSnapshot snapshot = PrecisionPSDManager.getProperties(player);
            if (snapshot == null) {
                player.sendMessage(net.minecraft.text.Text.literal("Select or preview a PSD first"), true);
                return;
            }
            net.minecraft.network.PacketByteBuf out = new net.minecraft.network.PacketByteBuf(io.netty.buffer.Unpooled.buffer());
            out.writeDouble(snapshot.x()); out.writeDouble(snapshot.y()); out.writeDouble(snapshot.z());
            out.writeFloat(snapshot.yaw()); out.writeString(snapshot.blockId());
            ServerPlayNetworking.send(player, SHOW_PSD_PROPERTIES, out);
        }));

        ServerPlayNetworking.registerGlobalReceiver(APPLY_PSD_PROPERTIES, (server, player, handler, buf, responseSender) -> {
            double x=buf.readDouble(), y=buf.readDouble(), z=buf.readDouble(); float yaw=buf.readFloat(); String blockId=buf.readString(128);
            server.execute(() -> PrecisionPSDManager.applyProperties(player,x,y,z,yaw,blockId));
        });
    }
}
