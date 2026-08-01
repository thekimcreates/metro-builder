package dev.thekimcreates.metrobuilder.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** The single, mode-free MetroBuilder precision PSD editing tool. */
public final class BuilderWandItem extends Item {
    public BuilderWandItem(Settings settings) {
        super(settings.maxCount(1));
    }

    /** Client input and server-authoritative packets are handled by the wand controller. */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            if (context.getWorld().isClient) {
                context.getPlayer().sendMessage(
                        Text.translatable("message.metrobuilder.creative_only"),
                        true
                );
            }
            return ActionResult.FAIL;
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(
            World world,
            net.minecraft.entity.player.PlayerEntity user,
            Hand hand
    ) {
        final ItemStack stack = user.getStackInHand(hand);
        if (!user.isCreative()) {
            if (world.isClient) {
                user.sendMessage(Text.translatable("message.metrobuilder.creative_only"), true);
            }
            return TypedActionResult.fail(stack);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            World world,
            List<Text> tooltip,
            TooltipContext context
    ) {
        tooltip.add(Text.translatable("item.metrobuilder.builder_wand.tooltip")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.metrobuilder.builder_wand.tooltip.place")
                .formatted(Formatting.DARK_GRAY));
    }
}
