package dev.thekimcreates.metrobuilder.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * The single editing tool used by MetroBuilder.
 *
 * <p>The selection and transform modes will be added after the clean project foundation
 * has passed its first build and launch test.</p>
 */
public final class BuilderWandItem extends Item {
    public BuilderWandItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.metrobuilder.builder_wand.tooltip")
                .formatted(Formatting.GRAY));
    }
}
