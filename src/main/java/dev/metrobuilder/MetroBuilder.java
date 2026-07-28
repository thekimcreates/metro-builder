package dev.metrobuilder;

import dev.metrobuilder.item.MetroBuilderItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetroBuilder implements ModInitializer {

    public static final String MOD_ID = "metrobuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        MetroBuilderItems.register();
        dev.metrobuilder.network.MetroBuilderNetworking.registerServerReceivers();
        LOGGER.info("MetroBuilder common initialization complete");
    }
}
