package dev.thekimcreates.metrobuilder.precision;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Describes one serializable precision-object type.
 *
 * <p>Milestone 3 will register the first concrete type: the precision PSD.</p>
 */
public final class PrecisionObjectType<T extends PrecisionObject> {
    private final Identifier id;
    private final Loader<T> loader;

    public PrecisionObjectType(Identifier id, Loader<T> loader) {
        this.id = Objects.requireNonNull(id, "id");
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public Identifier id() {
        return id;
    }

    T load(UUID objectId, PrecisionTransform transform, long revision, NbtCompound data) {
        return Objects.requireNonNull(
                loader.load(objectId, transform, revision, data.copy()),
                "Precision object loader returned null for " + id
        );
    }

    @FunctionalInterface
    public interface Loader<T extends PrecisionObject> {
        T load(UUID objectId, PrecisionTransform transform, long revision, NbtCompound data);
    }
}
