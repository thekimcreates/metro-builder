package dev.metrobuilder.item;

import dev.metrobuilder.MetroBuilder;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class MetroBuilderItems {
    public static final Item METROBUILDER_LOGO = registerItem("metrobuilder_logo", new Item(new Item.Settings()));
    public static final Item BUILDER_WAND = registerItem("builder_wand", new BuilderWandItem(new Item.Settings()));
    public static final Item PLATFORM_GENERATOR = registerItem("platform_generator", new PlatformGeneratorItem(new Item.Settings()));

    public static final RegistryKey<ItemGroup> METROBUILDER_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP, new Identifier(MetroBuilder.MOD_ID, "main"));

    public static final ItemGroup METROBUILDER_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(METROBUILDER_LOGO))
            .displayName(Text.translatable("itemGroup.metrobuilder.main"))
            .entries((displayContext, entries) -> {
                entries.add(BUILDER_WAND);
                entries.add(PLATFORM_GENERATOR);
            }).build();

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MetroBuilder.MOD_ID, name), item);
    }

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, METROBUILDER_GROUP_KEY, METROBUILDER_GROUP);
        MetroBuilder.LOGGER.info("Registered MetroBuilder items and creative tab");
    }

    private MetroBuilderItems() {}
}
