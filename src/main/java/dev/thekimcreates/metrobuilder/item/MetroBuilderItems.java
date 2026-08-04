package dev.thekimcreates.metrobuilder.item;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Registers all MetroBuilder items. */
public final class MetroBuilderItems {
    public static final Item BUILDER_WAND = Registry.register(
            Registries.ITEM,
            MetroBuilder.id("builder_wand"),
            new BuilderWandItem(new Item.Settings())
    );

    private MetroBuilderItems() {
    }

    /** Forces static registration during mod initialization. */
    public static void initialize() {
        MetroBuilder.LOGGER.debug("Registered MetroBuilder items");
    }
}
