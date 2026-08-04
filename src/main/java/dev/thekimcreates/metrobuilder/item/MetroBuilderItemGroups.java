package dev.thekimcreates.metrobuilder.item;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

/** Registers MetroBuilder's Creative inventory tab. */
public final class MetroBuilderItemGroups {
    public static final ItemGroup MAIN = Registry.register(
            Registries.ITEM_GROUP,
            MetroBuilder.id("main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.metrobuilder.main"))
                    .icon(() -> new ItemStack(MetroBuilderItems.BUILDER_WAND))
                    .entries((displayContext, entries) -> entries.add(MetroBuilderItems.BUILDER_WAND))
                    .build()
    );

    private MetroBuilderItemGroups() {
    }

    /** Forces static registration during mod initialization. */
    public static void initialize() {
        MetroBuilder.LOGGER.debug("Registered MetroBuilder item groups");
    }
}
