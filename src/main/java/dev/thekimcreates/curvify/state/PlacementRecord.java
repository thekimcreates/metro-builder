package dev.thekimcreates.curvify.state;

import dev.thekimcreates.curvify.placement.PlacementConfig;
import dev.thekimcreates.curvify.placement.PsdType;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PlacementRecord(
        PlacementConfig config,
        Map<BlockPos, BlockState> originalStates,
        Map<BlockPos, Identifier> expectedBlocks
) {
    private static final String CONFIG = "Config";
    private static final String PATTERN = "Pattern";
    private static final String PLATFORM = "Platform";
    private static final String START_FAR = "StartFar";
    private static final String LEFT_SIDE = "LeftSide";
    private static final String ORIGINALS = "Originals";
    private static final String EXPECTED = "Expected";
    private static final String POS = "Pos";
    private static final String STATE = "State";
    private static final String BLOCK = "Block";

    public PlacementRecord {
        originalStates = Map.copyOf(originalStates);
        expectedBlocks = Map.copyOf(expectedBlocks);
    }

    public NbtCompound toNbt() {
        final NbtCompound root = new NbtCompound();
        final NbtCompound configNbt = new NbtCompound();
        final NbtList patternNbt = new NbtList();
        config.pattern().forEach(type -> patternNbt.add(NbtString.of(type.itemId().toString())));
        configNbt.put(PATTERN, patternNbt);
        configNbt.putString(PLATFORM, config.platformBlockItemId().toString());
        configNbt.putBoolean(START_FAR, config.startFar());
        configNbt.putBoolean(LEFT_SIDE, config.leftSide());
        root.put(CONFIG, configNbt);

        final NbtList originalsNbt = new NbtList();
        originalStates.forEach((pos, state) -> {
            final NbtCompound entry = new NbtCompound();
            entry.putLong(POS, pos.asLong());
            entry.put(STATE, NbtHelper.fromBlockState(state));
            originalsNbt.add(entry);
        });
        root.put(ORIGINALS, originalsNbt);

        final NbtList expectedNbt = new NbtList();
        expectedBlocks.forEach((pos, blockId) -> {
            final NbtCompound entry = new NbtCompound();
            entry.putLong(POS, pos.asLong());
            entry.putString(BLOCK, blockId.toString());
            expectedNbt.add(entry);
        });
        root.put(EXPECTED, expectedNbt);
        return root;
    }

    public static PlacementRecord fromNbt(NbtCompound root) {
        final NbtCompound configNbt = root.getCompound(CONFIG);
        final NbtList patternNbt = configNbt.getList(PATTERN, NbtElement.STRING_TYPE);
        final List<PsdType> pattern = new ArrayList<>();
        for (int index = 0; index < patternNbt.size(); index++) {
            PsdType.fromItemId(new Identifier(patternNbt.getString(index))).ifPresent(pattern::add);
        }
        final PlacementConfig config = new PlacementConfig(
                pattern.isEmpty() ? PlacementConfig.DEFAULT.pattern() : pattern,
                new Identifier(configNbt.getString(PLATFORM)),
                configNbt.getBoolean(START_FAR),
                configNbt.getBoolean(LEFT_SIDE)
        );

        final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        final NbtList originalsNbt = root.getList(ORIGINALS, NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < originalsNbt.size(); index++) {
            final NbtCompound entry = originalsNbt.getCompound(index);
            originals.put(
                    BlockPos.fromLong(entry.getLong(POS)),
                    NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), entry.getCompound(STATE))
            );
        }

        final Map<BlockPos, Identifier> expected = new LinkedHashMap<>();
        final NbtList expectedNbt = root.getList(EXPECTED, NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < expectedNbt.size(); index++) {
            final NbtCompound entry = expectedNbt.getCompound(index);
            expected.put(BlockPos.fromLong(entry.getLong(POS)), new Identifier(entry.getString(BLOCK)));
        }

        return new PlacementRecord(config, originals, expected);
    }
}
