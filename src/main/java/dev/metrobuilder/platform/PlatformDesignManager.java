package dev.metrobuilder.platform;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlatformDesignManager {
    public static final int MAX_ROWS = 32;
    public static final int MAX_LENGTH = 256;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private PlatformDesignManager() {}

    public static Session get(ServerPlayerEntity player) {
        return SESSIONS.computeIfAbsent(player.getUuid(), ignored -> new Session());
    }

    public static void remove(ServerPlayerEntity player) {
        SESSIONS.remove(player.getUuid());
    }

    public static List<String> defaultRows() {
        return new ArrayList<>(List.of(
                "minecraft:yellow_concrete",
                "minecraft:smooth_stone",
                "minecraft:stone_bricks",
                "minecraft:stone_bricks",
                "minecraft:stone_bricks",
                "minecraft:smooth_stone",
                "minecraft:yellow_concrete"
        ));
    }

    public static List<String> sanitizeRows(List<String> requested) {
        List<String> result = new ArrayList<>();
        for (String raw : requested) {
            if (result.size() >= MAX_ROWS) break;
            Identifier id = Identifier.tryParse(raw);
            if (id != null && Registries.BLOCK.containsId(id)) {
                result.add(id.toString());
            }
        }
        return result.isEmpty() ? defaultRows() : result;
    }

    public static int generate(World world, BlockPos start, BlockPos clickedEnd, List<String> rows) {
        int dx = clickedEnd.getX() - start.getX();
        int dz = clickedEnd.getZ() - start.getZ();
        boolean alongX = Math.abs(dx) >= Math.abs(dz);
        BlockPos end = alongX
                ? new BlockPos(clickedEnd.getX(), start.getY(), start.getZ())
                : new BlockPos(start.getX(), start.getY(), clickedEnd.getZ());

        int minAxis = alongX ? Math.min(start.getX(), end.getX()) : Math.min(start.getZ(), end.getZ());
        int maxAxis = alongX ? Math.max(start.getX(), end.getX()) : Math.max(start.getZ(), end.getZ());
        int placed = 0;

        for (int axis = minAxis; axis <= maxAxis; axis++) {
            for (int row = 0; row < rows.size(); row++) {
                Identifier id = Identifier.tryParse(rows.get(row));
                BlockState state = id == null ? Blocks.STONE.getDefaultState() : Registries.BLOCK.get(id).getDefaultState();
                BlockPos pos = alongX
                        ? new BlockPos(axis, start.getY(), start.getZ() + row)
                        : new BlockPos(start.getX() + row, start.getY(), axis);
                if (world.setBlockState(pos, state, Block.NOTIFY_ALL)) placed++;
            }
        }
        return placed;
    }

    public static final class Session {
        private List<String> rows = defaultRows();
        private BlockPos position1;
        private boolean openedDesigner;

        public List<String> rows() { return new ArrayList<>(rows); }
        public void setRows(List<String> rows) { this.rows = sanitizeRows(rows); }
        public BlockPos position1() { return position1; }
        public void setPosition1(BlockPos position1) { this.position1 = position1; }
        public void clearPosition1() { this.position1 = null; }
        public boolean openedDesigner() { return openedDesigner; }
        public void markDesignerOpened() { this.openedDesigner = true; }
    }
}
