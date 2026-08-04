package dev.thekimcreates.metrobuilder;

import dev.thekimcreates.metrobuilder.command.MetroBuilderCommands;
import dev.thekimcreates.metrobuilder.item.MetroBuilderItemGroups;
import dev.thekimcreates.metrobuilder.item.MetroBuilderItems;
import dev.thekimcreates.metrobuilder.precision.PrecisionEngine;
import dev.thekimcreates.metrobuilder.psd.PSDTypes;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common MetroBuilder entry point. */
public final class MetroBuilder implements ModInitializer {
    public static final String MOD_ID = "metrobuilder";
    public static final String VERSION = "1.0.0-beta.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Public constructor required by Fabric Loader. */
    public MetroBuilder() {
    }

    @Override
    public void onInitialize() {
        MetroBuilderItems.initialize();
        MetroBuilderItemGroups.initialize();

        // Concrete types must be registered before any world precision save is decoded.
        PSDTypes.initialize();
        PrecisionEngine.initialize();

        MetroBuilderCommands.initialize();
        LOGGER.info("MetroBuilder {} initialized", VERSION);
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
