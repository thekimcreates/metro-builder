package dev.metrobuilder.selection;

import net.minecraft.block.BlockState;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.*;

public final class SelectionManager {
    public static final long MAX_BLOCKS = 32768;
    private static final Map<UUID, BlockPos> POS_1 = new HashMap<>();
    private static final Map<UUID, BlockPos> POS_2 = new HashMap<>();
    private static final Map<UUID, PendingAction> PENDING = new HashMap<>();

    private SelectionManager() {}

    public static void setFirst(ServerPlayerEntity player, BlockPos pos) {
        POS_1.put(player.getUuid(), pos.toImmutable());
        PENDING.remove(player.getUuid());
        player.sendMessage(Text.literal("Position 1: " + format(pos)), true);
        showPoint(player, pos);
    }

    public static void setSecond(ServerPlayerEntity player, BlockPos pos) {
        POS_2.put(player.getUuid(), pos.toImmutable());
        PENDING.remove(player.getUuid());
        Selection selection = get(player);
        if (selection != null) {
            player.sendMessage(Text.literal("Selection: " + selection.sizeX() + " × " + selection.sizeY() + " × " + selection.sizeZ() + " (" + selection.volume() + " blocks)"), true);
            showBox(player, selection, 0.15f, 0.55f, 1.0f);
        }
    }

    public static Selection get(ServerPlayerEntity player) {
        BlockPos a = POS_1.get(player.getUuid());
        BlockPos b = POS_2.get(player.getUuid());
        return a == null || b == null ? null : new Selection(a, b);
    }

    public static boolean requireUsable(ServerPlayerEntity player) {
        Selection selection = get(player);
        if (selection == null) {
            player.sendMessage(Text.literal("Create a selection first"), true);
            return false;
        }
        if (selection.volume() > MAX_BLOCKS) {
            player.sendMessage(Text.literal("Selection is too large. Maximum: " + MAX_BLOCKS + " blocks"), true);
            return false;
        }
        return true;
    }

    public static void clear(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        POS_1.remove(id); POS_2.remove(id); PENDING.remove(id);
        player.sendMessage(Text.literal("Selection cleared"), true);
    }

    public static boolean stageOrConfirm(ServerPlayerEntity player, ActionType type, BlockPos destinationMin) {
        UUID id = player.getUuid();
        PendingAction pending = PENDING.get(id);
        if (pending != null && pending.type == type && pending.destinationMin.equals(destinationMin)) {
            PENDING.remove(id);
            return true;
        }
        PENDING.put(id, new PendingAction(type, destinationMin.toImmutable()));
        Selection selection = get(player);
        if (selection != null) showBox(player, selection.moved(destinationMin), 0.25f, 0.9f, 1.0f);
        player.sendMessage(Text.literal(type.label + " preview ready — right-click the same destination again to confirm"), true);
        return false;
    }

    public static boolean stageDeleteOrConfirm(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        PendingAction pending = PENDING.get(id);
        if (pending != null && pending.type == ActionType.DELETE) {
            PENDING.remove(id);
            return true;
        }
        Selection selection = get(player);
        PENDING.put(id, new PendingAction(ActionType.DELETE, selection.min()));
        showBox(player, selection, 1.0f, 0.2f, 0.2f);
        player.sendMessage(Text.literal("Delete armed — right-click again to confirm"), true);
        return false;
    }

    public static void cancelPending(ServerPlayerEntity player) {
        PENDING.remove(player.getUuid());
        player.sendMessage(Text.literal("Pending operation cancelled"), true);
    }

    public static void duplicate(ServerPlayerEntity player, BlockPos destinationMin) {
        Selection source = get(player);
        ServerWorld world = player.getServerWorld();
        List<BlockSnapshot> snapshots = snapshot(world, source);
        place(world, snapshots, destinationMin);
        Selection copy = source.moved(destinationMin);
        POS_1.put(player.getUuid(), copy.first()); POS_2.put(player.getUuid(), copy.second());
        player.sendMessage(Text.literal("Duplicated " + snapshots.size() + " blocks"), true);
    }

    public static void move(ServerPlayerEntity player, BlockPos destinationMin) {
        Selection source = get(player);
        ServerWorld world = player.getServerWorld();
        List<BlockSnapshot> snapshots = snapshot(world, source);
        clearRegion(world, source);
        place(world, snapshots, destinationMin);
        Selection moved = source.moved(destinationMin);
        POS_1.put(player.getUuid(), moved.first()); POS_2.put(player.getUuid(), moved.second());
        player.sendMessage(Text.literal("Moved " + snapshots.size() + " blocks"), true);
    }

    public static void delete(ServerPlayerEntity player) {
        Selection selection = get(player);
        clearRegion(player.getServerWorld(), selection);
        clear(player);
        player.sendMessage(Text.literal("Deleted " + selection.volume() + " blocks"), true);
    }

    public static void rotateClockwise(ServerPlayerEntity player) {
        Selection source = get(player);
        ServerWorld world = player.getServerWorld();
        BlockPos min = source.min();
        List<BlockSnapshot> snapshots = snapshot(world, source);
        clearRegion(world, source);
        for (BlockSnapshot snap : snapshots) {
            int newX = source.sizeZ() - 1 - snap.relative.getZ();
            int newZ = snap.relative.getX();
            BlockPos target = min.add(newX, snap.relative.getY(), newZ);
            world.setBlockState(target, snap.state.rotate(net.minecraft.util.BlockRotation.CLOCKWISE_90), 3);
        }
        Selection rotated = new Selection(min, min.add(source.sizeZ() - 1, source.sizeY() - 1, source.sizeX() - 1));
        POS_1.put(player.getUuid(), rotated.first()); POS_2.put(player.getUuid(), rotated.second());
        showBox(player, rotated, 0.65f, 0.35f, 1.0f);
        player.sendMessage(Text.literal("Rotated selection 90° clockwise"), true);
    }

    private static List<BlockSnapshot> snapshot(ServerWorld world, Selection selection) {
        List<BlockSnapshot> out = new ArrayList<>((int) selection.volume());
        BlockPos min = selection.min();
        for (BlockPos pos : BlockPos.iterate(min, selection.max())) {
            out.add(new BlockSnapshot(pos.subtract(min), world.getBlockState(pos)));
        }
        return out;
    }

    private static void place(ServerWorld world, List<BlockSnapshot> snapshots, BlockPos min) {
        for (BlockSnapshot snap : snapshots) world.setBlockState(min.add(snap.relative), snap.state, 3);
    }

    private static void clearRegion(ServerWorld world, Selection selection) {
        for (BlockPos pos : BlockPos.iterate(selection.min(), selection.max())) world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
    }

    public static void showBox(ServerPlayerEntity player, Selection s, float r, float g, float b) {
        ServerWorld world = player.getServerWorld();
        DustParticleEffect dust = new DustParticleEffect(new Vector3f(r, g, b), 1.0f);
        double minX=s.min().getX(), minY=s.min().getY(), minZ=s.min().getZ();
        double maxX=s.max().getX()+1, maxY=s.max().getY()+1, maxZ=s.max().getZ()+1;
        int steps = Math.max(2, Math.min(64, Math.max(Math.max(s.sizeX(), s.sizeY()), s.sizeZ()) * 2));
        for (int i=0;i<=steps;i++) {
            double t=(double)i/steps;
            particle(world, player, lerp(minX,maxX,t),minY,minZ,dust); particle(world, player, lerp(minX,maxX,t),maxY,minZ,dust);
            particle(world, player, lerp(minX,maxX,t),minY,maxZ,dust); particle(world, player, lerp(minX,maxX,t),maxY,maxZ,dust);
            particle(world, player, minX,lerp(minY,maxY,t),minZ,dust); particle(world, player, maxX,lerp(minY,maxY,t),minZ,dust);
            particle(world, player, minX,lerp(minY,maxY,t),maxZ,dust); particle(world, player, maxX,lerp(minY,maxY,t),maxZ,dust);
            particle(world, player, minX,minY,lerp(minZ,maxZ,t),dust); particle(world, player, maxX,minY,lerp(minZ,maxZ,t),dust);
            particle(world, player, minX,maxY,lerp(minZ,maxZ,t),dust); particle(world, player, maxX,maxY,lerp(minZ,maxZ,t),dust);
        }
    }

    private static void showPoint(ServerPlayerEntity player, BlockPos pos) {
        showBox(player, new Selection(pos, pos), 0.15f, 0.55f, 1.0f);
    }
    private static void particle(ServerWorld w, ServerPlayerEntity p, double x,double y,double z,DustParticleEffect d) { w.spawnParticles(p,d,true,x,y,z,1,0,0,0,0); }
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static String format(BlockPos p){return p.getX()+", "+p.getY()+", "+p.getZ();}
    private record BlockSnapshot(BlockPos relative, BlockState state) {}
    private record PendingAction(ActionType type, BlockPos destinationMin) {}
    public enum ActionType { MOVE("Move"), DUPLICATE("Duplicate"), DELETE("Delete"); private final String label; ActionType(String label){this.label=label;} }
}
