package dev.thekimcreates.metrobuilder.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.precision.PrecisionObjectRegistry;
import dev.thekimcreates.metrobuilder.precision.PrecisionObjectType;

/** Registers the concrete precision-object types owned by the PSD subsystem. */
public final class PSDTypes {
    public static final PrecisionObjectType<PSDObject> PSD = PrecisionObjectRegistry.register(
            new PrecisionObjectType<>(MetroBuilder.id("psd"), PSDObject::load)
    );

    private static boolean initialized;

    private PSDTypes() {
    }

    /** Forces class initialization exactly once before world precision data is loaded. */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        MetroBuilder.LOGGER.info("PSD precision-object type initialized as {}", PSD.id());
    }
}
