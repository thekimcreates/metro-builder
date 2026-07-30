package dev.metrobuilder.display;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrecisionPSDManager {
    public static final String DOOR_ID = "tjmetro:psd_door_tianjin_bmt";
    public static final String GLASS_ID = "mtr:psd_glass";

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private PrecisionPSDManager() {}

    public static Session get(ServerPlayerEntity player) {
        return SESSIONS.computeIfAbsent(player.getUuid(), ignored -> new Session());
    }

    public static void startOrConfirm(ServerPlayerEntity player, Vec3d position) {
        Session session = get(player);
        if (session.preview != null && !session.preview.isRemoved()) {
            session.preview.removeCommandTag("metrobuilder.psd_preview");
            session.preview.addCommandTag("metrobuilder.precision_psd");
            session.preview.setGlowing(false);
            session.preview = null;
            player.sendMessage(Text.literal("Precision PSD placed"), true);
            return;
        }

        Identifier id = Identifier.tryParse(session.blockId);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            player.sendMessage(Text.literal("Required block is unavailable: " + session.blockId), false);
            return;
        }

        Block block = Registries.BLOCK.get(id);
        DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, player.getServerWorld());
        NbtCompound nbt = new NbtCompound();
        nbt.put("block_state", NbtHelper.fromBlockState(block.getDefaultState()));
        entity.readNbt(nbt);
        entity.setPosition(position.x, position.y, position.z);
        entity.setYaw(session.yaw);
        entity.setGlowing(true);
        entity.addCommandTag("metrobuilder.psd_preview");
        entity.addCommandTag("metrobuilder.precision_psd");
        if (player.getServerWorld().spawnEntity(entity)) {
            session.preview = entity;
            player.sendMessage(Text.literal("Preview: " + displayName(session.blockId) + " | right-click again to place"), true);
        }
    }

    public static void rotate(ServerPlayerEntity player, float delta) {
        Session session = get(player);
        session.yaw = normalize(session.yaw + delta);
        if (session.preview != null && !session.preview.isRemoved()) session.preview.setYaw(session.yaw);
        player.sendMessage(Text.literal(String.format("PSD rotation: %.2f°", session.yaw)), true);
    }

    public static void nudge(ServerPlayerEntity player, double localX, double y, double localZ) {
        Session session = get(player);
        if (session.preview == null || session.preview.isRemoved()) return;
        double radians = Math.toRadians(session.yaw);
        double dx = localX * Math.cos(radians) - localZ * Math.sin(radians);
        double dz = localX * Math.sin(radians) + localZ * Math.cos(radians);
        session.preview.setPosition(session.preview.getX() + dx, session.preview.getY() + y, session.preview.getZ() + dz);
    }

    public static void toggleType(ServerPlayerEntity player) {
        Session session = get(player);
        session.blockId = session.blockId.equals(DOOR_ID) ? GLASS_ID : DOOR_ID;
        player.sendMessage(Text.literal("PSD type: " + displayName(session.blockId)), true);
        if (session.preview != null && !session.preview.isRemoved()) {
            Vec3d pos = session.preview.getPos();
            session.preview.discard();
            session.preview = null;
            startOrConfirm(player, pos);
        }
    }

    public static void cancel(ServerPlayerEntity player) {
        Session session = get(player);
        if (session.preview != null && !session.preview.isRemoved()) session.preview.discard();
        session.preview = null;
        player.sendMessage(Text.literal("Precision PSD preview cancelled"), true);
    }

    public static void remove(ServerPlayerEntity player) {
        cancel(player);
        SESSIONS.remove(player.getUuid());
    }

    private static float normalize(float yaw) {
        yaw %= 360.0f;
        return yaw < 0 ? yaw + 360.0f : yaw;
    }

    private static String displayName(String id) {
        return id.equals(DOOR_ID) ? "Tianjin BMT PSD Door" : "MTR PSD Glass";
    }

    public static final class Session {
        private String blockId = DOOR_ID;
        private float yaw;
        private DisplayEntity.BlockDisplayEntity preview;
    }
}
