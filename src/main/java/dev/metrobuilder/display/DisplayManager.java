package dev.metrobuilder.display;

import dev.metrobuilder.network.PlaceDisplayPayload;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class DisplayManager {
    private static final double MAX_REACH_SQUARED = 64.0 * 64.0;

    private DisplayManager() {}

    public static void place(ServerPlayerEntity player, PlaceDisplayPayload payload) {
        Vec3d position = payload.position();

        if (player.squaredDistanceTo(position) > MAX_REACH_SQUARED
                || !player.canModifyAt(player.getWorld(), player.getBlockPos())) {
            return;
        }

        Identifier id = Identifier.tryParse(payload.blockId());
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return;
        }

        Block block = Registries.BLOCK.get(id);

        DisplayEntity.BlockDisplayEntity entity =
                new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, player.getWorld());

        // BlockDisplayEntity#setBlockState is private in Minecraft 1.20.4.
        // Load the state through the entity's normal NBT deserialization path instead.
        NbtCompound entityNbt = new NbtCompound();
        entityNbt.put("block_state", NbtHelper.fromBlockState(block.getDefaultState()));
        entity.readNbt(entityNbt);

        entity.setPosition(position.x, position.y, position.z);
        entity.setYaw(payload.yaw());
        entity.addCommandTag("metrobuilder.display");

        player.getWorld().spawnEntity(entity);
    }
}
