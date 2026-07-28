package dev.metrobuilder.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

public record PlaceDisplayPayload(Vec3d position, float yaw, String blockId) {
    public void write(PacketByteBuf buf) {
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        buf.writeFloat(yaw);
        buf.writeString(blockId, 128);
    }

    public static PlaceDisplayPayload read(PacketByteBuf buf) {
        return new PlaceDisplayPayload(
                new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readFloat(),
                buf.readString(128)
        );
    }
}
