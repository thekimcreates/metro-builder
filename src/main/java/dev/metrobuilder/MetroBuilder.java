package dev.metrobuilder;

import dev.metrobuilder.network.MetroBuilderNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetroBuilder implements ModInitializer {
    public static final String MOD_ID = "metrobuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        MetroBuilderNetworking.registerServerReceivers();
        LOGGER.info("MetroBuilder {} initialized", "0.1.0-alpha");
    }
}
