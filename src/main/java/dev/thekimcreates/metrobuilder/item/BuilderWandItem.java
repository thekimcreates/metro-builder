package dev.thekimcreates.metrobuilder.item;

import dev.thekimcreates.metrobuilder.network.PrecisionNetworking;
import dev.thekimcreates.metrobuilder.precision.PrecisionSelectionManager;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDManager;
import dev.thekimcreates.metrobuilder.psd.PSDObject;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

/**
 * The focused MetroBuilder precision-PSD editing tool.
 *
 * <p>Milestone 3.2 adds server-authoritative PSD placement. Right-clicking a
 * block stores the exact hit position. Right-clicking air places three blocks
 * in front of the player's eyes. The newly created PSD becomes the player's
 * active precision selection.</p>
 */
public final class BuilderWandItem extends Item {
    private static final double AIR_PLACEMENT_DISTANCE = 3.0D;
    private static final double RAYCAST_DISTANCE = 8.0D;
    private static final int PLACEMENT_COOLDOWN_TICKS = 4;

    public BuilderWandItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        final World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        return placePsd(player, context.getHitPos());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        final ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack, true);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        final HitResult hitResult = player.raycast(RAYCAST_DISTANCE, 1.0F, false);
        final Vec3d placementPosition;
        if (hitResult.getType() != HitResult.Type.MISS) {
            placementPosition = hitResult.getPos();
        } else {
            placementPosition = player.getEyePos().add(
                    player.getRotationVec(1.0F).multiply(AIR_PLACEMENT_DISTANCE)
            );
        }

        final ActionResult result = placePsd(player, placementPosition);
        return result.isAccepted()
                ? TypedActionResult.success(stack, false)
                : TypedActionResult.fail(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.metrobuilder.builder_wand.tooltip")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.metrobuilder.builder_wand.tooltip.place")
                .formatted(Formatting.DARK_GRAY));
    }

    private ActionResult placePsd(ServerPlayerEntity player, Vec3d position) {
        if (!player.isCreative()) {
            player.sendMessage(Text.translatable("message.metrobuilder.creative_only"), true);
            return ActionResult.FAIL;
        }

        final ServerWorld world = player.getServerWorld();
        final PrecisionTransform transform = PrecisionTransform
                .at(position)
                .withYaw(player.getYaw());
        final PSDObject psd = PSDManager.create(world, transform);

        PrecisionSelectionManager.select(player, psd.id());
        PrecisionNetworking.broadcastSnapshot(world);
        PrecisionNetworking.sendSelectionState(player);
        player.getItemCooldownManager().set(this, PLACEMENT_COOLDOWN_TICKS);

        player.sendMessage(
                Text.translatable(
                        "message.metrobuilder.psd_placed",
                        format(position.x),
                        format(position.y),
                        format(position.z),
                        format(player.getYaw())
                ),
                true
        );
        return ActionResult.SUCCESS;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
