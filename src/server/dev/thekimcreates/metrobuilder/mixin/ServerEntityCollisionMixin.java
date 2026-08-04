package dev.thekimcreates.metrobuilder.mixin;

import dev.thekimcreates.metrobuilder.psd.ServerPSDCollisionShapes;
import dev.thekimcreates.metrobuilder.psd.PSDManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_265;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_1297.class)
abstract class ServerEntityCollisionMixin {
    @Unique private static final ThreadLocal<Boolean> metrobuilder$serverPsdCollision =
            ThreadLocal.withInitial(() -> false);
    @Unique private static boolean metrobuilder$loggedServerObjects;
    @Unique private static boolean metrobuilder$loggedServerShapes;

    @Inject(method = "method_20736(Lnet/minecraft/class_1297;Lnet/minecraft/class_243;Lnet/minecraft/class_238;Lnet/minecraft/class_1937;Ljava/util/List;)Lnet/minecraft/class_243;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void metrobuilder$serverPsdCollision(class_1297 entity, class_243 movement,
            class_238 entityBox, class_1937 world, List<class_265> collisions,
            CallbackInfoReturnable<class_243> callback) {
        if (!(world instanceof class_3218 serverWorld) || metrobuilder$serverPsdCollision.get()) return;
        if (!metrobuilder$loggedServerObjects) {
            metrobuilder$loggedServerObjects = true;
            System.out.println("[MetroBuilder collision diagnostic] Server movement hook active; PSD objects="
                    + PSDManager.count(serverWorld));
        }
        List<class_265> combined = new ArrayList<>(collisions);
        ServerPSDCollisionShapes.append(serverWorld, entityBox, movement, combined);
        if (combined.size() == collisions.size()) return;
        if (!metrobuilder$loggedServerShapes) {
            metrobuilder$loggedServerShapes = true;
            System.out.println("[MetroBuilder collision diagnostic] Server generated PSD shapes="
                    + (combined.size() - collisions.size()));
        }
        metrobuilder$serverPsdCollision.set(true);
        try {
            callback.setReturnValue(class_1297.method_20736(entity, movement, entityBox, world, combined));
        } finally {
            metrobuilder$serverPsdCollision.remove();
        }
    }
}
