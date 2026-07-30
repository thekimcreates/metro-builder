package dev.metrobuilder.item;

import dev.metrobuilder.network.MetroBuilderNetworking;
import dev.metrobuilder.platform.PlatformDesignManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PlatformBuilderItem extends Item {
    public PlatformBuilderItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public boolean canMine(net.minecraft.block.BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        if (!world.isClient && miner instanceof ServerPlayerEntity player) {
            PlatformDesignManager.Session session = PlatformDesignManager.get(player);
            if (!session.openedDesigner()) {
                player.sendMessage(Text.literal("Right click the air to open the menu"), false);
                return false;
            }
            BlockPos platformPos = pos.up();
            session.setPosition1(platformPos);
            player.sendMessage(Text.literal("Platform position 1 set: " + format(platformPos)), true);
        }
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return ActionResult.PASS;

        PlatformDesignManager.Session session = PlatformDesignManager.get(player);
        if (!session.openedDesigner()) {
            player.sendMessage(Text.literal("Right click the air to open the menu"), false);
            return ActionResult.CONSUME;
        }
        if (session.position1() == null) {
            player.sendMessage(Text.literal("Left click a block to set position 1"), true);
            return ActionResult.CONSUME;
        }

        BlockPos end = context.getBlockPos().up();
        int dx = Math.abs(end.getX() - session.position1().getX());
        int dz = Math.abs(end.getZ() - session.position1().getZ());
        int length = Math.max(dx, dz) + 1;
        if (length > PlatformDesignManager.MAX_LENGTH) {
            player.sendMessage(Text.literal("Platform is too long. Maximum length: " + PlatformDesignManager.MAX_LENGTH), true);
            return ActionResult.CONSUME;
        }

        int placed = PlatformDesignManager.generate(player.getWorld(), session.position1(), end, session.rows());
        player.sendMessage(Text.literal("Built platform: " + length + " × " + session.rows().size() + " (" + placed + " blocks)"), true);
        session.clearPosition1();
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            PlatformDesignManager.Session session = PlatformDesignManager.get(player);
            session.markDesignerOpened();
            PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
            buf.writeVarInt(session.rows().size());
            for (String row : session.rows()) buf.writeString(row);
            ServerPlayNetworking.send(player, MetroBuilderNetworking.OPEN_PLATFORM_BUILDER, buf);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    private static String format(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
