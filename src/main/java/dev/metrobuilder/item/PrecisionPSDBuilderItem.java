package dev.metrobuilder.item;

import dev.metrobuilder.display.PrecisionPSDManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class PrecisionPSDBuilderItem extends Item {
    public PrecisionPSDBuilderItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return ActionResult.PASS;
        if (PrecisionPSDManager.selectLookedAt(player)) return ActionResult.CONSUME;
        PrecisionPSDManager.startOrConfirm(player, context.getHitPos());
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            if (PrecisionPSDManager.selectLookedAt(player)) {
                return TypedActionResult.success(stack, false);
            }
            HitResult hit = raycast(world, user, RaycastContext.FluidHandling.NONE);
            Vec3d target = hit.getType() == HitResult.Type.MISS
                    ? user.getEyePos().add(user.getRotationVec(1.0f).multiply(3.0))
                    : hit.getPos();
            PrecisionPSDManager.startOrConfirm(player, target);
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
