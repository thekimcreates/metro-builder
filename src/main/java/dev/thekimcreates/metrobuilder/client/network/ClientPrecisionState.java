package dev.thekimcreates.metrobuilder.client.network;

import dev.thekimcreates.metrobuilder.client.psd.ClientPSDObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Client-side read-only snapshot of precision state received from the server. */
public final class ClientPrecisionState {
    private static Identifier dimensionId;
    private static NbtCompound snapshot = new NbtCompound();
    private static UUID selectedObjectId;
    private static List<ClientPSDObject> psds = List.of();

    private ClientPrecisionState() {
    }

    public static synchronized void applySnapshot(Identifier newDimensionId, NbtCompound newSnapshot) {
        dimensionId = newDimensionId;
        snapshot = newSnapshot.copy();
        psds = ClientPSDObject.decodeSnapshot(snapshot);
    }

    public static synchronized void applySelection(Identifier selectedDimensionId, UUID objectId) {
        if (dimensionId != null && dimensionId.equals(selectedDimensionId)) {
            selectedObjectId = objectId;
        } else {
            selectedObjectId = null;
        }
    }

    public static synchronized void clearSelection() {
        selectedObjectId = null;
    }

    public static synchronized Optional<Identifier> dimensionId() {
        return Optional.ofNullable(dimensionId);
    }

    public static synchronized NbtCompound snapshot() {
        return snapshot.copy();
    }

    public static synchronized int objectCount() {
        return snapshot.getList("Objects", NbtElement.COMPOUND_TYPE).size();
    }

    public static synchronized Optional<UUID> selectedObjectId() {
        return Optional.ofNullable(selectedObjectId);
    }

    public static synchronized List<ClientPSDObject> psds() {
        return List.copyOf(psds);
    }

    public static synchronized Optional<ClientPSDObject> findPsd(UUID objectId) {
        return psds.stream().filter(psd -> psd.id().equals(objectId)).findFirst();
    }

    public static synchronized void updatePsdTransform(
            UUID objectId,
            dev.thekimcreates.metrobuilder.precision.PrecisionTransform transform
    ) {
        psds = psds.stream()
                .map(psd -> psd.id().equals(objectId)
                        ? new ClientPSDObject(psd.id(), transform, psd.packId(), psd.doorValue())
                        : psd)
                .toList();
    }

    public static synchronized void reset() {
        dimensionId = null;
        snapshot = new NbtCompound();
        selectedObjectId = null;
        psds = List.of();
    }
}
