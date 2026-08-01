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

/**
 * MetroBuilder's single unified precision-editing wand.
 *
 * <p>All placement and editing input is handled by the client controller and
 * validated by server networking. The item deliberately contains no mode
 * state and performs no direct world mutation.</p>
 */
public final class BuilderWandItem extends Item {
    public BuilderWandItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(
            World world,
            net.minecraft.entity.player.PlayerEntity user,
            Hand hand
    ) {
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
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
        tooltip.add(Text.translatable("item.metrobuilder.builder_wand.tooltip.controls")
                .formatted(Formatting.DARK_GRAY));
    }
}
