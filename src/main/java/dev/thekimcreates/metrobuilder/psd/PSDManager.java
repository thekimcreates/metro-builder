package dev.thekimcreates.metrobuilder.psd;

import dev.thekimcreates.metrobuilder.precision.PrecisionManager;
import dev.thekimcreates.metrobuilder.precision.PrecisionObject;
import dev.thekimcreates.metrobuilder.precision.PrecisionSaveData;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-thread facade for persistent precision PSD data in one dimension. */
public final class PSDManager {
    private PSDManager() {
    }

    public static PSDObject create(ServerWorld world, PrecisionTransform transform) {
        return create(world, transform, PSDObject.DEFAULT_PACK_ID);
    }

    public static PSDObject create(
            ServerWorld world,
            PrecisionTransform transform,
            Identifier packId
    ) {
        return create(world, transform, packId, PSDDisplayProperties.defaults());
    }

    public static PSDObject create(
            ServerWorld world,
            PrecisionTransform transform,
            Identifier packId,
            PSDDisplayProperties displayProperties
    ) {
        Objects.requireNonNull(world, "world");
        final PSDObject object = PSDObject.create(
                Objects.requireNonNull(transform, "transform"),
                Objects.requireNonNull(packId, "packId"),
                Objects.requireNonNull(displayProperties, "displayProperties")
        );
        precisionManager(world).add(object);
        return object;
    }

    public static Optional<PSDObject> find(ServerWorld world, UUID objectId) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(objectId, "objectId");
        return precisionManager(world)
                .find(objectId)
                .filter(PSDObject.class::isInstance)
                .map(PSDObject.class::cast);
    }

    public static Collection<PSDObject> all(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        final List<PSDObject> psds = new ArrayList<>();
        for (PrecisionObject object : precisionManager(world).objects()) {
            if (object instanceof PSDObject psd) {
                psds.add(psd);
            }
        }
        return List.copyOf(psds);
    }

    public static int count(ServerWorld world) {
        return all(world).size();
    }

    public static boolean updateTransform(
            ServerWorld world,
            UUID objectId,
            PrecisionTransform transform
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(objectId, "objectId");
        Objects.requireNonNull(transform, "transform");
        return precisionManager(world).updateTransform(objectId, transform);
    }

    public static boolean updatePackId(
            ServerWorld world,
            UUID objectId,
            Identifier packId
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(objectId, "objectId");
        Objects.requireNonNull(packId, "packId");

        final Optional<PSDObject> object = find(world, objectId);
        if (object.isEmpty() || !object.get().replacePackId(packId)) {
            return false;
        }
        precisionManager(world).markObjectChanged(objectId);
        return true;
    }


    public static boolean updateDisplayProperties(
            ServerWorld world,
            UUID objectId,
            PSDDisplayProperties displayProperties
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(objectId, "objectId");
        Objects.requireNonNull(displayProperties, "displayProperties");

        final Optional<PSDObject> object = find(world, objectId);
        if (object.isEmpty() || !object.get().replaceDisplayProperties(displayProperties)) {
            return false;
        }
        precisionManager(world).markObjectChanged(objectId);
        return true;
    }

    public static boolean updateDoorValue(
            ServerWorld world,
            UUID objectId,
            double doorValue
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(objectId, "objectId");

        final Optional<PSDObject> object = find(world, objectId);
        if (object.isEmpty() || !object.get().replaceDoorValue(doorValue)) {
            return false;
        }
        precisionManager(world).markObjectChanged(objectId);
        return true;
    }

    public static boolean remove(ServerWorld world, UUID objectId) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(objectId, "objectId");
        final Optional<PSDObject> object = find(world, objectId);
        if (object.isEmpty()) {
            return false;
        }
        return precisionManager(world).remove(objectId).isPresent();
    }

    /** Removes only PSD objects, leaving other future precision-object types untouched. */
    public static int clear(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        final List<UUID> ids = all(world).stream().map(PSDObject::id).toList();
        for (UUID id : ids) {
            precisionManager(world).remove(id);
        }
        return ids.size();
    }

    private static PrecisionManager precisionManager(ServerWorld world) {
        return PrecisionSaveData.get(world).manager();
    }
}
