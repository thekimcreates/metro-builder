package dev.thekimcreates.metrobuilder.client;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.fabricmc.api.ClientModInitializer;

/** Client-only MetroBuilder entry point. */
public final class MetroBuilderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MetroBuilder.LOGGER.info("MetroBuilder client initialized");
    }
}
