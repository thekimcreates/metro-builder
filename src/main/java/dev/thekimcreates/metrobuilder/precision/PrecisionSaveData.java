package dev.thekimcreates.metrobuilder.precision;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.Objects;

/** Persistent precision-object data for one server dimension. */
public final class PrecisionSaveData extends PersistentState {
    public static final String STORAGE_KEY = "metrobuilder_precision";

    private static final Type<PrecisionSaveData> TYPE = new Type<>(
            PrecisionSaveData::new,
            PrecisionSaveData::fromNbt,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final PrecisionManager manager;

    public PrecisionSaveData() {
        manager = new PrecisionManager(this::markDirty);
    }

    private PrecisionSaveData(NbtCompound nbt) {
        manager = PrecisionManager.fromNbt(nbt, this::markDirty);
    }

    public PrecisionManager manager() {
        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return manager.writeNbt(nbt);
    }

    public static PrecisionSaveData get(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        return world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    private static PrecisionSaveData fromNbt(NbtCompound nbt) {
        return new PrecisionSaveData(nbt);
    }
}
