package dev.thekimcreates.curvify;

import dev.thekimcreates.curvify.network.CurvifyNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Curvify implements ModInitializer {
    public static final String MOD_ID = "curvify";
    public static final Logger LOGGER = LoggerFactory.getLogger("Curvify");

    @Override
    public void onInitialize() {
        CurvifyNetworking.registerServerReceivers();
        LOGGER.info("Curvify initialized without custom blocks, items, tools, or creative tabs");
    }
}
