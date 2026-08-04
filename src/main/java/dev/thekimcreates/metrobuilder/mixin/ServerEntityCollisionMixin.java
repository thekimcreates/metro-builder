package dev.thekimcreates.metrobuilder.mixin;

import dev.thekimcreates.metrobuilder.psd.ServerPSDCollisionShapes;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
abstract class ServerEntityCollisionMixin {
    @Unique private static final ThreadLocal<Boolean> METROBUILDER_RECALCULATING =
            ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void metrobuilder$serverPsdCollision(Entity entity, Vec3d movement, Box box,
            World world, List<VoxelShape> collisions, CallbackInfoReturnable<Vec3d> callback) {
        if (!(world instanceof ServerWorld serverWorld) || METROBUILDER_RECALCULATING.get()) return;
        final List<VoxelShape> combined = new ArrayList<>(collisions);
        ServerPSDCollisionShapes.append(serverWorld, box, movement, combined);
        if (combined.size() == collisions.size()) return;
        METROBUILDER_RECALCULATING.set(true);
        try {
            callback.setReturnValue(Entity.adjustMovementForCollisions(entity, movement, box, world, combined));
        } finally {
            METROBUILDER_RECALCULATING.remove();
        }
    }
}
