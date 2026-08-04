package dev.thekimcreates.metrobuilder.client.mixin;

import dev.thekimcreates.metrobuilder.client.psd.PSDCollisionShapes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_265;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(class_1297.class)
abstract class ClientEntityMoveMixin {
    @ModifyVariable(
            method = "method_5784(Lnet/minecraft/class_1313;Lnet/minecraft/class_243;)V",
            at = @At("HEAD"), argsOnly = true, remap = false)
    private class_243 metrobuilder$applyPsdCollision(class_243 movement) {
        class_1297 entity = (class_1297) (Object) this;
        class_1937 world = entity.method_37908();
        if (!world.field_9236 || movement.method_1027() == 0.0) return movement;

        class_238 box = entity.method_5829();
        List<class_265> psdShapes = new ArrayList<>();
        PSDCollisionShapes.append(box, movement, psdShapes);
        if (psdShapes.isEmpty()) return movement;
        return class_1297.method_20736(entity, movement, box, world, psdShapes);
    }
}
