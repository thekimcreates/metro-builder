package dev.thekimcreates.metrobuilder.client.mixin;

import dev.thekimcreates.metrobuilder.client.psd.PSDCollisionShapes;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Adds precision PSD collision to Minecraft's normal entity movement resolver. */
@Mixin(Entity.class)
abstract class EntityCollisionMixin {
    @Inject(
            method = "adjustMovementForCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Lnet/minecraft/world/World;Ljava/util/List;)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD")
    )
    private static void metrobuilder$appendPsdCollision(
            Entity entity,
            Vec3d movement,
            Box entityBox,
            World world,
            List<VoxelShape> collisions,
            CallbackInfoReturnable<Vec3d> callback
    ) {
        if (world.isClient) {
            PSDCollisionShapes.append(entityBox, movement, collisions);
        }
    }
}
