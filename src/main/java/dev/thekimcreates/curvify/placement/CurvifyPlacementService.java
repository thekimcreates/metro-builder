package dev.thekimcreates.curvify.placement;

import dev.thekimcreates.curvify.state.CurvifyPlacementState;
import dev.thekimcreates.curvify.state.PlacementRecord;
import dev.thekimcreates.curvify.util.BlockStateUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.mtr.core.data.Rail;
import org.mtr.core.operation.RailsRequest;
import org.mtr.core.operation.RailsResponse;
import org.mtr.core.servlet.OperationProcessor;
import org.mtr.mod.Init;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Server-authoritative, transactional placement and removal of Curvify structures. */
public final class CurvifyPlacementService {
    private static final Identifier PSD_TOP_ID = new Identifier("mtr", "psd_top");
    private static final int UPDATE_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

    private CurvifyPlacementService() {
    }

    public static PlacementConfig getConfig(ServerWorld world, String railId) {
        return CurvifyPlacementState.get(world).get(railId)
                .map(PlacementRecord::config)
                .orElse(PlacementConfig.DEFAULT);
    }

    public static void save(ServerPlayerEntity player, String railId, PlacementConfig config) {
        if (!canEdit(player)) {
            player.sendMessage(Text.translatable("curvify.permission_denied"), true);
            return;
        }

        final ServerWorld world = player.getServerWorld();
        final Block platformBlock;
        try {
            platformBlock = validatePlatform(config.platformBlockItemId());
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Text.literal(exception.getMessage()), true);
            return;
        }

        findRail(world, railId, rail -> {
            try {
                apply(world, player, railId, rail, config, platformBlock);
                player.sendMessage(Text.translatable("curvify.saved"), true);
            } catch (PlacementException exception) {
                player.sendMessage(Text.literal(exception.getMessage()), true);
            } catch (RuntimeException exception) {
                player.sendMessage(Text.translatable("curvify.invalid_config"), true);
            }
        }, () -> player.sendMessage(Text.translatable("curvify.rail_not_found"), true));
    }

    public static void delete(ServerPlayerEntity player, String railId) {
        if (!canEdit(player)) {
            player.sendMessage(Text.translatable("curvify.permission_denied"), true);
            return;
        }

        final ServerWorld world = player.getServerWorld();
        final CurvifyPlacementState state = CurvifyPlacementState.get(world);
        final Optional<PlacementRecord> recordOptional = state.get(railId);
        if (recordOptional.isEmpty()) {
            player.sendMessage(Text.translatable("curvify.no_existing"), true);
            return;
        }

        final PlacementRecord record = recordOptional.get();
        final Map<BlockPos, BlockState> rollback = snapshot(world, record.originalStates().keySet());
        try {
            for (Map.Entry<BlockPos, BlockState> entry : record.originalStates().entrySet()) {
                final Identifier expected = record.expectedBlocks().get(entry.getKey());
                if (expected != null && expected.equals(blockId(world.getBlockState(entry.getKey())))) {
                    world.setBlockState(entry.getKey(), entry.getValue(), UPDATE_FLAGS);
                }
            }
            state.remove(railId);
            notifyNeighbors(world, record.originalStates().keySet());
            player.sendMessage(Text.translatable("curvify.deleted"), true);
        } catch (RuntimeException exception) {
            restoreSnapshot(world, rollback);
            player.sendMessage(Text.translatable("curvify.invalid_config"), true);
        }
    }

    private static void apply(
            ServerWorld world,
            ServerPlayerEntity player,
            String railId,
            Rail rail,
            PlacementConfig config,
            Block platformBlock
    ) {
        final RailPlacementPlanner.Plan plan = RailPlacementPlanner.create(rail, config);
        final CurvifyPlacementState storage = CurvifyPlacementState.get(world);
        final PlacementRecord oldRecord = storage.get(railId).orElse(null);
        final Set<BlockPos> newPositions = plan.allPositions();
        final Set<BlockPos> affectedPositions = new LinkedHashSet<>(newPositions);
        if (oldRecord != null) {
            affectedPositions.addAll(oldRecord.originalStates().keySet());
        }

        preflight(world, player, newPositions, oldRecord);
        final Map<BlockPos, BlockState> rollback = snapshot(world, affectedPositions);
        final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        for (BlockPos position : newPositions) {
            final BlockState original = oldRecord == null
                    ? rollback.get(position)
                    : oldRecord.originalStates().getOrDefault(position, rollback.get(position));
            originals.put(position, original);
        }

        try {
            if (oldRecord != null) {
                restoreExpected(world, oldRecord);
            }

            final Map<BlockPos, Identifier> expected = new LinkedHashMap<>();
            final BlockState platformState = platformBlock.getDefaultState();
            for (BlockPos position : plan.platformPositions()) {
                setChecked(world, position, platformState);
                expected.put(position, blockId(platformState));
            }

            final Block topBlock = requireBlock(PSD_TOP_ID);
            for (RailPlacementPlanner.SectionPlacement section : plan.sections()) {
                final Block psdBlock = requireBlock(section.type().blockId());
                for (int column = 0; column < section.bases().size(); column++) {
                    final BlockPos base = section.bases().get(column);
                    final String facing = section.facing().asString();
                    final String side = section.sideValue(column);
                    final BlockState lower = configure(psdBlock.getDefaultState(), facing, "lower", side);
                    final BlockState upper = configure(psdBlock.getDefaultState(), facing, "upper", side);
                    final BlockState top = BlockStateUtil.with(
                            BlockStateUtil.with(topBlock.getDefaultState(), "facing", facing),
                            "side",
                            side
                    );

                    setChecked(world, base, lower);
                    setChecked(world, base.up(), upper);
                    setChecked(world, base.up(2), top);
                    expected.put(base, blockId(lower));
                    expected.put(base.up(), blockId(upper));
                    expected.put(base.up(2), blockId(top));
                }
            }

            notifyNeighbors(world, newPositions);
            storage.put(railId, new PlacementRecord(config, originals, expected));
        } catch (RuntimeException exception) {
            restoreSnapshot(world, rollback);
            throw exception;
        }
    }

    private static void preflight(
            ServerWorld world,
            ServerPlayerEntity player,
            Set<BlockPos> newPositions,
            PlacementRecord oldRecord
    ) {
        int blocked = 0;

        if (oldRecord != null) {
            for (Map.Entry<BlockPos, Identifier> entry : oldRecord.expectedBlocks().entrySet()) {
                if (!entry.getValue().equals(blockId(world.getBlockState(entry.getKey())))) {
                    blocked++;
                }
            }
        }

        for (BlockPos position : newPositions) {
            if (!world.isInBuildLimit(position) || !world.canPlayerModifyAt(player, position)) {
                blocked++;
                continue;
            }
            if (oldRecord != null && oldRecord.expectedBlocks().containsKey(position)) {
                continue;
            }

            final BlockState existing = world.getBlockState(position);
            if (world.getBlockEntity(position) != null || (!existing.isAir() && !existing.isReplaceable())) {
                blocked++;
            }
        }

        if (blocked > 0) {
            throw new PlacementException(Text.translatable("curvify.collision", blocked).getString());
        }
    }

    private static BlockState configure(BlockState state, String facing, String half, String side) {
        return BlockStateUtil.with(
                BlockStateUtil.with(BlockStateUtil.with(state, "facing", facing), "half", half),
                "side",
                side
        );
    }

    private static void restoreExpected(ServerWorld world, PlacementRecord record) {
        for (Map.Entry<BlockPos, BlockState> entry : record.originalStates().entrySet()) {
            final Identifier expected = record.expectedBlocks().get(entry.getKey());
            if (expected != null && expected.equals(blockId(world.getBlockState(entry.getKey())))) {
                world.setBlockState(entry.getKey(), entry.getValue(), UPDATE_FLAGS);
            }
        }
    }

    private static Map<BlockPos, BlockState> snapshot(ServerWorld world, Set<BlockPos> positions) {
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>();
        positions.forEach(position -> states.put(position.toImmutable(), world.getBlockState(position)));
        return states;
    }

    private static void restoreSnapshot(ServerWorld world, Map<BlockPos, BlockState> snapshot) {
        snapshot.forEach((position, blockState) -> world.setBlockState(position, blockState, UPDATE_FLAGS));
        notifyNeighbors(world, snapshot.keySet());
    }

    private static void notifyNeighbors(ServerWorld world, Set<BlockPos> positions) {
        positions.forEach(position -> world.updateNeighbors(position, world.getBlockState(position).getBlock()));
    }

    private static void setChecked(ServerWorld world, BlockPos position, BlockState state) {
        world.setBlockState(position, state, UPDATE_FLAGS);
        if (!world.getBlockState(position).isOf(state.getBlock())) {
            throw new IllegalStateException("Could not place block at " + position.toShortString());
        }
    }

    private static Block validatePlatform(Identifier itemId) {
        if (PsdType.isPsdItem(itemId)) {
            throw new IllegalArgumentException("Platform block cannot be a PSD");
        }
        final Item item = Registries.ITEM.get(itemId);
        if (!(item instanceof BlockItem blockItem)) {
            throw new IllegalArgumentException("Platform selection must be a placeable block");
        }
        final Block block = blockItem.getBlock();
        if (block.getDefaultState().isAir() || block.getDefaultState().hasBlockEntity()) {
            throw new IllegalArgumentException("That block cannot be used as a platform");
        }
        return block;
    }

    private static Block requireBlock(Identifier id) {
        final Block block = Registries.BLOCK.get(id);
        if (block.getDefaultState().isAir()) {
            throw new IllegalArgumentException("Required MTR block is missing: " + id);
        }
        return block;
    }

    private static Identifier blockId(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock());
    }

    private static boolean canEdit(ServerPlayerEntity player) {
        return player.isCreative() || player.hasPermissionLevel(2);
    }

    private static void findRail(ServerWorld world, String railId, java.util.function.Consumer<Rail> found, Runnable missing) {
        Init.sendMessageC2S(
                OperationProcessor.RAILS,
                new org.mtr.mapping.holder.MinecraftServer(world.getServer()),
                new org.mtr.mapping.holder.World(world),
                new RailsRequest().addRailId(railId),
                response -> {
                    final RailsResponse railsResponse = (RailsResponse) response;
                    if (railsResponse.getRails().isEmpty()) {
                        missing.run();
                    } else {
                        found.accept(railsResponse.getRails().get(0));
                    }
                },
                RailsResponse.class
        );
    }

    private static final class PlacementException extends RuntimeException {
        private PlacementException(String message) {
            super(message);
        }
    }
}
