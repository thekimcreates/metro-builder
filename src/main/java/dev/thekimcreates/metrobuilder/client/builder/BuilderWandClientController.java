package dev.thekimcreates.metrobuilder.client.builder;

import dev.thekimcreates.metrobuilder.client.network.ClientPrecisionState;
import dev.thekimcreates.metrobuilder.client.network.PrecisionClientNetworking;
import dev.thekimcreates.metrobuilder.client.psd.ClientPSDObject;
import dev.thekimcreates.metrobuilder.client.screen.PSDPropertiesScreen;
import dev.thekimcreates.metrobuilder.item.MetroBuilderItems;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Client input controller for the single, mode-free Builder Wand. */
public final class BuilderWandClientController {
    private static final double RAYCAST_DISTANCE = 8.0D;
    private static final double AIR_PLACEMENT_DISTANCE = 3.0D;
    private static final double NORMAL_MOVE_STEP = 0.01D;
    private static final double FINE_MOVE_STEP = 0.001D;
    private static final UUID PENDING_PREVIEW_ID = new UUID(0L, 1L);

    private static KeyBinding rotateLeft;
    private static KeyBinding rotateRight;
    private static KeyBinding moveForward;
    private static KeyBinding moveBackward;
    private static KeyBinding moveLeft;
    private static KeyBinding moveRight;
    private static KeyBinding moveUp;
    private static KeyBinding moveDown;
    private static KeyBinding properties;

    private static PendingPSD pending;
    private static PSDTemplate lastPlacedTemplate;
    private static boolean initialized;

    private BuilderWandClientController() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        rotateLeft = register("rotate_left", GLFW.GLFW_KEY_LEFT_BRACKET);
        rotateRight = register("rotate_right", GLFW.GLFW_KEY_RIGHT_BRACKET);
        moveForward = register("move_forward", GLFW.GLFW_KEY_UP);
        moveBackward = register("move_backward", GLFW.GLFW_KEY_DOWN);
        moveLeft = register("move_left", GLFW.GLFW_KEY_LEFT);
        moveRight = register("move_right", GLFW.GLFW_KEY_RIGHT);
        moveUp = register("move_up", GLFW.GLFW_KEY_PAGE_UP);
        moveDown = register("move_down", GLFW.GLFW_KEY_PAGE_DOWN);
        properties = register("properties", GLFW.GLFW_KEY_P);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient || !isWand(player.getStackInHand(hand))) {
                return ActionResult.PASS;
            }
            handleRightClick(hitResult.getPos());
            return ActionResult.SUCCESS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            final ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient || !isWand(stack)) {
                return TypedActionResult.pass(stack);
            }

            final HitResult hitResult = player.raycast(RAYCAST_DISTANCE, 1.0F, false);
            final Vec3d position = hitResult.getType() == HitResult.Type.MISS
                    ? player.getEyePos().add(player.getRotationVec(1.0F).multiply(AIR_PLACEMENT_DISTANCE))
                    : hitResult.getPos();
            handleRightClick(position);
            return TypedActionResult.success(stack, true);
        });

        ClientTickEvents.END_CLIENT_TICK.register(BuilderWandClientController::tick);
    }

    public static void reset() {
        pending = null;
        lastPlacedTemplate = null;
    }

    public static Optional<ClientPSDObject> pendingPreview() {
        if (pending == null) {
            return Optional.empty();
        }
        return Optional.of(new ClientPSDObject(
                PENDING_PREVIEW_ID,
                pending.transform,
                pending.packId,
                0.0D
        ));
    }

    private static void handleRightClick(Vec3d clickedPosition) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (!canEdit(client)) {
            return;
        }

        if (pending != null) {
            // Match the original precision Block Display workflow: the second
            // right-click confirms the preview exactly where it currently is.
            // It must not jump to the position targeted by the confirmation click.
            final PendingPSD confirmed = pending;
            lastPlacedTemplate = new PSDTemplate(
                    confirmed.packId,
                    confirmed.transform.pitch(),
                    confirmed.transform.yaw(),
                    confirmed.transform.roll(),
                    confirmed.transform.scaleX(),
                    confirmed.transform.scaleY(),
                    confirmed.transform.scaleZ()
            );
            PrecisionClientNetworking.placePsd(confirmed.packId, confirmed.transform);
            pending = null;
            return;
        }

        final Optional<ClientPSDObject> targeted = findTargetedPsd(client);
        if (targeted.isPresent()) {
            selectAndOpenProperties(client, targeted.get());
            return;
        }

        final PSDTemplate template = lastPlacedTemplate == null
                ? PSDTemplate.defaultFor(client.player.getYaw())
                : lastPlacedTemplate;
        pending = new PendingPSD(
                template.packId,
                new PrecisionTransform(
                        clickedPosition.x,
                        clickedPosition.y,
                        clickedPosition.z,
                        template.pitch,
                        template.yaw,
                        template.roll,
                        template.scaleX,
                        template.scaleY,
                        template.scaleZ
                )
        );
        PrecisionClientNetworking.requestSelectionClear();
        client.player.sendMessage(
                Text.literal("Pending PSD created — right-click another spot to place"),
                true
        );
    }

    private static void tick(MinecraftClient client) {
        if (!canEdit(client) || client.currentScreen != null) {
            drainUnusedKeys();
            return;
        }

        while (rotateLeft.wasPressed()) {
            rotate(-1.0F);
        }
        while (rotateRight.wasPressed()) {
            rotate(1.0F);
        }

        final double step = client.options.sneakKey.isPressed()
                ? FINE_MOVE_STEP
                : NORMAL_MOVE_STEP;
        while (moveLeft.wasPressed()) {
            moveLocal(-step, 0.0D, 0.0D);
        }
        while (moveRight.wasPressed()) {
            moveLocal(step, 0.0D, 0.0D);
        }
        while (moveForward.wasPressed()) {
            moveLocal(0.0D, 0.0D, -step);
        }
        while (moveBackward.wasPressed()) {
            moveLocal(0.0D, 0.0D, step);
        }
        while (moveUp.wasPressed()) {
            moveLocal(0.0D, step, 0.0D);
        }
        while (moveDown.wasPressed()) {
            moveLocal(0.0D, -step, 0.0D);
        }
        while (properties.wasPressed()) {
            openPropertiesForCurrent(client);
        }
    }

    private static void rotate(float degrees) {
        transformCurrent(transform -> transform.withYaw(normalizeYaw(transform.yaw() + degrees)));
    }

    private static void moveLocal(double localX, double y, double localZ) {
        transformCurrent(transform -> {
            final double radians = Math.toRadians(transform.yaw());
            final double deltaX = localX * Math.cos(radians) - localZ * Math.sin(radians);
            final double deltaZ = localX * Math.sin(radians) + localZ * Math.cos(radians);
            return transform.translated(deltaX, y, deltaZ);
        });
    }

    private static void transformCurrent(TransformOperation operation) {
        if (pending != null) {
            pending = pending.withTransform(operation.apply(pending.transform));
            return;
        }

        final Optional<UUID> selectedId = ClientPrecisionState.selectedObjectId();
        if (selectedId.isEmpty()) {
            return;
        }
        ClientPrecisionState.findPsd(selectedId.get()).ifPresent(psd -> {
            final PrecisionTransform updated = operation.apply(psd.transform());
            lastPlacedTemplate = PSDTemplate.from(psd.packId(), updated);
            ClientPrecisionState.updatePsdTransform(psd.id(), updated);
            PrecisionClientNetworking.updatePsdTransform(psd.id(), updated);
        });
    }

    private static void openPropertiesForCurrent(MinecraftClient client) {
        if (pending != null) {
            final PendingPSD openedPending = pending;
            client.setScreen(new PSDPropertiesScreen(
                    "Pending PSD",
                    openedPending.packId,
                    openedPending.transform,
                    updated -> pending = pending == null
                            ? openedPending.withTransform(updated)
                            : pending.withTransform(updated)
            ));
            return;
        }

        ClientPrecisionState.selectedObjectId()
                .flatMap(ClientPrecisionState::findPsd)
                .ifPresent(psd -> openProperties(client, psd));
    }

    private static void selectAndOpenProperties(MinecraftClient client, ClientPSDObject psd) {
        ClientPrecisionState.dimensionId().ifPresent(dimensionId ->
                ClientPrecisionState.applySelection(dimensionId, psd.id()));
        PrecisionClientNetworking.requestSelection(psd.id());
        openProperties(client, psd);
    }

    private static void openProperties(MinecraftClient client, ClientPSDObject psd) {
        client.setScreen(new PSDPropertiesScreen(
                "PSD Properties",
                psd.packId(),
                psd.transform(),
                updated -> {
                    lastPlacedTemplate = PSDTemplate.from(psd.packId(), updated);
                    ClientPrecisionState.updatePsdTransform(psd.id(), updated);
                    PrecisionClientNetworking.updatePsdTransform(psd.id(), updated);
                },
                () -> {
                    // Keep the deleted PSD's properties as the template for the
                    // next newly-created pending PSD.
                    lastPlacedTemplate = PSDTemplate.from(psd.packId(), psd.transform());
                    ClientPrecisionState.removePsd(psd.id());
                    PrecisionClientNetworking.deletePsd(psd.id());
                }
        ));
    }

    private static Optional<ClientPSDObject> findTargetedPsd(MinecraftClient client) {
        if (client.player == null) {
            return Optional.empty();
        }

        final Vec3d rayStart = client.player.getCameraPosVec(1.0F);
        final Vec3d rayEnd = rayStart.add(client.player.getRotationVec(1.0F).multiply(RAYCAST_DISTANCE));

        return ClientPrecisionState.psds().stream()
                .map(psd -> new Target(psd, intersectionDistance(psd.transform(), rayStart, rayEnd)))
                .filter(target -> Double.isFinite(target.distance))
                .min(Comparator.comparingDouble(Target::distance))
                .map(Target::psd);
    }

    private static double intersectionDistance(
            PrecisionTransform transform,
            Vec3d worldStart,
            Vec3d worldEnd
    ) {
        final Vec3d localStart = worldToLocal(transform, worldStart);
        final Vec3d localEnd = worldToLocal(transform, worldEnd);
        final Vec3d direction = localEnd.subtract(localStart);

        double minimum = 0.0D;
        double maximum = 1.0D;
        final double[] start = {localStart.x, localStart.y, localStart.z};
        final double[] delta = {direction.x, direction.y, direction.z};
        final double[] boundsMin = {-1.0D, 0.0D, -0.20D};
        final double[] boundsMax = {1.0D, 3.0D, 0.20D};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(delta[axis]) < 1.0E-8D) {
                if (start[axis] < boundsMin[axis] || start[axis] > boundsMax[axis]) {
                    return Double.NaN;
                }
                continue;
            }

            double near = (boundsMin[axis] - start[axis]) / delta[axis];
            double far = (boundsMax[axis] - start[axis]) / delta[axis];
            if (near > far) {
                final double swap = near;
                near = far;
                far = swap;
            }
            minimum = Math.max(minimum, near);
            maximum = Math.min(maximum, far);
            if (minimum > maximum) {
                return Double.NaN;
            }
        }

        return worldStart.distanceTo(worldStart.lerp(worldEnd, minimum));
    }

    private static Vec3d worldToLocal(PrecisionTransform transform, Vec3d worldPoint) {
        final double worldX = worldPoint.x - transform.x();
        final double worldY = worldPoint.y - transform.y();
        final double worldZ = worldPoint.z - transform.z();
        final double radians = Math.toRadians(transform.yaw());
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);

        final double localX = (worldX * cos + worldZ * sin) / transform.scaleX();
        final double localZ = (-worldX * sin + worldZ * cos) / transform.scaleZ();
        return new Vec3d(localX, worldY / transform.scaleY(), localZ);
    }

    private static boolean canEdit(MinecraftClient client) {
        return client.player != null
                && client.world != null
                && client.player.isCreative()
                && (isWand(client.player.getMainHandStack())
                || isWand(client.player.getOffHandStack()));
    }

    private static boolean isWand(ItemStack stack) {
        return stack.isOf(MetroBuilderItems.BUILDER_WAND);
    }

    private static KeyBinding register(String name, int keyCode) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.metrobuilder." + name,
                InputUtil.Type.KEYSYM,
                keyCode,
                "key.category.metrobuilder"
        ));
    }

    private static void drainUnusedKeys() {
        if (rotateLeft == null) {
            return;
        }
        while (rotateLeft.wasPressed()) { }
        while (rotateRight.wasPressed()) { }
        while (moveForward.wasPressed()) { }
        while (moveBackward.wasPressed()) { }
        while (moveLeft.wasPressed()) { }
        while (moveRight.wasPressed()) { }
        while (moveUp.wasPressed()) { }
        while (moveDown.wasPressed()) { }
        while (properties.wasPressed()) { }
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    private record PendingPSD(net.minecraft.util.Identifier packId, PrecisionTransform transform) {
        private PendingPSD withTransform(PrecisionTransform updatedTransform) {
            return new PendingPSD(packId, updatedTransform);
        }
    }

    private record PSDTemplate(
            net.minecraft.util.Identifier packId,
            float pitch,
            float yaw,
            float roll,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
        private static PSDTemplate defaultFor(float yaw) {
            return new PSDTemplate(
                    PSDObject.DEFAULT_PACK_ID,
                    0.0F,
                    normalizeYaw(yaw),
                    0.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }

        private static PSDTemplate from(
                net.minecraft.util.Identifier packId,
                PrecisionTransform transform
        ) {
            return new PSDTemplate(
                    packId,
                    transform.pitch(),
                    transform.yaw(),
                    transform.roll(),
                    transform.scaleX(),
                    transform.scaleY(),
                    transform.scaleZ()
            );
        }
    }

    private record Target(ClientPSDObject psd, double distance) {
    }

    @FunctionalInterface
    private interface TransformOperation {
        PrecisionTransform apply(PrecisionTransform transform);
    }
}
