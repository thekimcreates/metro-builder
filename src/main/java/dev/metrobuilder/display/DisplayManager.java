package dev.metrobuilder.display;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DisplayManager {
    private static final Map<UUID, DisplayEntity.BlockDisplayEntity> PREVIEWS = new HashMap<>();

    private DisplayManager() {
    }

    public static boolean previewOrConfirm(ServerPlayerEntity player, BlockPos position, float yaw, String blockId) {
        DisplayEntity.BlockDisplayEntity existing = PREVIEWS.remove(player.getUuid());
        if (existing != null && !existing.isRemoved()) {
            existing.removeCommandTag("metrobuilder.preview");
            existing.addCommandTag("metrobuilder.display");
            existing.setGlowing(false);
            return true;
        }

        Identifier id = Identifier.tryParse(blockId);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return false;
        }

        Block block = Registries.BLOCK.get(id);
        DisplayEntity.BlockDisplayEntity entity =
                new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, player.getServerWorld());

        NbtCompound entityNbt = new NbtCompound();
        entityNbt.put("block_state", NbtHelper.fromBlockState(block.getDefaultState()));
        entity.readNbt(entityNbt);

        entity.setPosition(position.getX(), position.getY(), position.getZ());
        entity.setYaw(yaw);
        entity.addCommandTag("metrobuilder.preview");
        entity.setGlowing(true);

        if (!player.getServerWorld().spawnEntity(entity)) {
            return false;
        }

        PREVIEWS.put(player.getUuid(), entity);
        return false;
    }

    public static void rotatePreview(ServerPlayerEntity player, float yaw) {
        DisplayEntity.BlockDisplayEntity entity = PREVIEWS.get(player.getUuid());
        if (entity == null || entity.isRemoved()) {
            PREVIEWS.remove(player.getUuid());
            return;
        }
        entity.setYaw(yaw);
    }

    public static void cancelPreview(ServerPlayerEntity player) {
        DisplayEntity.BlockDisplayEntity entity = PREVIEWS.remove(player.getUuid());
        if (entity != null && !entity.isRemoved()) {
            entity.discard();
        }
    }
}
