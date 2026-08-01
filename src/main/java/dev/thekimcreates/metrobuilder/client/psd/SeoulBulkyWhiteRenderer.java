package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.client.model.obj.ObjMesh;
import dev.thekimcreates.metrobuilder.client.model.obj.ObjMeshCache;
import dev.thekimcreates.metrobuilder.psd.PSDDisplayProperties;
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
    private static final Identifier SIDE_THRESHOLD_MODEL = model("side_threshold");
    private static final Identifier LEFT_DOOR_WHITE_MODEL = model("left_door_white");
    private static final Identifier LEFT_DOOR_DARK_MODEL = model("left_door_dark");
    private static final Identifier LEFT_DOOR_GLASS_MODEL = model("left_door_glass");
    private static final Identifier LEFT_DOOR_THRESHOLD_MODEL = model("left_door_threshold");
    private static final Identifier RIGHT_DOOR_WHITE_MODEL = model("right_door_white");
    private static final Identifier RIGHT_DOOR_DARK_MODEL = model("right_door_dark");
    private static final Identifier RIGHT_DOOR_GLASS_MODEL = model("right_door_glass");
    private static final Identifier RIGHT_DOOR_THRESHOLD_MODEL = model("right_door_threshold");

    private static final Identifier WHITE_METAL = texture("white_metal");
    private static final Identifier DARK_FRAME = texture("dark_frame");
    private static final Identifier GLASS = texture("glass");
    private static final Identifier THRESHOLD = texture("threshold");
    private static final Identifier CAUTION = texture("caution");
    private static final Identifier INDICATOR_ON = texture("indicator_on");
    private static final Identifier INDICATOR_OFF = texture("indicator_off");

    private static final float DOOR_OVERLAY_Z = 0.058F;
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
        final float doorOffset = (float) (clampDoorValue(psd.doorValue()) * 0.82D);

        renderMesh(matrices, consumers, HEADER_MODEL, WHITE_METAL, light, false);

        // Rear sliding rail: opening leaves travel into the clear side pockets.
        matrices.push();
        matrices.translate(-doorOffset, 0.0F, 0.0F);
        renderDoorMeshes(
                matrices,
                consumers,
                LEFT_DOOR_WHITE_MODEL,
                LEFT_DOOR_DARK_MODEL,
                LEFT_DOOR_GLASS_MODEL,
                LEFT_DOOR_THRESHOLD_MODEL,
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
                RIGHT_DOOR_THRESHOLD_MODEL,
                light
        );
        renderCaution(matrices, consumers, 0.5F, light);
        if (!psd.displayProperties().platformNumber().isBlank()) {
            renderCenteredText(
                    client,
                    matrices,
                    consumers,
                    psd.displayProperties().platformNumber(),
                    0.5F,
                    0.63F,
                    DOOR_OVERLAY_Z + 0.002F,
                    0.010F,
                    0xFFF0F0F0,
                    light
            );
        }
        matrices.pop();

        // Fixed panels are a physically separate front layer.
        renderSideAssembly(matrices, consumers, light);

        renderIndicator(matrices, consumers, indicatorLit(psd), light);
        renderHeaderText(client, matrices, consumers, psd.displayProperties(), light);
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
        renderMesh(matrices, consumers, SIDE_THRESHOLD_MODEL, THRESHOLD, light, false);
    }

    private static void renderDoorMeshes(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier whiteModel,
            Identifier darkModel,
            Identifier glassModel,
            Identifier thresholdModel,
            int light
    ) {
        renderMesh(matrices, consumers, whiteModel, WHITE_METAL, light, false);
        renderMesh(matrices, consumers, darkModel, DARK_FRAME, light, false);
        renderMesh(matrices, consumers, glassModel, GLASS, light, true);
        renderMesh(matrices, consumers, thresholdModel, THRESHOLD, light, false);
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
                0.92F,
                centerX + 0.30F,
                1.30F,
                DOOR_OVERLAY_Z,
                light,
                false
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
                -0.16F,
                2.06F,
                0.16F,
                2.16F,
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

    private static void renderHeaderText(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            PSDDisplayProperties properties,
            int light
    ) {
        final boolean arrowRight = properties.arrowRight();
        final String arrow = arrowRight ? "→" : "←";

        final float previousX = arrowRight ? -1.67F : 1.67F;
        final float currentX = 0.0F;
        final float arrowX = arrowRight ? 0.84F : -0.84F;
        final float nextX = arrowRight ? 1.67F : -1.67F;

        renderStationPair(
                client,
                matrices,
                consumers,
                properties.previousStationKorean(),
                properties.previousStationEnglish(),
                previousX,
                0xFF777777,
                1.20F,
                light
        );
        renderStationPair(
                client,
                matrices,
                consumers,
                properties.currentStationKorean(),
                properties.currentStationEnglish(),
                currentX,
                0xFF111111,
                1.25F,
                light
        );
        renderCenteredText(
                client,
                matrices,
                consumers,
                arrow,
                arrowX,
                2.49F,
                0.211F,
                0.016F,
                0xFF111111,
                light
        );
        renderStationPair(
                client,
                matrices,
                consumers,
                properties.nextStationKorean(),
                properties.nextStationEnglish(),
                nextX,
                0xFF222222,
                1.20F,
                light
        );

        if (!properties.currentStationCode().isBlank()) {
            renderBadgeText(
                    client,
                    matrices,
                    consumers,
                    properties.currentStationCode(),
                    currentX,
                    2.20F,
                    light
            );
        }
    }

    private static void renderStationPair(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            String korean,
            String english,
            float centerX,
            int color,
            float maxWidth,
            int light
    ) {
        if (!korean.isBlank()) {
            renderFittedCenteredText(
                    client,
                    matrices,
                    consumers,
                    korean,
                    centerX,
                    2.61F,
                    0.211F,
                    0.010F,
                    maxWidth,
                    color,
                    light
            );
        }
        if (!english.isBlank()) {
            renderFittedCenteredText(
                    client,
                    matrices,
                    consumers,
                    english,
                    centerX,
                    2.40F,
                    0.211F,
                    0.0065F,
                    maxWidth,
                    color,
                    light
            );
        }
    }

    private static void renderFittedCenteredText(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            String text,
            float x,
            float y,
            float z,
            float preferredScale,
            float maxWidth,
            int color,
            int light
    ) {
        if (text == null || text.isBlank()) {
            return;
        }
        final int pixelWidth = Math.max(1, client.textRenderer.getWidth(text));
        final float fittedScale = Math.min(preferredScale, maxWidth / pixelWidth);
        renderCenteredText(client, matrices, consumers, text, x, y, z, fittedScale, color, light);
    }

    private static void renderBadgeText(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            String text,
            float centerX,
            float centerY,
            int light
    ) {
        renderFrontQuad(
                matrices,
                consumers,
                DARK_FRAME,
                centerX - 0.115F,
                centerY - 0.115F,
                centerX + 0.115F,
                centerY + 0.115F,
                0.2105F,
                light,
                false
        );
        renderCenteredText(
                client,
                matrices,
                consumers,
                text,
                centerX,
                centerY + 0.015F,
                0.212F,
                0.0075F,
                0xFFFFFFFF,
                light
        );
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

    private static boolean indicatorLit(ClientPSDObject psd) {
        final long now = Util.getMeasuringTimeMs();
        final DoorMotionMemory memory = DOOR_MOTION.computeIfAbsent(
                psd.id(),
                ignored -> new DoorMotionMemory(psd.doorValue(), 0, now)
        );
        final double value = psd.doorValue();
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
