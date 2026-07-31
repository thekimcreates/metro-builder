package dev.thekimcreates.metrobuilder.precision;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side collection of all precision objects in one Minecraft dimension.
 *
 * <p>All mutation methods are expected to run on the logical server thread.</p>
 */
public final class PrecisionManager {
    public static final int FORMAT_VERSION = 1;

    private static final String FORMAT_VERSION_KEY = "FormatVersion";
    private static final String OBJECTS_KEY = "Objects";

    private final Map<UUID, PrecisionObject> objects = new LinkedHashMap<>();
    private final List<NbtCompound> preservedUnknownObjects = new ArrayList<>();
    private final Runnable dirtyCallback;

    public PrecisionManager(Runnable dirtyCallback) {
        this.dirtyCallback = Objects.requireNonNull(dirtyCallback, "dirtyCallback");
    }

    public int size() {
        return objects.size();
    }

    public boolean isEmpty() {
        return objects.isEmpty();
    }

    public int preservedUnknownCount() {
        return preservedUnknownObjects.size();
    }

    public boolean contains(UUID objectId) {
        return objects.containsKey(Objects.requireNonNull(objectId, "objectId"));
    }

    public Optional<PrecisionObject> find(UUID objectId) {
        return Optional.ofNullable(objects.get(Objects.requireNonNull(objectId, "objectId")));
    }

    public Collection<PrecisionObject> objects() {
        return List.copyOf(objects.values());
    }

    public void add(PrecisionObject object) {
        Objects.requireNonNull(object, "object");
        if (objects.putIfAbsent(object.id(), object) != null) {
            throw new IllegalArgumentException("A precision object already uses UUID " + object.id());
        }
        dirtyCallback.run();
    }

    public Optional<PrecisionObject> remove(UUID objectId) {
        Objects.requireNonNull(objectId, "objectId");
        final PrecisionObject removed = objects.remove(objectId);
        if (removed != null) {
            dirtyCallback.run();
        }
        return Optional.ofNullable(removed);
    }

    public boolean updateTransform(UUID objectId, PrecisionTransform newTransform) {
        Objects.requireNonNull(objectId, "objectId");
        Objects.requireNonNull(newTransform, "newTransform");

        final PrecisionObject object = objects.get(objectId);
        if (object == null || !object.replaceTransform(newTransform)) {
            return false;
        }

        dirtyCallback.run();
        return true;
    }

    public Optional<PrecisionObject> findNearest(Vec3d point, double maximumDistance) {
        Objects.requireNonNull(point, "point");
        if (!Double.isFinite(maximumDistance) || maximumDistance < 0.0) {
            throw new IllegalArgumentException("maximumDistance must be finite and non-negative");
        }

        final double maximumSquaredDistance = maximumDistance * maximumDistance;
        PrecisionObject nearest = null;
        double nearestSquaredDistance = maximumSquaredDistance;

        for (PrecisionObject object : objects.values()) {
            final double squaredDistance = object.transform().squaredDistanceTo(point);
            if (squaredDistance <= nearestSquaredDistance) {
                nearest = object;
                nearestSquaredDistance = squaredDistance;
            }
        }

        return Optional.ofNullable(nearest);
    }

    public NbtCompound writeNbt(NbtCompound root) {
        Objects.requireNonNull(root, "root");
        root.putInt(FORMAT_VERSION_KEY, FORMAT_VERSION);

        final NbtList objectList = new NbtList();
        for (PrecisionObject object : objects.values()) {
            objectList.add(object.writeNbt());
        }
        for (NbtCompound unknownObject : preservedUnknownObjects) {
            objectList.add(unknownObject.copy());
        }
        root.put(OBJECTS_KEY, objectList);
        return root;
    }

    public NbtCompound createSnapshot() {
        return writeNbt(new NbtCompound());
    }

    public static PrecisionManager fromNbt(NbtCompound root, Runnable dirtyCallback) {
        Objects.requireNonNull(root, "root");
        final PrecisionManager manager = new PrecisionManager(dirtyCallback);
        final int storedFormatVersion = root.contains(FORMAT_VERSION_KEY)
                ? root.getInt(FORMAT_VERSION_KEY)
                : 0;

        if (storedFormatVersion > FORMAT_VERSION) {
            MetroBuilder.LOGGER.warn(
                    "Precision data uses newer format {} (supported: {}); unknown entries will be preserved",
                    storedFormatVersion,
                    FORMAT_VERSION
            );
        }

        final NbtList objectList = root.getList(OBJECTS_KEY, NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < objectList.size(); index++) {
            final NbtCompound objectNbt = objectList.getCompound(index);
            final Optional<PrecisionObject> decoded = PrecisionObjectRegistry.decode(objectNbt);

            if (decoded.isEmpty()) {
                manager.preservedUnknownObjects.add(objectNbt.copy());
                continue;
            }

            final PrecisionObject object = decoded.get();
            if (manager.objects.putIfAbsent(object.id(), object) != null) {
                MetroBuilder.LOGGER.warn(
                        "Duplicate precision object UUID {}; preserving the duplicate raw NBT",
                        object.id()
                );
                manager.preservedUnknownObjects.add(objectNbt.copy());
            }
        }

        return manager;
    }
}
