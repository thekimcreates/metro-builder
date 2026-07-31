package dev.metrobuilder.precision;

import dev.metrobuilder.MetroBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-dimension persistent storage for precision objects.
 *
 * <p>Entries remain raw NBT here so data belonging to an unavailable or newer
 * object type is preserved instead of being discarded.</p>
 */
public final class PrecisionSaveData extends PersistentState {
    private static final String STATE_ID = MetroBuilder.MOD_ID + "_precision_objects";
    private static final String KEY_OBJECTS = "objects";

    private static final Type<PrecisionSaveData> TYPE = new Type<>(
            PrecisionSaveData::new,
            PrecisionSaveData::fromNbt,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<UUID, NbtCompound> entries = new LinkedHashMap<>();

    public PrecisionSaveData() {
    }

    public static PrecisionSaveData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public static PrecisionSaveData fromNbt(NbtCompound nbt) {
        PrecisionSaveData data = new PrecisionSaveData();
        NbtList list = nbt.getList(KEY_OBJECTS, NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < list.size(); index++) {
            NbtCompound entry = list.getCompound(index);
            Optional<UUID> id = PrecisionObject.readId(entry);
            if (id.isPresent()) {
                data.entries.put(id.get(), entry.copy());
            } else {
                MetroBuilder.LOGGER.warn("Skipped a precision object with an invalid or missing UUID while loading");
            }
        }
        return data;
    }

    @Override
    public synchronized NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (NbtCompound entry : entries.values()) {
            list.add(entry.copy());
        }
        nbt.put(KEY_OBJECTS, list);
        return nbt;
    }

    public synchronized Optional<NbtCompound> get(UUID id) {
        NbtCompound entry = entries.get(id);
        return entry == null ? Optional.empty() : Optional.of(entry.copy());
    }

    public synchronized List<NbtCompound> getAll() {
        List<NbtCompound> copy = new ArrayList<>(entries.size());
        for (NbtCompound entry : entries.values()) {
            copy.add(entry.copy());
        }
        return List.copyOf(copy);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void put(NbtCompound objectNbt) {
        UUID id = PrecisionObject.readId(objectNbt)
                .orElseThrow(() -> new IllegalArgumentException("Precision object NBT has no valid id"));
        entries.put(id, objectNbt.copy());
        markDirty();
    }

    public synchronized boolean remove(UUID id) {
        if (entries.remove(id) == null) {
            return false;
        }
        markDirty();
        return true;
    }

    public synchronized void clear() {
        if (entries.isEmpty()) {
            return;
        }
        entries.clear();
        markDirty();
    }
}
