package dev.thekimcreates.curvify;

import dev.thekimcreates.curvify.client.CurvifyClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public final class CurvifyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CurvifyClientNetworking.registerReceivers();
    }
}
