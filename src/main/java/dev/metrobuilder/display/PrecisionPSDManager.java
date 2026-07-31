package dev.metrobuilder.display;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
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
    public static final String TOP_ID = "mtr:psd_top";

    private static final String PLACED_TAG = "metrobuilder.precision_psd";
    private static final String PREVIEW_TAG = "metrobuilder.psd_preview";
    private static final String GROUP_PREFIX = "metrobuilder.psd_group.";
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private PrecisionPSDManager() {}

    public static Session get(ServerPlayerEntity player) {
        return SESSIONS.computeIfAbsent(player.getUuid(), ignored -> new Session());
    }

    public static boolean selectLookedAt(ServerPlayerEntity player) {
        Session session = get(player);
        if (session.hasPreview()) return false;

        Entity hit = findLookedAtPlacedPart(player, 6.0);
        if (hit == null) return false;

        String groupTag = findGroupTag(hit);
        if (groupTag == null) return false;

        ServerWorld world = player.getServerWorld();
        Box search = hit.getBoundingBox().expand(8.0);
        List<Entity> group = world.getOtherEntities(null, search, entity -> entity.getCommandTags().contains(groupTag));

        for (Entity entity : group) {
            if (entity instanceof DisplayEntity.BlockDisplayEntity display) {
                display.setGlowing(true);
                display.addCommandTag(PREVIEW_TAG);
                session.previewParts.add(display);
            }
        }

        if (!session.previewParts.isEmpty()) {
            session.groupTag = groupTag;
            session.yaw = session.previewParts.get(0).getYaw();
            player.sendMessage(Text.literal("Precision PSD selected | adjust it, then right-click to save"), true);
            return true;
        }
        return false;
    }

    public static void startOrConfirm(ServerPlayerEntity player, Vec3d position) {
        Session session = get(player);
        if (session.hasPreview()) {
            confirm(player, session);
            return;
        }

        Identifier id = Identifier.tryParse(session.blockId);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            player.sendMessage(Text.literal("Required block is unavailable: " + session.blockId), false);
            return;
        }

        session.groupTag = GROUP_PREFIX + UUID.randomUUID();
        Block block = Registries.BLOCK.get(id);

        if (session.blockId.equals(GLASS_ID)) {
            BlockState lower = withStringProperty(withStringProperty(block.getDefaultState(), "side", "single"), "half", "lower");
            BlockState upper = withStringProperty(withStringProperty(block.getDefaultState(), "side", "single"), "half", "upper");

            Identifier topId = Identifier.tryParse(TOP_ID);
            BlockState top = null;
            if (topId != null && Registries.BLOCK.containsId(topId)) {
                top = withStringProperty(
                        withStringProperty(Registries.BLOCK.get(topId).getDefaultState(), "air_left", "false"),
                        "air_right", "false"
                );
            } else {
                player.sendMessage(Text.literal("Warning: required block is unavailable: " + TOP_ID), false);
            }

            // One glass placement is a connected three-panel assembly. Every part shares
            // the same group tag and is transformed as one object when moved or rotated.
            for (int panel = 0; panel < 3; panel++) {
                Vec3d panelBase = localOffset(position, session.yaw, panel, 0);
                spawnPart(player, session, lower, panelBase);
                spawnPart(player, session, upper, panelBase.add(0, 1, 0));
                if (top != null) spawnPart(player, session, top, panelBase.add(0, 2, 0));
            }
        } else {
            spawnPart(player, session, block.getDefaultState(), position);
        }

        if (session.hasPreview()) {
            player.sendMessage(Text.literal("Preview: " + displayName(session.blockId) + " | right-click again to place"), true);
        }
    }

    private static void confirm(ServerPlayerEntity player, Session session) {
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (!entity.isRemoved()) {
                entity.removeCommandTag(PREVIEW_TAG);
                entity.addCommandTag(PLACED_TAG);
                entity.setGlowing(false);
            }
        }
        session.previewParts.clear();
        session.groupTag = null;
        player.sendMessage(Text.literal("Precision PSD placed"), true);
    }

    private static void spawnPart(ServerPlayerEntity player, Session session, BlockState state, Vec3d position) {
        DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, player.getServerWorld());
        NbtCompound nbt = new NbtCompound();
        nbt.put("block_state", NbtHelper.fromBlockState(state));
        nbt.putFloat("width", 1.0f);
        nbt.putFloat("height", 1.0f);
        entity.readNbt(nbt);
        entity.setPosition(position.x, position.y, position.z);
        entity.setYaw(session.yaw);
        entity.setGlowing(true);
        entity.addCommandTag(PREVIEW_TAG);
        entity.addCommandTag(PLACED_TAG);
        entity.addCommandTag(session.groupTag);
        if (player.getServerWorld().spawnEntity(entity)) session.previewParts.add(entity);
    }

    private static Vec3d localOffset(Vec3d origin, float yaw, double localX, double localZ) {
        double radians = Math.toRadians(yaw);
        double dx = localX * Math.cos(radians) - localZ * Math.sin(radians);
        double dz = localX * Math.sin(radians) + localZ * Math.cos(radians);
        return origin.add(dx, 0, dz);
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
        if (!session.hasPreview()) return;

        DisplayEntity.BlockDisplayEntity root = session.previewParts.get(0);
        double pivotX = root.getX();
        double pivotZ = root.getZ();
        double radians = Math.toRadians(delta);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        session.yaw = normalize(session.yaw + delta);
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (entity.isRemoved()) continue;
            double relativeX = entity.getX() - pivotX;
            double relativeZ = entity.getZ() - pivotZ;
            entity.setPosition(
                    pivotX + relativeX * cos - relativeZ * sin,
                    entity.getY(),
                    pivotZ + relativeX * sin + relativeZ * cos
            );
            entity.setYaw(session.yaw);
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
        if (session.hasPreview()) {
            player.sendMessage(Text.literal("Finish or cancel the selected PSD before switching type"), true);
            return;
        }
        session.blockId = session.blockId.equals(DOOR_ID) ? GLASS_ID : DOOR_ID;
        player.sendMessage(Text.literal("PSD type: " + displayName(session.blockId)), true);
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
        session.groupTag = null;
    }

    private static Entity findLookedAtPlacedPart(ServerPlayerEntity player, double distance) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(distance));
        Box search = player.getBoundingBox().stretch(player.getRotationVec(1.0f).multiply(distance)).expand(1.0);
        Entity closest = null;
        double closestSq = distance * distance;

        for (Entity entity : player.getServerWorld().getOtherEntities(player, search,
                e -> e.getCommandTags().contains(PLACED_TAG))) {
            Optional<Vec3d> hit = entity.getBoundingBox().expand(0.2).raycast(start, end);
            if (hit.isEmpty()) continue;
            double sq = start.squaredDistanceTo(hit.get());
            if (sq < closestSq) {
                closestSq = sq;
                closest = entity;
            }
        }
        return closest;
    }

    private static String findGroupTag(Entity entity) {
        for (String tag : entity.getCommandTags()) if (tag.startsWith(GROUP_PREFIX)) return tag;
        return null;
    }

    public static PropertiesSnapshot getProperties(ServerPlayerEntity player) {
        Session session = get(player);
        if (!session.hasPreview()) return null;
        DisplayEntity.BlockDisplayEntity root = session.previewParts.get(0);
        return new PropertiesSnapshot(root.getX(), root.getY(), root.getZ(), session.yaw, session.blockId);
    }

    public static void applyProperties(ServerPlayerEntity player, double x, double y, double z, float yaw, String blockId) {
        Session session = get(player);
        if (!session.hasPreview()) return;
        DisplayEntity.BlockDisplayEntity root = session.previewParts.get(0);
        double dx = x - root.getX();
        double dy = y - root.getY();
        double dz = z - root.getZ();
        session.yaw = normalize(yaw);
        if (DOOR_ID.equals(blockId) || GLASS_ID.equals(blockId)) session.blockId = blockId;
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (!entity.isRemoved()) {
                entity.setPosition(entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz);
                entity.setYaw(session.yaw);
            }
        }
        player.sendMessage(Text.literal("Precision PSD properties applied"), true);
    }

    public record PropertiesSnapshot(double x, double y, double z, float yaw, String blockId) {}

    public static void remove(ServerPlayerEntity player) {
        cancel(player);
        SESSIONS.remove(player.getUuid());
    }

    private static float normalize(float yaw) {
        yaw %= 360.0f;
        return yaw < 0 ? yaw + 360.0f : yaw;
    }

    private static String displayName(String id) {
        return id.equals(DOOR_ID) ? "Tianjin BMT PSD Door" : "MTR PSD Glass + Top";
    }

    public static final class Session {
        private String blockId = DOOR_ID;
        private float yaw;
        private String groupTag;
        private final List<DisplayEntity.BlockDisplayEntity> previewParts = new ArrayList<>();

        private boolean hasPreview() {
            previewParts.removeIf(entity -> entity == null || entity.isRemoved());
            return !previewParts.isEmpty();
        }
    }
}
