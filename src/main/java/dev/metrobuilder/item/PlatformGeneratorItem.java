package dev.metrobuilder.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PlatformGeneratorItem extends Item {
    private static final String START_X = "MetroBuilderPlatformStartX";
    private static final String START_Y = "MetroBuilderPlatformStartY";
    private static final String START_Z = "MetroBuilderPlatformStartZ";
    private static final String HAS_START = "MetroBuilderPlatformHasStart";
    private static final String WIDTH = "MetroBuilderPlatformWidth";
    private static final int[] WIDTHS = {3, 5, 7, 9};
    private static final int MAX_LENGTH = 256;

    public PlatformGeneratorItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        if (!world.isClient && miner instanceof ServerPlayerEntity player) {
            ItemStack stack = player.getMainHandStack();
            BlockPos start = pos.up();
            stack.getOrCreateNbt().putInt(START_X, start.getX());
            stack.getOrCreateNbt().putInt(START_Y, start.getY());
            stack.getOrCreateNbt().putInt(START_Z, start.getZ());
            stack.getOrCreateNbt().putBoolean(HAS_START, true);
            player.sendMessage(Text.literal("Platform start set: " + format(start)), true);
        }
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return ActionResult.PASS;

        ItemStack stack = context.getStack();
        if (player.isSneaking()) {
            clearStart(stack);
            player.sendMessage(Text.literal("Platform selection cleared"), true);
            return ActionResult.CONSUME;
        }

        if (!stack.getOrCreateNbt().getBoolean(HAS_START)) {
            player.sendMessage(Text.literal("Left-click a block to set the platform start"), true);
            return ActionResult.FAIL;
        }

        BlockPos start = readStart(stack);
        BlockPos clickedEnd = context.getBlockPos().up();
        int dx = clickedEnd.getX() - start.getX();
        int dz = clickedEnd.getZ() - start.getZ();
        boolean alongX = Math.abs(dx) >= Math.abs(dz);
        BlockPos end = alongX
                ? new BlockPos(clickedEnd.getX(), start.getY(), start.getZ())
                : new BlockPos(start.getX(), start.getY(), clickedEnd.getZ());

        int length = alongX ? Math.abs(end.getX() - start.getX()) + 1 : Math.abs(end.getZ() - start.getZ()) + 1;
        if (length > MAX_LENGTH) {
            player.sendMessage(Text.literal("Platform is too long. Maximum length: " + MAX_LENGTH + " blocks"), true);
            return ActionResult.FAIL;
        }

        int width = getWidth(stack);
        int placed = generatePlatform(player.getWorld(), start, end, width, alongX);
        clearStart(stack);
        player.sendMessage(Text.literal("Generated " + length + " × " + width + " platform (" + placed + " blocks)"), true);
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isSneaking()) return TypedActionResult.pass(stack);

        if (!world.isClient) {
            int current = getWidth(stack);
            int next = WIDTHS[0];
            for (int i = 0; i < WIDTHS.length; i++) {
                if (WIDTHS[i] == current) {
                    next = WIDTHS[(i + 1) % WIDTHS.length];
                    break;
                }
            }
            stack.getOrCreateNbt().putInt(WIDTH, next);
            user.sendMessage(Text.literal("Platform width: " + next + " blocks"), true);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    private static int generatePlatform(World world, BlockPos start, BlockPos end, int width, boolean alongX) {
        int minAxis = alongX ? Math.min(start.getX(), end.getX()) : Math.min(start.getZ(), end.getZ());
        int maxAxis = alongX ? Math.max(start.getX(), end.getX()) : Math.max(start.getZ(), end.getZ());
        int half = width / 2;
        int placed = 0;

        for (int axis = minAxis; axis <= maxAxis; axis++) {
            for (int offset = -half; offset <= half; offset++) {
                BlockPos pos = alongX
                        ? new BlockPos(axis, start.getY(), start.getZ() + offset)
                        : new BlockPos(start.getX() + offset, start.getY(), axis);

                BlockState state;
                if (Math.abs(offset) == half) {
                    state = Blocks.YELLOW_CONCRETE.getDefaultState();
                } else if (Math.abs(offset) == half - 1) {
                    state = Blocks.SMOOTH_STONE.getDefaultState();
                } else {
                    state = Blocks.STONE_BRICKS.getDefaultState();
                }

                if (world.setBlockState(pos, state, Block.NOTIFY_ALL)) placed++;
            }
        }
        return placed;
    }

    public static int getWidth(ItemStack stack) {
        int width = stack.getOrCreateNbt().getInt(WIDTH);
        if (width == 0) {
            width = 5;
            stack.getOrCreateNbt().putInt(WIDTH, width);
        }
        return width;
    }

    private static BlockPos readStart(ItemStack stack) {
        return new BlockPos(
                stack.getOrCreateNbt().getInt(START_X),
                stack.getOrCreateNbt().getInt(START_Y),
                stack.getOrCreateNbt().getInt(START_Z)
        );
    }

    private static void clearStart(ItemStack stack) {
        stack.getOrCreateNbt().putBoolean(HAS_START, false);
    }

    private static String format(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
