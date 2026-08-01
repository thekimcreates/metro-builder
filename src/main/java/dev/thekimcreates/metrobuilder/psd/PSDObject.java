package dev.thekimcreates.metrobuilder.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.precision.PrecisionObject;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * One persistent precision platform-screen-door object.
 *
 * <p>Milestone 3.1 stores the PSD's identity and data only. Rendering and Builder Wand
 * placement are intentionally added in later Milestone 3 commits.</p>
 */
public final class PSDObject extends PrecisionObject {
    public static final Identifier DEFAULT_PACK_ID = MetroBuilder.id("tjmetro_default");

    private static final String PACK_ID_KEY = "PackId";
    private static final String DOOR_VALUE_KEY = "DoorValue";

    private Identifier packId;
    private final double doorValue;

    private PSDObject(
            UUID id,
            PrecisionTransform transform,
            long revision,
            Identifier packId,
            double doorValue
    ) {
        super(PSDTypes.PSD, id, transform, revision);
        this.packId = Objects.requireNonNull(packId, "packId");
        this.doorValue = validateDoorValue(doorValue);
    }

    /** Creates a new closed PSD using the built-in TJMetro-default pack reference. */
    public static PSDObject create(PrecisionTransform transform) {
        return create(transform, DEFAULT_PACK_ID);
    }

    /** Creates a new closed PSD using the requested pack reference. */
    public static PSDObject create(PrecisionTransform transform, Identifier packId) {
        return new PSDObject(
                UUID.randomUUID(),
                Objects.requireNonNull(transform, "transform"),
                0L,
                Objects.requireNonNull(packId, "packId"),
                0.0
        );
    }

    static PSDObject load(
            UUID id,
            PrecisionTransform transform,
            long revision,
            NbtCompound data
    ) {
        Objects.requireNonNull(data, "data");
        return new PSDObject(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(transform, "transform"),
                revision,
                readPackId(data),
                data.contains(DOOR_VALUE_KEY) ? data.getDouble(DOOR_VALUE_KEY) : 0.0
        );
    }

    public Identifier packId() {
        return packId;
    }

    boolean replacePackId(Identifier newPackId) {
        Objects.requireNonNull(newPackId, "newPackId");
        if (packId.equals(newPackId)) {
            return false;
        }
        packId = newPackId;
        markRevised();
        return true;
    }

    /** Door animation value reserved for Beta 2 synchronization: 0 = closed, 1 = open. */
    public double doorValue() {
        return doorValue;
    }

    @Override
    protected void writeData(NbtCompound data) {
        data.putString(PACK_ID_KEY, packId.toString());
        data.putDouble(DOOR_VALUE_KEY, doorValue);
    }

    private static Identifier readPackId(NbtCompound data) {
        if (!data.contains(PACK_ID_KEY)) {
            return DEFAULT_PACK_ID;
        }

        try {
            return new Identifier(data.getString(PACK_ID_KEY));
        } catch (RuntimeException exception) {
            MetroBuilder.LOGGER.warn(
                    "PSD object contained invalid pack ID '{}'; using {}",
                    data.getString(PACK_ID_KEY),
                    DEFAULT_PACK_ID
            );
            return DEFAULT_PACK_ID;
        }
    }

    private static double validateDoorValue(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("doorValue must be finite");
        }
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("doorValue must be between 0 and 1");
        }
        return value;
    }
}
