package dev.thekimcreates.metrobuilder.client.network;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;

/** Client-side read-only snapshot of precision state received from the server. */
public final class ClientPrecisionState {
    private static Identifier dimensionId;
    private static NbtCompound snapshot = new NbtCompound();
    private static UUID selectedObjectId;

    private ClientPrecisionState() {
    }

    public static synchronized void applySnapshot(Identifier newDimensionId, NbtCompound newSnapshot) {
        dimensionId = newDimensionId;
        snapshot = newSnapshot.copy();
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

    public static synchronized void reset() {
        dimensionId = null;
        snapshot = new NbtCompound();
        selectedObjectId = null;
    }
}
