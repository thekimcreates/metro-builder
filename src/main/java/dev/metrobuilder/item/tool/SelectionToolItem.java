package dev.metrobuilder.item.tool;
import dev.metrobuilder.selection.SelectionManager;
import net.minecraft.block.BlockState; import net.minecraft.entity.player.PlayerEntity; import net.minecraft.item.*; import net.minecraft.server.network.ServerPlayerEntity; import net.minecraft.util.ActionResult; import net.minecraft.util.math.BlockPos; import net.minecraft.world.World;
public final class SelectionToolItem extends Item {
 public SelectionToolItem(Settings s){super(s.maxCount(1));}
 @Override public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner){ if(!world.isClient && miner instanceof ServerPlayerEntity p){ if(p.isSneaking()) SelectionManager.clear(p); else SelectionManager.setFirst(p,pos);} return false; }
 @Override public ActionResult useOnBlock(ItemUsageContext c){ if(c.getWorld().isClient) return ActionResult.SUCCESS; if(c.getPlayer() instanceof ServerPlayerEntity p){ if(p.isSneaking()) SelectionManager.clear(p); else SelectionManager.setSecond(p,c.getBlockPos()); return ActionResult.CONSUME;} return ActionResult.PASS; }
}
