package dev.metrobuilder.item;

import dev.metrobuilder.display.DisplayManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.item.ItemUsageContext;

public final class BuilderWandItem extends Item {
    public static final String SELECTED_BLOCK_KEY = "MetroBuilderSelectedBlock";
    public static final String ROTATION_KEY = "MetroBuilderRotation";

    public BuilderWandItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        if (!world.isClient && miner instanceof ServerPlayerEntity player) {
            ItemStack stack = player.getMainHandStack();
            Identifier id = Registries.BLOCK.getId(state.getBlock());
            stack.getOrCreateNbt().putString(SELECTED_BLOCK_KEY, id.toString());
            player.sendMessage(Text.literal("Selected: " + id), true);
        }
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        ItemStack stack = context.getStack();
        if (player.isSneaking()) {
            DisplayManager.cancelPreview(player);
            player.sendMessage(Text.literal("Builder Wand preview cancelled"), true);
            return ActionResult.CONSUME;
        }

        String blockId = stack.getOrCreateNbt().getString(SELECTED_BLOCK_KEY);
        if (blockId.isBlank()) {
            player.sendMessage(Text.literal("Left-click a block with the Builder Wand first"), true);
            return ActionResult.FAIL;
        }

        Direction side = context.getSide();
        BlockPos target = context.getBlockPos().offset(side);
        float rotation = stack.getOrCreateNbt().getFloat(ROTATION_KEY);

        boolean confirmed = DisplayManager.previewOrConfirm(player, target, rotation, blockId);
        player.sendMessage(
                Text.literal(confirmed ? "Placement confirmed" : "Preview: " + blockId + " (" + (int) rotation + "°)"),
                true
        );
        return ActionResult.CONSUME;
    }
}
