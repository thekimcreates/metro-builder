package dev.thekimcreates.metrobuilder.precision;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Registry and NBT decoder for precision-object types. */
public final class PrecisionObjectRegistry {
    private static final Map<Identifier, PrecisionObjectType<?>> TYPES = new LinkedHashMap<>();

    private PrecisionObjectRegistry() {
    }

    public static synchronized <T extends PrecisionObject> PrecisionObjectType<T> register(
            PrecisionObjectType<T> type
    ) {
        Objects.requireNonNull(type, "type");
        if (TYPES.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("Duplicate precision object type: " + type.id());
        }

        MetroBuilder.LOGGER.info("Registered precision object type {}", type.id());
        return type;
    }

    public static synchronized Optional<PrecisionObjectType<?>> find(Identifier id) {
        return Optional.ofNullable(TYPES.get(Objects.requireNonNull(id, "id")));
    }

    public static synchronized Map<Identifier, PrecisionObjectType<?>> registeredTypes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(TYPES));
    }

    public static Optional<PrecisionObject> decode(NbtCompound root) {
        Objects.requireNonNull(root, "root");

        try {
            final Identifier typeId = new Identifier(root.getString(PrecisionObject.typeKey()));
            final PrecisionObjectType<?> type;
            synchronized (PrecisionObjectRegistry.class) {
                type = TYPES.get(typeId);
            }

            if (type == null) {
                return Optional.empty();
            }

            final UUID objectId = UUID.fromString(root.getString(PrecisionObject.idKey()));
            final long revision = Math.max(0L, root.getLong(PrecisionObject.revisionKey()));
            final PrecisionTransform transform = PrecisionTransform.fromNbt(
                    root.getCompound(PrecisionObject.transformKey())
            );
            final NbtCompound data = root.getCompound(PrecisionObject.dataKey());
            final PrecisionObject object = loadUnchecked(type, objectId, transform, revision, data);

            if (!object.id().equals(objectId)) {
                throw new IllegalStateException("Precision loader changed object UUID for " + typeId);
            }
            if (!object.type().id().equals(typeId)) {
                throw new IllegalStateException("Precision loader returned the wrong type for " + typeId);
            }

            return Optional.of(object);
        } catch (RuntimeException exception) {
            MetroBuilder.LOGGER.warn("Could not decode a precision object; preserving its raw NBT", exception);
            return Optional.empty();
        }
    }

    private static <T extends PrecisionObject> T loadUnchecked(
            PrecisionObjectType<T> type,
            UUID objectId,
            PrecisionTransform transform,
            long revision,
            NbtCompound data
    ) {
        return type.load(objectId, transform, revision, data);
    }
}
