package dev.thekimcreates.metrobuilder.client;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.client.builder.BuilderWandClientController;
import dev.thekimcreates.metrobuilder.client.network.PrecisionClientNetworking;
import dev.thekimcreates.metrobuilder.client.psd.PSDWorldRenderer;
import net.fabricmc.api.ClientModInitializer;

/** Client-only MetroBuilder entry point. */
public final class MetroBuilderClient implements ClientModInitializer {
    /** Public constructor required by Fabric Loader. */
    public MetroBuilderClient() {
    }

    @Override
    public void onInitializeClient() {
        PrecisionClientNetworking.initializeClient();
        BuilderWandClientController.initialize();
        PSDWorldRenderer.initialize();
        MetroBuilder.LOGGER.info("MetroBuilder client initialized");
    }
}
