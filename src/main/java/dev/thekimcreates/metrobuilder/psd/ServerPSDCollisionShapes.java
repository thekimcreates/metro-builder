package dev.thekimcreates.metrobuilder.psd;

import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.List;

/** Builds authoritative server-side collision for precision PSD objects. */
public final class ServerPSDCollisionShapes {
    private static final double SEGMENT_WIDTH = 0.25D;
    private static final Identifier SINGLE_PANEL_PACK =
            new Identifier("metrobuilder", "seoul_bulky_glass_panel");

    private ServerPSDCollisionShapes() {}

    public static void append(ServerWorld world, Box entityBox, Vec3d movement,
                              List<VoxelShape> collisions) {
        final Box swept = entityBox.stretch(movement).expand(0.01D);
        for (PSDObject psd : PSDManager.all(world)) {
            final PrecisionTransform transform = psd.transform();
            if (transform.squaredDistanceTo(swept.getCenter()) > 4096.0D) continue;
            if (SINGLE_PANEL_PACK.equals(psd.packId())) {
                appendSegmented(collisions, swept, transform,
                        -0.75D, 0.75D, 0.0D, 3.0D, -0.125D, 0.125D);
                continue;
            }
            final double open = Math.max(0.0D, Math.min(1.0D, psd.doorValue()));
            appendSegmented(collisions, swept, transform, -2.5D, -1.0D, 0, 2.1D, 0, 0.125D);
            appendSegmented(collisions, swept, transform, 1.0D, 2.5D, 0, 2.1D, 0, 0.125D);
            appendSegmented(collisions, swept, transform, -2.5D, 2.5D, 2.1D, 3, -0.18D, 0.18D);
            appendSegmented(collisions, swept, transform, -1.0D-open, -open, 0, 2.1D, -0.125D, 0);
            appendSegmented(collisions, swept, transform, open, 1.0D+open, 0, 2.1D, -0.125D, 0);
        }
    }

    private static void appendSegmented(List<VoxelShape> out, Box swept, PrecisionTransform transform,
            double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        for (double x = minX; x < maxX; x += SEGMENT_WIDTH) {
            final Box box = transformBox(transform, x, Math.min(maxX, x + SEGMENT_WIDTH),
                    minY, maxY, minZ, maxZ);
            if (box.intersects(swept)) out.add(VoxelShapes.cuboid(box));
        }
    }

    private static Box transformBox(PrecisionTransform t, double x0, double x1,
                                    double y0, double y1, double z0, double z1) {
        double ax=Double.POSITIVE_INFINITY, ay=ax, az=ax;
        double bx=Double.NEGATIVE_INFINITY, by=bx, bz=bx;
        for(double x:new double[]{x0,x1}) for(double y:new double[]{y0,y1}) for(double z:new double[]{z0,z1}) {
            final Vec3d p=point(t,x,y,z);
            ax=Math.min(ax,p.x); ay=Math.min(ay,p.y); az=Math.min(az,p.z);
            bx=Math.max(bx,p.x); by=Math.max(by,p.y); bz=Math.max(bz,p.z);
        }
        return new Box(ax,ay,az,bx,by,bz);
    }

    private static Vec3d point(PrecisionTransform t,double x,double y,double z) {
        x*=t.scaleX(); y*=t.scaleY(); z*=t.scaleZ();
        double a=Math.toRadians(t.roll()), n=x*Math.cos(a)-y*Math.sin(a);
        y=x*Math.sin(a)+y*Math.cos(a); x=n;
        a=Math.toRadians(t.pitch()); n=y*Math.cos(a)-z*Math.sin(a);
        z=y*Math.sin(a)+z*Math.cos(a); y=n;
        a=Math.toRadians(-t.yaw()); n=x*Math.cos(a)+z*Math.sin(a);
        z=-x*Math.sin(a)+z*Math.cos(a); x=n;
        return new Vec3d(t.x()+x,t.y()+y,t.z()+z);
    }
}
