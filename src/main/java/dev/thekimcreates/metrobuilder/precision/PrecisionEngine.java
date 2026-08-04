package dev.thekimcreates.metrobuilder.precision;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.network.PrecisionNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

/** Initializes the server-side precision foundation. */
public final class PrecisionEngine {
    private PrecisionEngine() {
    }

    public static void initialize() {
        PrecisionNetworking.initializeServer();

        ServerWorldEvents.LOAD.register((server, world) -> {
            final PrecisionSaveData saveData = PrecisionSaveData.get(world);
            MetroBuilder.LOGGER.info(
                    "Precision data ready for {} ({} known object(s), {} preserved unknown object(s))",
                    world.getRegistryKey().getValue(),
                    saveData.manager().size(),
                    saveData.manager().preservedUnknownCount()
            );
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PrecisionSelectionManager.clearAll());
        MetroBuilder.LOGGER.info("Precision engine initialized");
    }
}
