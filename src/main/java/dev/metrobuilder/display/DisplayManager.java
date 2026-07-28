package dev.metrobuilder.display;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
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
    private static final Map<UUID, UUID> PREVIEWS = new HashMap<>();

    private DisplayManager() {}

    public static boolean previewOrConfirm(ServerPlayerEntity player, BlockPos position, float yaw, String blockId) {
        UUID existingId = PREVIEWS.get(player.getUuid());
        if (existingId != null) {
            Entity existing = player.getWorld().getEntity(existingId);
            if (existing != null) {
                existing.removeCommandTag("metrobuilder.preview");
                existing.addCommandTag("metrobuilder.display");
                PREVIEWS.remove(player.getUuid());
                return true;
            }
            PREVIEWS.remove(player.getUuid());
        }

        Identifier id = Identifier.tryParse(blockId);
        if (id == null || !Registries.BLOCK.containsId(id)) return false;

        Block block = Registries.BLOCK.get(id);
        DisplayEntity.BlockDisplayEntity entity =
                new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, player.getWorld());

        NbtCompound entityNbt = new NbtCompound();
        entityNbt.put("block_state", NbtHelper.fromBlockState(block.getDefaultState()));
        entity.readNbt(entityNbt);

        entity.setPosition(position.getX(), position.getY(), position.getZ());
        entity.setYaw(yaw);
        entity.addCommandTag("metrobuilder.preview");
        entity.setGlowing(true);

        player.getWorld().spawnEntity(entity);
        PREVIEWS.put(player.getUuid(), entity.getUuid());
        return false;
    }

    public static void rotatePreview(ServerPlayerEntity player, float yaw) {
        UUID id = PREVIEWS.get(player.getUuid());
        if (id == null) return;
        Entity entity = player.getWorld().getEntity(id);
        if (entity != null) entity.setYaw(yaw);
    }

    public static void cancelPreview(ServerPlayerEntity player) {
        UUID id = PREVIEWS.remove(player.getUuid());
        if (id == null) return;
        Entity entity = player.getWorld().getEntity(id);
        if (entity != null) entity.discard();
    }
}
