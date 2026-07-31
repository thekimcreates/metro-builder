package dev.metrobuilder.precision;

import dev.metrobuilder.MetroBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-side registry and repository for precision objects in one dimension.
 */
public final class PrecisionObjectManager {
    private static final Map<Identifier, ObjectFactory<? extends PrecisionObject>> TYPE_FACTORIES = new LinkedHashMap<>();
    private static final Map<ServerWorld, PrecisionObjectManager> WORLD_MANAGERS = new WeakHashMap<>();

    private final PrecisionSaveData saveData;
    private final Map<UUID, PrecisionObject> objects = new LinkedHashMap<>();

    private PrecisionObjectManager(PrecisionSaveData saveData) {
        this.saveData = saveData;
        hydrateAllKnownTypes();
    }

    public static PrecisionObjectManager get(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        synchronized (WORLD_MANAGERS) {
            return WORLD_MANAGERS.computeIfAbsent(world, key -> new PrecisionObjectManager(PrecisionSaveData.get(key)));
        }
    }

    /**
     * Registers a precision object type before objects of that type are created.
     * Existing raw save entries are hydrated immediately in already-open worlds.
     */
    public static <T extends PrecisionObject> void registerType(Identifier typeId, ObjectFactory<T> factory) {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(factory, "factory");

        synchronized (TYPE_FACTORIES) {
            if (TYPE_FACTORIES.putIfAbsent(typeId, factory) != null) {
                throw new IllegalStateException("Precision object type is already registered: " + typeId);
            }
        }

        synchronized (WORLD_MANAGERS) {
            for (PrecisionObjectManager manager : WORLD_MANAGERS.values()) {
                manager.hydrateType(typeId);
            }
        }
        MetroBuilder.LOGGER.info("Registered precision object type {}", typeId);
    }

    public static boolean isTypeRegistered(Identifier typeId) {
        synchronized (TYPE_FACTORIES) {
            return TYPE_FACTORIES.containsKey(typeId);
        }
    }

    public synchronized boolean add(PrecisionObject object) {
        Objects.requireNonNull(object, "object");
        if (!isTypeRegistered(object.getTypeId())) {
            throw new IllegalStateException("Precision object type is not registered: " + object.getTypeId());
        }
        if (objects.containsKey(object.getId())) {
            return false;
        }
        objects.put(object.getId(), object);
        saveData.put(object.toNbt());
        return true;
    }

    public synchronized void save(PrecisionObject object) {
        Objects.requireNonNull(object, "object");
        PrecisionObject managed = objects.get(object.getId());
        if (managed != object) {
            throw new IllegalArgumentException("Object is not managed by this precision object manager: " + object.getId());
        }
        saveData.put(object.toNbt());
    }

    public synchronized Optional<PrecisionObject> get(UUID id) {
        return Optional.ofNullable(objects.get(id));
    }

    public synchronized <T extends PrecisionObject> Optional<T> get(UUID id, Class<T> objectClass) {
        PrecisionObject object = objects.get(id);
        return objectClass.isInstance(object) ? Optional.of(objectClass.cast(object)) : Optional.empty();
    }

    public synchronized List<PrecisionObject> getAll() {
        return List.copyOf(objects.values());
    }

    public synchronized List<PrecisionObject> getByType(Identifier typeId) {
        List<PrecisionObject> result = new ArrayList<>();
        for (PrecisionObject object : objects.values()) {
            if (object.getTypeId().equals(typeId)) {
                result.add(object);
            }
        }
        return List.copyOf(result);
    }

    public synchronized List<PrecisionObject> findWithin(Vec3d center, double radius) {
        Objects.requireNonNull(center, "center");
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException("radius must be finite and non-negative");
        }
        double radiusSquared = radius * radius;
        List<PrecisionObject> result = new ArrayList<>();
        for (PrecisionObject object : objects.values()) {
            if (object.squaredDistanceTo(center) <= radiusSquared) {
                result.add(object);
            }
        }
        result.sort(Comparator.comparingDouble(object -> object.squaredDistanceTo(center)));
        return List.copyOf(result);
    }

    public synchronized Optional<PrecisionObject> findNearest(Vec3d center, double radius) {
        List<PrecisionObject> result = findWithin(center, radius);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public synchronized boolean remove(UUID id) {
        if (objects.remove(id) == null) {
            return false;
        }
        saveData.remove(id);
        PrecisionSelectionManager.clearObject(id);
        return true;
    }

    public synchronized int size() {
        return objects.size();
    }

    public synchronized int rawStoredSize() {
        return saveData.size();
    }

    private synchronized void hydrateAllKnownTypes() {
        for (NbtCompound nbt : saveData.getAll()) {
            hydrate(nbt, null);
        }
    }

    private synchronized void hydrateType(Identifier typeId) {
        for (NbtCompound nbt : saveData.getAll()) {
            hydrate(nbt, typeId);
        }
    }

    private void hydrate(NbtCompound nbt, Identifier requiredType) {
        Optional<UUID> id = PrecisionObject.readId(nbt);
        Optional<Identifier> typeId = PrecisionObject.readTypeId(nbt);
        if (id.isEmpty() || typeId.isEmpty() || objects.containsKey(id.get())) {
            return;
        }
        if (requiredType != null && !requiredType.equals(typeId.get())) {
            return;
        }

        ObjectFactory<? extends PrecisionObject> factory;
        synchronized (TYPE_FACTORIES) {
            factory = TYPE_FACTORIES.get(typeId.get());
        }
        if (factory == null) {
            return;
        }

        try {
            PrecisionObject object = factory.create(nbt.copy());
            if (!id.get().equals(object.getId()) || !typeId.get().equals(object.getTypeId())) {
                throw new IllegalStateException("Factory returned an object with a different id or type");
            }
            objects.put(object.getId(), object);
        } catch (RuntimeException exception) {
            MetroBuilder.LOGGER.error("Could not load precision object {} of type {}", id.get(), typeId.get(), exception);
        }
    }

    @FunctionalInterface
    public interface ObjectFactory<T extends PrecisionObject> {
        T create(NbtCompound nbt);
    }
}
