package dev.metrobuilder.display;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        if (session.hasPreview()) {
            for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
                if (!entity.isRemoved()) {
                    entity.removeCommandTag("metrobuilder.psd_preview");
                    entity.addCommandTag("metrobuilder.precision_psd");
                    entity.setGlowing(false);
                }
            }
            session.previewParts.clear();
            player.sendMessage(Text.literal("Precision PSD placed"), true);
            return;
        }

        Identifier id = Identifier.tryParse(session.blockId);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            player.sendMessage(Text.literal("Required block is unavailable: " + session.blockId), false);
            return;
        }

        Block block = Registries.BLOCK.get(id);
        if (session.blockId.equals(GLASS_ID)) {
            // MTR PSD glass is a multi-block model. A single default block state only
            // renders one small connected-model fragment. Render the complete standalone
            // lower and upper states as one movable preview/placement.
            BlockState lower = withStringProperty(withStringProperty(block.getDefaultState(), "side", "single"), "half", "lower");
            BlockState upper = withStringProperty(withStringProperty(block.getDefaultState(), "side", "single"), "half", "upper");
            spawnPart(player, session, lower, position);
            spawnPart(player, session, upper, position.add(0, 1, 0));
        } else {
            spawnPart(player, session, block.getDefaultState(), position);
        }

        if (session.hasPreview()) {
            player.sendMessage(Text.literal("Preview: " + displayName(session.blockId) + " | right-click again to place"), true);
        }
    }

    private static void spawnPart(ServerPlayerEntity player, Session session, BlockState state, Vec3d position) {
        DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, player.getServerWorld());
        NbtCompound nbt = new NbtCompound();
        nbt.put("block_state", NbtHelper.fromBlockState(state));
        entity.readNbt(nbt);
        entity.setPosition(position.x, position.y, position.z);
        entity.setYaw(session.yaw);
        entity.setGlowing(true);
        entity.addCommandTag("metrobuilder.psd_preview");
        entity.addCommandTag("metrobuilder.precision_psd");
        if (player.getServerWorld().spawnEntity(entity)) {
            session.previewParts.add(entity);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState withStringProperty(BlockState state, String propertyName, String valueName) {
        for (Property property : state.getProperties()) {
            if (!property.getName().equals(propertyName)) continue;
            Optional value = property.parse(valueName);
            if (value.isPresent()) return state.with(property, (Comparable) value.get());
        }
        return state;
    }

    public static void rotate(ServerPlayerEntity player, float delta) {
        Session session = get(player);
        session.yaw = normalize(session.yaw + delta);
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (!entity.isRemoved()) entity.setYaw(session.yaw);
        }
        player.sendMessage(Text.literal(String.format("PSD rotation: %.2f°", session.yaw)), true);
    }

    public static void nudge(ServerPlayerEntity player, double localX, double y, double localZ) {
        Session session = get(player);
        if (!session.hasPreview()) return;
        double radians = Math.toRadians(session.yaw);
        double dx = localX * Math.cos(radians) - localZ * Math.sin(radians);
        double dz = localX * Math.sin(radians) + localZ * Math.cos(radians);
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (!entity.isRemoved()) entity.setPosition(entity.getX() + dx, entity.getY() + y, entity.getZ() + dz);
        }
    }

    public static void toggleType(ServerPlayerEntity player) {
        Session session = get(player);
        Vec3d pos = session.basePosition();
        discardPreview(session);
        session.blockId = session.blockId.equals(DOOR_ID) ? GLASS_ID : DOOR_ID;
        player.sendMessage(Text.literal("PSD type: " + displayName(session.blockId)), true);
        if (pos != null) startOrConfirm(player, pos);
    }

    public static void cancel(ServerPlayerEntity player) {
        Session session = get(player);
        discardPreview(session);
        player.sendMessage(Text.literal("Precision PSD preview cancelled"), true);
    }

    private static void discardPreview(Session session) {
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (!entity.isRemoved()) entity.discard();
        }
        session.previewParts.clear();
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
        private final List<DisplayEntity.BlockDisplayEntity> previewParts = new ArrayList<>();

        private boolean hasPreview() {
            previewParts.removeIf(entity -> entity == null || entity.isRemoved());
            return !previewParts.isEmpty();
        }

        private Vec3d basePosition() {
            return hasPreview() ? previewParts.get(0).getPos() : null;
        }
    }
}
