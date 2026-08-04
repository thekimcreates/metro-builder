package dev.thekimcreates.metrobuilder.psd;

import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import java.util.List;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_259;
import net.minecraft.class_265;
import net.minecraft.class_3218;

public final class ServerPSDCollisionShapes {
    private static final double SEGMENT_WIDTH = 0.25;

    private ServerPSDCollisionShapes() {}

    public static void append(class_3218 world, class_238 entityBox, class_243 movement,
                              List<class_265> collisions) {
        class_238 sweptBox = entityBox.method_18804(movement).method_1014(0.01);
        for (PSDObject psd : PSDManager.all(world)) {
            PrecisionTransform transform = psd.transform();
            if (transform.squaredDistanceTo(sweptBox.method_1005()) > 4096.0) continue;
            if ("metrobuilder:seoul_bulky_glass_panel".equals(psd.packId().toString())) {
                appendSegmented(collisions, sweptBox, transform, -0.75, 0.75, 0, 3, -0.125, 0.125);
                continue;
            }
            double open = Math.max(0.0, Math.min(1.0, psd.doorValue()));
            appendSegmented(collisions, sweptBox, transform, -2.5, -1.0, 0, 2.1, 0, 0.125);
            appendSegmented(collisions, sweptBox, transform, 1.0, 2.5, 0, 2.1, 0, 0.125);
            appendSegmented(collisions, sweptBox, transform, -2.5, 2.5, 2.1, 3.0, -0.18, 0.18);
            appendSegmented(collisions, sweptBox, transform, -1.0 - open, -open, 0, 2.1, -0.125, 0);
            appendSegmented(collisions, sweptBox, transform, open, 1.0 + open, 0, 2.1, -0.125, 0);
        }
    }

    private static void appendSegmented(List<class_265> collisions, class_238 sweptBox,
                                        PrecisionTransform transform, double minX, double maxX,
                                        double minY, double maxY, double minZ, double maxZ) {
        for (double x = minX; x < maxX; x += SEGMENT_WIDTH) {
            class_238 transformed = transformBox(transform, x, Math.min(maxX, x + SEGMENT_WIDTH),
                    minY, maxY, minZ, maxZ);
            if (transformed.method_994(sweptBox)) collisions.add(class_259.method_1078(transformed));
        }
    }

    private static class_238 transformBox(PrecisionTransform t, double minX, double maxX,
                                          double minY, double maxY, double minZ, double maxZ) {
        double wx0 = Double.POSITIVE_INFINITY, wy0 = Double.POSITIVE_INFINITY, wz0 = Double.POSITIVE_INFINITY;
        double wx1 = Double.NEGATIVE_INFINITY, wy1 = Double.NEGATIVE_INFINITY, wz1 = Double.NEGATIVE_INFINITY;
        for (double x : new double[]{minX, maxX}) for (double y : new double[]{minY, maxY})
            for (double z : new double[]{minZ, maxZ}) {
                class_243 p = transformPoint(t, x, y, z);
                wx0 = Math.min(wx0, p.field_1352); wy0 = Math.min(wy0, p.field_1351); wz0 = Math.min(wz0, p.field_1350);
                wx1 = Math.max(wx1, p.field_1352); wy1 = Math.max(wy1, p.field_1351); wz1 = Math.max(wz1, p.field_1350);
            }
        return new class_238(wx0, wy0, wz0, wx1, wy1, wz1);
    }

    private static class_243 transformPoint(PrecisionTransform t, double x, double y, double z) {
        x *= t.scaleX(); y *= t.scaleY(); z *= t.scaleZ();
        double a = Math.toRadians(t.roll()), nx = x * Math.cos(a) - y * Math.sin(a);
        y = x * Math.sin(a) + y * Math.cos(a); x = nx;
        a = Math.toRadians(t.pitch()); double ny = y * Math.cos(a) - z * Math.sin(a);
        z = y * Math.sin(a) + z * Math.cos(a); y = ny;
        a = Math.toRadians(-t.yaw()); nx = x * Math.cos(a) + z * Math.sin(a);
        z = -x * Math.sin(a) + z * Math.cos(a); x = nx;
        return new class_243(t.x() + x, t.y() + y, t.z() + z);
    }
}
