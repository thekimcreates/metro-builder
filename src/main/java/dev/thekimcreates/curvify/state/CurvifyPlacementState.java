package dev.thekimcreates.curvify.state;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CurvifyPlacementState extends PersistentState {
    public static final String STORAGE_KEY = "curvify_placements";
    private static final String PLACEMENTS = "Placements";
    private static final Type<CurvifyPlacementState> TYPE = new Type<>(
            CurvifyPlacementState::new,
            CurvifyPlacementState::fromNbt,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final Map<String, PlacementRecord> placements = new LinkedHashMap<>();

    public CurvifyPlacementState() {
    }

    private CurvifyPlacementState(NbtCompound nbt) {
        final NbtCompound placementsNbt = nbt.getCompound(PLACEMENTS);
        placementsNbt.getKeys().forEach(railId -> {
            try {
                placements.put(railId, PlacementRecord.fromNbt(placementsNbt.getCompound(railId)));
            } catch (RuntimeException ignored) {
                // Ignore one malformed record rather than losing every placement in the dimension.
            }
        });
    }

    public Optional<PlacementRecord> get(String railId) {
        return Optional.ofNullable(placements.get(railId));
    }

    public void put(String railId, PlacementRecord record) {
        placements.put(railId, record);
        markDirty();
    }

    public Optional<PlacementRecord> remove(String railId) {
        final PlacementRecord removed = placements.remove(railId);
        if (removed != null) {
            markDirty();
        }
        return Optional.ofNullable(removed);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        final NbtCompound placementsNbt = new NbtCompound();
        placements.forEach((railId, record) -> placementsNbt.put(railId, record.toNbt()));
        nbt.put(PLACEMENTS, placementsNbt);
        return nbt;
    }

    public static CurvifyPlacementState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    private static CurvifyPlacementState fromNbt(NbtCompound nbt) {
        return new CurvifyPlacementState(nbt);
    }
}
