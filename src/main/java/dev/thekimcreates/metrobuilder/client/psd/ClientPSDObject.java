package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable client-side view of one synchronized PSD precision object. */
public record ClientPSDObject(
        UUID id,
        PrecisionTransform transform,
        Identifier packId,
        double doorValue
) {
    private static final Identifier PSD_TYPE_ID = MetroBuilder.id("psd");

    public ClientPSDObject {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(transform, "transform");
        Objects.requireNonNull(packId, "packId");
        if (!Double.isFinite(doorValue) || doorValue < 0.0D || doorValue > 1.0D) {
            throw new IllegalArgumentException("doorValue must be finite and between 0 and 1");
        }
    }

    /** Decodes every PSD entry from a server precision snapshot. */
    public static List<ClientPSDObject> decodeSnapshot(NbtCompound snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        final NbtList objects = snapshot.getList("Objects", NbtElement.COMPOUND_TYPE);
        final List<ClientPSDObject> psds = new ArrayList<>();

        for (int index = 0; index < objects.size(); index++) {
            decode(objects.getCompound(index)).ifPresent(psds::add);
        }

        return List.copyOf(psds);
    }

    private static Optional<ClientPSDObject> decode(NbtCompound root) {
        if (!PSD_TYPE_ID.toString().equals(root.getString("Type"))) {
            return Optional.empty();
        }

        try {
            final UUID id = UUID.fromString(root.getString("Id"));
            final PrecisionTransform transform = PrecisionTransform.fromNbt(root.getCompound("Transform"));
            final NbtCompound data = root.getCompound("Data");
            final Identifier packId = data.contains("PackId")
                    ? new Identifier(data.getString("PackId"))
                    : MetroBuilder.id("tjmetro_default");
            final double doorValue = data.contains("DoorValue")
                    ? Math.max(0.0D, Math.min(1.0D, data.getDouble("DoorValue")))
                    : 0.0D;
            return Optional.of(new ClientPSDObject(id, transform, packId, doorValue));
        } catch (RuntimeException exception) {
            MetroBuilder.LOGGER.warn("Ignoring malformed synchronized PSD object", exception);
            return Optional.empty();
        }
    }
}
