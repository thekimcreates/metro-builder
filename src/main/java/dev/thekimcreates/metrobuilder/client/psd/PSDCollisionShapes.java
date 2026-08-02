package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.client.network.ClientPrecisionState;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.List;

/** Builds collision shapes matching the rendered precision PSD assembly. */
public final class PSDCollisionShapes {
    private static final double SEGMENT_WIDTH = 0.25D;

    private PSDCollisionShapes() {
    }

    public static void append(Box entityBox, Vec3d movement, List<VoxelShape> collisions) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || ClientPrecisionState.dimensionId()
                .filter(client.world.getRegistryKey().getValue()::equals).isEmpty()) {
            return;
        }

        final Box sweptBox = entityBox.stretch(movement).expand(0.01D);
        for (ClientPSDObject psd : ClientPrecisionState.psds()) {
            if (psd.transform().squaredDistanceTo(sweptBox.getCenter()) > 64.0D * 64.0D) {
                continue;
            }

            final double open = MtrTrainDoorLink.findDoorValue(client, psd)
                    .orElse(psd.doorValue());
            final double clampedOpen = Math.max(0.0D, Math.min(1.0D, open));
            final PrecisionTransform transform = psd.transform();

            // Fixed front rail.
            appendSegmented(collisions, sweptBox, transform, -2.5D, -1.0D, 0.0D, 2.1D, 0.0D, 0.125D);
            appendSegmented(collisions, sweptBox, transform, 1.0D, 2.5D, 0.0D, 2.1D, 0.0D, 0.125D);

            // Full header housing.
            appendSegmented(collisions, sweptBox, transform, -2.5D, 2.5D, 2.1D, 3.0D, -0.18D, 0.18D);

            // Animated rear rail. These use the exact same one-block travel as rendering.
            appendSegmented(collisions, sweptBox, transform, -1.0D - clampedOpen, -clampedOpen, 0.0D, 2.1D, -0.125D, 0.0D);
            appendSegmented(collisions, sweptBox, transform, clampedOpen, 1.0D + clampedOpen, 0.0D, 2.1D, -0.125D, 0.0D);
        }
    }

    private static void appendSegmented(
            List<VoxelShape> collisions,
            Box sweptBox,
            PrecisionTransform transform,
            double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ
    ) {
        for (double segmentMinX = minX; segmentMinX < maxX; segmentMinX += SEGMENT_WIDTH) {
            final double segmentMaxX = Math.min(maxX, segmentMinX + SEGMENT_WIDTH);
            final Box transformed = transformBox(
                    transform,
                    segmentMinX,
                    segmentMaxX,
                    minY,
                    maxY,
                    minZ,
                    maxZ
            );
            if (transformed.intersects(sweptBox)) {
                collisions.add(VoxelShapes.cuboid(transformed));
            }
        }
    }

    private static Box transformBox(
            PrecisionTransform transform,
            double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ
    ) {
        double worldMinX = Double.POSITIVE_INFINITY;
        double worldMinY = Double.POSITIVE_INFINITY;
        double worldMinZ = Double.POSITIVE_INFINITY;
        double worldMaxX = Double.NEGATIVE_INFINITY;
        double worldMaxY = Double.NEGATIVE_INFINITY;
        double worldMaxZ = Double.NEGATIVE_INFINITY;

        for (double x : new double[]{minX, maxX}) {
            for (double y : new double[]{minY, maxY}) {
                for (double z : new double[]{minZ, maxZ}) {
                    final Vec3d point = transformPoint(transform, x, y, z);
                    worldMinX = Math.min(worldMinX, point.x);
                    worldMinY = Math.min(worldMinY, point.y);
                    worldMinZ = Math.min(worldMinZ, point.z);
                    worldMaxX = Math.max(worldMaxX, point.x);
                    worldMaxY = Math.max(worldMaxY, point.y);
                    worldMaxZ = Math.max(worldMaxZ, point.z);
                }
            }
        }
        return new Box(worldMinX, worldMinY, worldMinZ, worldMaxX, worldMaxY, worldMaxZ);
    }

    private static Vec3d transformPoint(PrecisionTransform transform, double x, double y, double z) {
        x *= transform.scaleX();
        y *= transform.scaleY();
        z *= transform.scaleZ();

        final double roll = Math.toRadians(transform.roll());
        final double rolledX = x * Math.cos(roll) - y * Math.sin(roll);
        final double rolledY = x * Math.sin(roll) + y * Math.cos(roll);
        x = rolledX;
        y = rolledY;

        final double pitch = Math.toRadians(transform.pitch());
        final double pitchedY = y * Math.cos(pitch) - z * Math.sin(pitch);
        final double pitchedZ = y * Math.sin(pitch) + z * Math.cos(pitch);
        y = pitchedY;
        z = pitchedZ;

        final double yaw = Math.toRadians(-transform.yaw());
        final double yawedX = x * Math.cos(yaw) + z * Math.sin(yaw);
        final double yawedZ = -x * Math.sin(yaw) + z * Math.cos(yaw);

        return new Vec3d(
                transform.x() + yawedX,
                transform.y() + y,
                transform.z() + yawedZ
        );
    }
}
