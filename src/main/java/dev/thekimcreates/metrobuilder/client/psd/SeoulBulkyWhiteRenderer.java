package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.client.model.obj.ObjMesh;
import dev.thekimcreates.metrobuilder.client.model.obj.ObjMeshCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Renders the five-block Seoul Metro Bulky White PSD from authored OBJ meshes. */
final class SeoulBulkyWhiteRenderer {
    static final double HALF_WIDTH = 2.5D;
    static final double HALF_DEPTH = 0.21D;

    private static final String MODEL_ROOT = "models/psd/seoul_bulky_white/";

    private static final Identifier HEADER_MODEL = model("header");
    private static final Identifier HEADER_WINGS_MODEL = model("header_wings");
    private static final Identifier SIDE_WHITE_MODEL = model("side_white");
    private static final Identifier SIDE_DARK_MODEL = model("side_dark");
    private static final Identifier SIDE_GLASS_MODEL = model("side_glass");
    private static final Identifier LEFT_DOOR_WHITE_MODEL = model("left_door_white");
    private static final Identifier LEFT_DOOR_DARK_MODEL = model("left_door_dark");
    private static final Identifier LEFT_DOOR_GLASS_MODEL = model("left_door_glass");
    private static final Identifier RIGHT_DOOR_WHITE_MODEL = model("right_door_white");
    private static final Identifier RIGHT_DOOR_DARK_MODEL = model("right_door_dark");
    private static final Identifier RIGHT_DOOR_GLASS_MODEL = model("right_door_glass");

    private static final Identifier WHITE_METAL = texture("white_metal");
    private static final Identifier DARK_FRAME = texture("dark_frame");
    private static final Identifier GLASS = texture("glass");
    private static final Identifier HEADER_SIGN = texture("header");
    private static final Identifier CAUTION = texture("caution");
    private static final Identifier INDICATOR_ON = texture("indicator_on");
    private static final Identifier INDICATOR_OFF = texture("indicator_off");
    private static final float DOOR_OVERLAY_Z = 0.002F;
    private static final Map<UUID, DoorMotionMemory> DOOR_MOTION = new HashMap<>();

    private SeoulBulkyWhiteRenderer() {
    }

    static void render(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            ClientPSDObject psd,
            int light
    ) {
        // Match MTR's built-in renderer: use its live door value directly and
        // translate each leaf by exactly that value, with no custom timing.
        final double effectiveDoorValue = MtrTrainDoorLink.findDoorValue(client, psd)
                .orElse(psd.doorValue());
        final float doorOffset = (float) clampDoorValue(effectiveDoorValue);

        renderMesh(matrices, consumers, HEADER_MODEL, WHITE_METAL, light, false);
        renderHeaderSign(matrices, consumers, light);

        // Rear sliding rail: opening leaves travel into the clear side pockets.
        matrices.push();
        matrices.translate(-doorOffset, 0.0F, 0.0F);
        renderDoorMeshes(
                matrices,
                consumers,
                LEFT_DOOR_WHITE_MODEL,
                LEFT_DOOR_DARK_MODEL,
                LEFT_DOOR_GLASS_MODEL,
                light
        );
        renderCaution(matrices, consumers, -0.5F, light);
        matrices.pop();

        matrices.push();
        matrices.translate(doorOffset, 0.0F, 0.0F);
        renderDoorMeshes(
                matrices,
                consumers,
                RIGHT_DOOR_WHITE_MODEL,
                RIGHT_DOOR_DARK_MODEL,
                RIGHT_DOOR_GLASS_MODEL,
                light
        );
        renderCaution(matrices, consumers, 0.5F, light);
        if (!psd.displayProperties().platformNumber().isBlank()) {
            renderDoorNumberLabel(
                    client,
                    matrices,
                    consumers,
                    psd.displayProperties().platformNumber(),
                    0.5F,
                    light
            );
        }
        matrices.pop();

        // Fixed panels are a physically separate front layer.
        renderSideAssembly(matrices, consumers, light);

        renderIndicator(matrices, consumers, indicatorLit(psd.id(), effectiveDoorValue), light);
    }

    /** Adds the standard clean 1.5-block glass wings to a native two-door pack. */
    static void renderCompanionGlass(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light
    ) {
        renderMesh(matrices, consumers, HEADER_WINGS_MODEL, WHITE_METAL, light, false);
        renderSideAssembly(matrices, consumers, light);
    }

    private static void renderSideAssembly(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light
    ) {
        renderMesh(matrices, consumers, SIDE_WHITE_MODEL, WHITE_METAL, light, false);
        renderMesh(matrices, consumers, SIDE_DARK_MODEL, DARK_FRAME, light, false);
        renderMesh(matrices, consumers, SIDE_GLASS_MODEL, GLASS, light, true);
    }

    private static void renderDoorMeshes(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier whiteModel,
            Identifier darkModel,
            Identifier glassModel,
            int light
    ) {
        renderMesh(matrices, consumers, whiteModel, WHITE_METAL, light, false);
        renderMesh(matrices, consumers, darkModel, DARK_FRAME, light, false);
        renderMesh(matrices, consumers, glassModel, GLASS, light, true);
    }

    private static void renderHeaderSign(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light
    ) {
        renderFrontQuad(
                matrices,
                consumers,
                HEADER_SIGN,
                -2.5F,
                2.1F,
                2.5F,
                3.0F,
                0.181F,
                light,
                false
        );
    }

    private static void renderCaution(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            float centerX,
            int light
    ) {
        renderFrontQuad(
                matrices,
                consumers,
                CAUTION,
                centerX - 0.30F,
                1.17F,
                centerX + 0.30F,
                1.55F,
                DOOR_OVERLAY_Z,
                light,
                false
        );
    }

    private static void renderDoorNumberLabel(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            String number,
            float centerX,
            int light
    ) {
        renderFrontQuad(
                matrices,
                consumers,
                DARK_FRAME,
                centerX - 0.30F,
                0.94F,
                centerX + 0.30F,
                1.10F,
                DOOR_OVERLAY_Z,
                light,
                false
        );
        renderCenteredText(
                client,
                matrices,
                consumers,
                number,
                centerX,
                0.985F,
                DOOR_OVERLAY_Z + 0.002F,
                0.008F,
                0xFFFFFFFF,
                light
        );
    }

    private static void renderIndicator(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            boolean lit,
            int light
    ) {
        renderFrontQuad(
                matrices,
                consumers,
                lit ? INDICATOR_ON : INDICATOR_OFF,
                -0.25F,
                2.13F,
                0.25F,
                2.255F,
                0.211F,
                light,
                true
        );
    }

    private static void renderMesh(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier model,
            Identifier texture,
            int light,
            boolean translucent
    ) {
        final ObjMesh mesh = ObjMeshCache.get(model);
        final VertexConsumer vertices = consumers.getBuffer(
                translucent
                        ? RenderLayer.getEntityTranslucent(texture)
                        : RenderLayer.getEntityCutoutNoCull(texture)
        );
        mesh.render(matrices, vertices, light);
    }

    private static void renderFrontQuad(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float minX,
            float minY,
            float maxX,
            float maxY,
            float z,
            int light,
            boolean translucent
    ) {
        final VertexConsumer vertices = consumers.getBuffer(
                translucent
                        ? RenderLayer.getEntityTranslucent(texture)
                        : RenderLayer.getEntityCutoutNoCull(texture)
        );
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f position = entry.getPositionMatrix();
        final Matrix3f normal = entry.getNormalMatrix();

        vertex(vertices, position, normal, minX, minY, z, 0.0F, 1.0F, light);
        vertex(vertices, position, normal, maxX, minY, z, 1.0F, 1.0F, light);
        vertex(vertices, position, normal, maxX, maxY, z, 1.0F, 0.0F, light);
        vertex(vertices, position, normal, minX, maxY, z, 0.0F, 0.0F, light);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f position,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light
    ) {
        consumer.vertex(position, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .next();
    }

    private static void renderCenteredText(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            String text,
            float x,
            float y,
            float z,
            float scale,
            int color,
            int light
    ) {
        if (text == null || text.isBlank()) {
            return;
        }

        final TextRenderer renderer = client.textRenderer;
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.scale(-scale, -scale, scale);
        final float textX = -renderer.getWidth(text) / 2.0F;
        renderer.draw(
                text,
                textX,
                0.0F,
                color,
                false,
                matrices.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.POLYGON_OFFSET,
                0,
                light
        );
        matrices.pop();
    }

    private static boolean indicatorLit(UUID psdId, double value) {
        final long now = Util.getMeasuringTimeMs();
        final DoorMotionMemory memory = DOOR_MOTION.computeIfAbsent(
                psdId,
                ignored -> new DoorMotionMemory(value, 0, now)
        );
        final double delta = value - memory.lastValue;

        final int newDirection = delta > 1.0E-4D ? 1 : delta < -1.0E-4D ? -1 : 0;
        if (newDirection != 0 && newDirection != memory.direction) {
            memory.direction = newDirection;
            memory.transitionStarted = now;
        }
        memory.lastValue = value;

        if (value <= 1.0E-4D) {
            memory.direction = 0;
            return false;
        }
        if (value >= 1.0D - 1.0E-4D) {
            memory.direction = 0;
            return true;
        }

        return ((now - memory.transitionStarted) / 500L) % 2L == 0L;
    }

    private static double clampDoorValue(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static Identifier model(String name) {
        return MetroBuilder.id(MODEL_ROOT + name + ".obj");
    }

    private static Identifier texture(String name) {
        return MetroBuilder.id("textures/psd/seoul_bulky_white/" + name + ".png");
    }

    private static final class DoorMotionMemory {
        private double lastValue;
        private int direction;
        private long transitionStarted;

        private DoorMotionMemory(double lastValue, int direction, long transitionStarted) {
            this.lastValue = lastValue;
            this.direction = direction;
            this.transitionStarted = transitionStarted;
        }
    }

}
