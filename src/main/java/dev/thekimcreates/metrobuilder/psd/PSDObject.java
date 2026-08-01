package dev.thekimcreates.metrobuilder.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.precision.PrecisionObject;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/** One persistent precision platform-screen-door assembly. */
public final class PSDObject extends PrecisionObject {
    public static final Identifier DEFAULT_PACK_ID = MetroBuilder.id("tjmetro_default");

    private static final String PACK_ID_KEY = "PackId";
    private static final String DOOR_VALUE_KEY = "DoorValue";
    private static final String DISPLAY_PROPERTIES_KEY = "DisplayProperties";

    private Identifier packId;
    private double doorValue;
    private PSDDisplayProperties displayProperties;

    private PSDObject(
            UUID id,
            PrecisionTransform transform,
            long revision,
            Identifier packId,
            double doorValue,
            PSDDisplayProperties displayProperties
    ) {
        super(PSDTypes.PSD, id, transform, revision);
        this.packId = Objects.requireNonNull(packId, "packId");
        this.doorValue = validateDoorValue(doorValue);
        this.displayProperties = Objects.requireNonNull(displayProperties, "displayProperties");
    }

    public static PSDObject create(PrecisionTransform transform) {
        return create(transform, DEFAULT_PACK_ID, PSDDisplayProperties.defaults());
    }

    public static PSDObject create(PrecisionTransform transform, Identifier packId) {
        return create(transform, packId, PSDDisplayProperties.defaults());
    }

    public static PSDObject create(
            PrecisionTransform transform,
            Identifier packId,
            PSDDisplayProperties displayProperties
    ) {
        return new PSDObject(
                UUID.randomUUID(),
                Objects.requireNonNull(transform, "transform"),
                0L,
                Objects.requireNonNull(packId, "packId"),
                0.0D,
                Objects.requireNonNull(displayProperties, "displayProperties")
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
                data.contains(DOOR_VALUE_KEY) ? data.getDouble(DOOR_VALUE_KEY) : 0.0D,
                data.contains(DISPLAY_PROPERTIES_KEY)
                        ? PSDDisplayProperties.fromNbt(data.getCompound(DISPLAY_PROPERTIES_KEY))
                        : PSDDisplayProperties.defaults()
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

    public double doorValue() {
        return doorValue;
    }

    boolean replaceDoorValue(double newDoorValue) {
        final double validated = validateDoorValue(newDoorValue);
        if (Double.compare(doorValue, validated) == 0) {
            return false;
        }
        doorValue = validated;
        markRevised();
        return true;
    }

    public PSDDisplayProperties displayProperties() {
        return displayProperties;
    }

    boolean replaceDisplayProperties(PSDDisplayProperties newProperties) {
        Objects.requireNonNull(newProperties, "newProperties");
        if (displayProperties.equals(newProperties)) {
            return false;
        }
        displayProperties = newProperties;
        markRevised();
        return true;
    }

    @Override
    protected void writeData(NbtCompound data) {
        data.putString(PACK_ID_KEY, packId.toString());
        data.putDouble(DOOR_VALUE_KEY, doorValue);
        data.put(DISPLAY_PROPERTIES_KEY, displayProperties.toNbt());
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
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("doorValue must be finite and between 0 and 1");
        }
        return value;
    }
}
