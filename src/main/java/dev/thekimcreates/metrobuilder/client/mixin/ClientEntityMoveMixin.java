package dev.thekimcreates.metrobuilder.client.mixin;

import dev.thekimcreates.metrobuilder.client.psd.PSDCollisionShapes;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/** Applies precision PSD collision directly to the client entity movement path. */
@Mixin(Entity.class)
abstract class ClientEntityMoveMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3d metrobuilder$applyPsdCollision(Vec3d movement) {
        final Entity entity = (Entity) (Object) this;
        if (!entity.getWorld().isClient || movement.lengthSquared() == 0.0D) {
            return movement;
        }
        final Box box = entity.getBoundingBox();
        final List<VoxelShape> psdShapes = new ArrayList<>();
        PSDCollisionShapes.append(box, movement, psdShapes);
        return psdShapes.isEmpty()
                ? movement
                : Entity.adjustMovementForCollisions(entity, movement, box, entity.getWorld(), psdShapes);
    }
}
