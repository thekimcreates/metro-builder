package dev.metrobuilder.mixin.client;

import dev.metrobuilder.network.MetroBuilderNetworking;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(targets = "org.mtr.mod.render.RenderVehicleHelper", remap = false)
public abstract class RenderVehicleHelperMixin {
    private static long lastSentNanos;
    private static double lastX, lastY, lastZ, lastValue;

    @Inject(method = "canOpenDoors", at = @At("HEAD"), remap = false)
    private static void metrobuilder$captureDoorway(@Coerce Object doorway, @Coerce Object positionAndRotation, double doorValue, CallbackInfoReturnable<Boolean> cir) {
        try {
            double minX = invokeDouble(doorway, "getMinXMapped");
            double maxX = invokeDouble(doorway, "getMaxXMapped");
            double maxY = invokeDouble(doorway, "getMaxYMapped");
            double minZ = invokeDouble(doorway, "getMinZMapped");
            double maxZ = invokeDouble(doorway, "getMaxZMapped");
            double localX = (minX + maxX) * 0.5;
            double localZ = (minZ + maxZ) * 0.5;

            Field positionField = positionAndRotation.getClass().getField("position");
            Object position = positionField.get(positionAndRotation);
            double px = readNumberField(position, "x");
            double py = readNumberField(position, "y");
            double pz = readNumberField(position, "z");
            double yaw = readNumberField(positionAndRotation, "yaw");
            double pitch = readNumberField(positionAndRotation, "pitch");

            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            double cy = Math.cos(yaw), sy = Math.sin(yaw);
            double pitchedY = maxY * cp - localZ * sp;
            double pitchedZ = maxY * sp + localZ * cp;
            double worldX = px + localX * cy + pitchedZ * sy;
            double worldY = py + pitchedY;
            double worldZ = pz - localX * sy + pitchedZ * cy;

            long now = System.nanoTime();
            boolean changed = Math.abs(worldX-lastX) > .05 || Math.abs(worldY-lastY) > .05 || Math.abs(worldZ-lastZ) > .05 || Math.abs(doorValue-lastValue) > .01;
            if (changed || now - lastSentNanos > 50_000_000L) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeDouble(worldX); buf.writeDouble(worldY); buf.writeDouble(worldZ);
                buf.writeFloat((float) yaw); buf.writeDouble(doorValue);
                ClientPlayNetworking.send(MetroBuilderNetworking.MTR_DOOR_STATE, buf);
                lastX=worldX; lastY=worldY; lastZ=worldZ; lastValue=doorValue; lastSentNanos=now;
            }
        } catch (Throwable ignored) {
        }
    }

    private static double invokeDouble(Object target, String name) throws Exception {
        Method method = target.getClass().getMethod(name);
        return ((Number) method.invoke(target)).doubleValue();
    }

    private static double readNumberField(Object target, String name) throws Exception {
        Field field = target.getClass().getField(name);
        return ((Number) field.get(target)).doubleValue();
    }
}
