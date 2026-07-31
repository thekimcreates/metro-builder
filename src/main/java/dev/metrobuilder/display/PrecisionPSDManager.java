package dev.metrobuilder.display;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrecisionPSDManager {
    public static final String DOOR_ID = "tjmetro:psd_door_tianjin_bmt";
    public static final String FALLBACK_DOOR_ID = "mtr:psd_door";
    public static final String DOOR_TOP_ID = "tjmetro:psd_top_tianjin_bmt";
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

        List<Entity> group = player.getServerWorld().getOtherEntities(null, hit.getBoundingBox().expand(8.0),
                entity -> entity.getCommandTags().contains(groupTag));
        for (Entity entity : group) {
            if (entity instanceof DisplayEntity.BlockDisplayEntity display) {
                display.setGlowing(true);
                display.addCommandTag(PREVIEW_TAG);
                session.previewParts.add(display);
            }
        }
        if (session.previewParts.isEmpty()) return false;
        session.groupTag = groupTag;
        session.yaw = session.previewParts.get(0).getYaw();
        session.blockId = session.previewParts.stream().anyMatch(e -> e.getCommandTags().contains(DOOR_LEFT_TAG) || e.getCommandTags().contains(DOOR_RIGHT_TAG)) ? DOOR_ID : GLASS_ID;
        session.origin = session.previewParts.get(0).getPos();
        player.sendMessage(Text.literal("Precision PSD selected | edit, then right-click to save"), true);
        return true;
    }

    public static void startOrConfirm(ServerPlayerEntity player, Vec3d position) {
        Session session = get(player);
        if (session.hasPreview()) {
            confirm(player, session);
            return;
        }

        session.groupTag = GROUP_PREFIX + UUID.randomUUID();
        if (GLASS_ID.equals(session.blockId)) {
            createGlassPreview(player, session, position);
        } else {
            createDoorPreview(player, session, position);
        }
    }

    private static void createGlassPreview(ServerPlayerEntity player, Session session, Vec3d position) {
        Block glass = getBlock(GLASS_ID);
        if (glass == null) {
            player.sendMessage(Text.literal("Required block is unavailable: " + GLASS_ID), false);
            return;
        }
        BlockState lower = withStringProperty(withStringProperty(glass.getDefaultState(), "side", "single"), "half", "lower");
        BlockState upper = withStringProperty(withStringProperty(glass.getDefaultState(), "side", "single"), "half", "upper");
        Block topBlock = getBlock(TOP_ID);
        BlockState top = topBlock == null ? null : withStringProperty(withStringProperty(topBlock.getDefaultState(), "air_left", "false"), "air_right", "false");

        for (int panel = 0; panel < 3; panel++) {
            Vec3d base = localOffset(position, session.yaw, panel, 0);
            spawnPart(player, session, lower, base);
            spawnPart(player, session, upper, base.add(0, 1, 0));
            if (top != null) spawnPart(player, session, top, base.add(0, 2, 0));
        }
        player.sendMessage(Text.literal("Preview: 3 connected MTR glass panels | right-click to place"), true);
    }

    private static void createDoorPreview(ServerPlayerEntity player, Session session, Vec3d position) {
        String resolved = resolveDoorId();
        Block door = getBlock(resolved);
        if (door == null) {
            player.sendMessage(Text.literal("Neither Tianjin nor MTR PSD door is available"), false);
            return;
        }
        session.resolvedDoorId = resolved;
        session.origin = position;
        BlockState lower = withStringProperty(door.getDefaultState(), "half", "lower");
        BlockState upper = withStringProperty(door.getDefaultState(), "half", "upper");
        spawnPart(player, session, lower, position);
        spawnPart(player, session, upper, position.add(0, 1, 0));

        Block topBlock = getBlock(DOOR_TOP_ID);
        if (topBlock != null) {
            spawnPart(player, session, topBlock.getDefaultState(), position.add(0, 2, 0));
        }
        player.sendMessage(Text.literal("Functional PSD preview | rotation will snap to 90° when placed"), true);
    }

    private static void confirm(ServerPlayerEntity player, Session session) {
        if (!GLASS_ID.equals(session.blockId)) {
            placePrecisionDoor(player, session);
            return;
        }
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (!entity.isRemoved()) {
                entity.removeCommandTag(PREVIEW_TAG);
                entity.addCommandTag(PLACED_TAG);
                entity.setGlowing(false);
            }
        }
        session.previewParts.clear();
        session.groupTag = null;
        player.sendMessage(Text.literal("Precision glass assembly placed"), true);
    }

    private static final String DOOR_ROOT_TAG = "metrobuilder.psd_door_root";
    private static final String DOOR_LEFT_TAG = "metrobuilder.psd_door_left";
    private static final String DOOR_RIGHT_TAG = "metrobuilder.psd_door_right";

    private static void placePrecisionDoor(ServerPlayerEntity player, Session session) {
        if (session.previewParts.isEmpty()) return;
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            if (!entity.isRemoved()) entity.discard();
        }
        session.previewParts.clear();

        String resolved = session.resolvedDoorId == null ? resolveDoorId() : session.resolvedDoorId;
        Block door = getBlock(resolved);
        if (door == null) return;
        Vec3d origin = session.origin;
        if (origin == null) return;

        BlockState lower = withStringProperty(door.getDefaultState(), "half", "lower");
        BlockState upper = withStringProperty(door.getDefaultState(), "half", "upper");
        spawnPlacedDoorPart(player, session, lower, origin, DOOR_LEFT_TAG, -0.25, 0);
        spawnPlacedDoorPart(player, session, upper, origin.add(0, 1, 0), DOOR_LEFT_TAG, -0.25, 0);
        spawnPlacedDoorPart(player, session, lower, origin, DOOR_RIGHT_TAG, 0.25, 0);
        spawnPlacedDoorPart(player, session, upper, origin.add(0, 1, 0), DOOR_RIGHT_TAG, 0.25, 0);

        Block topBlock = getBlock(DOOR_TOP_ID);
        if (topBlock != null) {
            DisplayEntity.BlockDisplayEntity root = spawnPlacedDoorPart(player, session, topBlock.getDefaultState(), origin.add(0, 2, 0), DOOR_ROOT_TAG, 0, 0);
            if (root != null) root.addCommandTag(DOOR_ROOT_TAG);
        } else {
            DisplayEntity.BlockDisplayEntity root = spawnPlacedDoorPart(player, session, Blocks.BARRIER.getDefaultState(), origin.add(0, 2, 0), DOOR_ROOT_TAG, 0, 0);
            if (root != null) root.setInvisible(true);
        }
        session.groupTag = null;
        player.sendMessage(Text.literal("Arbitrary-angle MetroBuilder PSD placed; MTR door synchronization active"), true);
    }

    private static DisplayEntity.BlockDisplayEntity spawnPlacedDoorPart(ServerPlayerEntity player, Session session, BlockState state, Vec3d base, String roleTag, double localX, double localZ) {
        Vec3d position = localOffset(base, session.yaw, localX, localZ);
        DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, player.getServerWorld());
        NbtCompound nbt = new NbtCompound();
        nbt.put("block_state", NbtHelper.fromBlockState(state));
        nbt.putFloat("width", 1.5f);
        nbt.putFloat("height", 3.0f);
        entity.readNbt(nbt);
        entity.setPosition(position.x, position.y, position.z);
        entity.setYaw(session.yaw);
        entity.addCommandTag(PLACED_TAG);
        entity.addCommandTag(session.groupTag);
        entity.addCommandTag(roleTag);
        return player.getServerWorld().spawnEntity(entity) ? entity : null;
    }

    public static void applyTrainDoorState(ServerPlayerEntity sender, double x, double y, double z, float trainYawRadians, double doorValue) {
        ServerWorld world = sender.getServerWorld();
        Vec3d doorway = new Vec3d(x, y, z);
        List<Entity> roots = world.getOtherEntities(null, new Box(x - 2.5, y - 3.0, z - 2.5, x + 2.5, y + 2.0, z + 2.5),
                entity -> entity.getCommandTags().contains(DOOR_ROOT_TAG));
        for (Entity rootEntity : roots) {
            if (!(rootEntity instanceof DisplayEntity.BlockDisplayEntity root)) continue;
            String groupTag = findGroupTag(root);
            if (groupTag == null) continue;
            Vec3d base = root.getPos().add(0, -2, 0);
            if (base.squaredDistanceTo(doorway) > 9.0) continue;
            float yaw = root.getYaw();
            double trainYawDegrees = Math.toDegrees(trainYawRadians);
            double angleDifference = Math.abs(((yaw - trainYawDegrees + 540) % 360) - 180);
            angleDifference = Math.min(angleDifference, Math.abs(180 - angleDifference));
            if (angleDifference > 35) continue;
            double open = Math.max(0, Math.min(1, doorValue));
            double slide = 0.48 * open;
            List<Entity> parts = world.getOtherEntities(null, root.getBoundingBox().expand(4), e -> e.getCommandTags().contains(groupTag));
            for (Entity partEntity : parts) {
                if (!(partEntity instanceof DisplayEntity.BlockDisplayEntity part) || part == root) continue;
                boolean left = part.getCommandTags().contains(DOOR_LEFT_TAG);
                boolean right = part.getCommandTags().contains(DOOR_RIGHT_TAG);
                if (!left && !right) continue;
                double yOffset = part.getBlockState().getEntries().entrySet().stream().anyMatch(e -> e.getKey().getName().equals("half") && e.getValue().toString().equals("upper")) ? 1 : 0;
                double local = (left ? -0.25 - slide : 0.25 + slide);
                Vec3d target = localOffset(base.add(0, yOffset, 0), yaw, local, 0);
                part.setPosition(target.x, target.y, target.z);
            }
        }
    }

    private static boolean canReplace(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.isReplaceable();
    }

    private static String resolveDoorId() {
        return getBlock(DOOR_ID) != null ? DOOR_ID : FALLBACK_DOOR_ID;
    }

    private static Block getBlock(String idString) {
        Identifier id = Identifier.tryParse(idString);
        if (id == null || !Registries.BLOCK.containsId(id)) return null;
        Block block = Registries.BLOCK.get(id);
        return block == Blocks.AIR ? null : block;
    }

    private static Direction directionFromYaw(float yaw) {
        return Direction.fromRotation(Math.round(normalize(yaw) / 90.0f) * 90.0f);
    }

    private static BlockState applyFacing(BlockState state, Direction facing) {
        return withStringProperty(state, "facing", facing.asString());
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
        entity.addCommandTag(session.groupTag);
        if (player.getServerWorld().spawnEntity(entity)) session.previewParts.add(entity);
    }

    private static Vec3d localOffset(Vec3d origin, float yaw, double localX, double localZ) {
        double radians = Math.toRadians(yaw);
        return origin.add(localX * Math.cos(radians) - localZ * Math.sin(radians), 0,
                localX * Math.sin(radians) + localZ * Math.cos(radians));
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
        double pivotX = root.getX(), pivotZ = root.getZ();
        double radians = Math.toRadians(delta), cos = Math.cos(radians), sin = Math.sin(radians);
        session.yaw = normalize(session.yaw + delta);
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            double x = entity.getX() - pivotX, z = entity.getZ() - pivotZ;
            entity.setPosition(pivotX + x * cos - z * sin, entity.getY(), pivotZ + x * sin + z * cos);
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
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) entity.setPosition(entity.getX() + dx, entity.getY() + y, entity.getZ() + dz);
    }

    public static void toggleType(ServerPlayerEntity player) {
        Session session = get(player);
        if (session.hasPreview()) {
            player.sendMessage(Text.literal("Finish or cancel the current preview first"), true);
            return;
        }
        session.blockId = GLASS_ID.equals(session.blockId) ? DOOR_ID : GLASS_ID;
        player.sendMessage(Text.literal("PSD type: " + displayName(session.blockId)), true);
    }

    public static void cancel(ServerPlayerEntity player) {
        discardPreview(get(player));
        player.sendMessage(Text.literal("PSD preview cancelled"), true);
    }

    private static void discardPreview(Session session) {
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) if (!entity.isRemoved()) entity.discard();
        session.previewParts.clear();
        session.groupTag = null;
    }

    private static Entity findLookedAtPlacedPart(ServerPlayerEntity player, double distance) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(distance));
        Box search = player.getBoundingBox().stretch(player.getRotationVec(1.0f).multiply(distance)).expand(1.0);
        Entity closest = null;
        double closestSq = distance * distance;
        for (Entity entity : player.getServerWorld().getOtherEntities(player, search, e -> e.getCommandTags().contains(PLACED_TAG))) {
            Optional<Vec3d> hit = entity.getBoundingBox().expand(0.2).raycast(start, end);
            if (hit.isPresent() && start.squaredDistanceTo(hit.get()) < closestSq) {
                closestSq = start.squaredDistanceTo(hit.get());
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
        double dx = x - root.getX(), dy = y - root.getY(), dz = z - root.getZ();
        session.yaw = normalize(yaw);
        for (DisplayEntity.BlockDisplayEntity entity : session.previewParts) {
            entity.setPosition(entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz);
            entity.setYaw(session.yaw);
        }
        player.sendMessage(Text.literal("PSD properties applied"), true);
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
        return GLASS_ID.equals(id) ? "MTR glass (3 connected panels)" : "Functional Tianjin BMT PSD door";
    }

    public static final class Session {
        private String blockId = DOOR_ID;
        private String resolvedDoorId;
        private float yaw;
        private String groupTag;
        private Vec3d origin;
        private final List<DisplayEntity.BlockDisplayEntity> previewParts = new ArrayList<>();
        private boolean hasPreview() {
            previewParts.removeIf(entity -> entity == null || entity.isRemoved());
            return !previewParts.isEmpty();
        }
    }
}
