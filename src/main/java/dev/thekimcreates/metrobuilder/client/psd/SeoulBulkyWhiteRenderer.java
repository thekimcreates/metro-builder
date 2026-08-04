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
    private static final String PANEL_MODEL_ROOT = "models/psd/seoul_bulky_glass_panel/";
    private static final Identifier SINGLE_PANEL_PACK =
            new Identifier(MetroBuilder.MOD_ID, "seoul_bulky_glass_panel");

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
    private static final Identifier PANEL_WHITE_MODEL = panelModel("panel_white");
    private static final Identifier PANEL_DARK_MODEL = panelModel("panel_dark");
    private static final Identifier PANEL_GLASS_MODEL = panelModel("panel_glass");

    private static final Identifier WHITE_METAL = texture("white_metal");
    private static final Identifier DARK_FRAME = texture("dark_frame");
    private static final Identifier GLASS = texture("glass");
    private static final Identifier HEADER_SIGN = texture("header");
    private static final Identifier CAUTION = texture("caution");
    private static final Identifier INDICATOR_ON = texture("indicator_on");
    private static final Identifier INDICATOR_OFF = texture("indicator_off");
    private static final TextureSet BULKY_WHITE_TEXTURES = new TextureSet(
            WHITE_METAL, DARK_FRAME, GLASS, HEADER_SIGN, CAUTION, INDICATOR_ON, INDICATOR_OFF);
    private static final TextureSet TEMPERED_WHITE_TEXTURES = new TextureSet(
            temperedTexture("white_metal"), temperedTexture("teal_frame"), temperedTexture("glass"),
            temperedTexture("header"), temperedTexture("caution"),
            temperedTexture("indicator_on"), temperedTexture("indicator_off"));
    private static final float DOOR_OVERLAY_Z = 0.002F;
    private static final Identifier HEADER_BADGE = temperedTexture("badge");
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
        final TextureSet textures = psd.packId().equals(
                dev.thekimcreates.metrobuilder.psd.PSDPackRegistry.SEOUL_LINES_5_7_TEMPERED_WHITE_PACK)
                ? TEMPERED_WHITE_TEXTURES : BULKY_WHITE_TEXTURES;
        if (SINGLE_PANEL_PACK.equals(psd.packId())) {
            renderMesh(matrices, consumers, PANEL_WHITE_MODEL, WHITE_METAL, light, false);
            renderMesh(matrices, consumers, PANEL_DARK_MODEL, DARK_FRAME, light, false);
            renderMesh(matrices, consumers, PANEL_GLASS_MODEL, GLASS, light, true);
            return;
        }
        // Match MTR's built-in renderer: use its live door value directly and
        // translate each leaf by exactly that value, with no custom timing.
        final double effectiveDoorValue = MtrTrainDoorLink.findDoorValue(client, psd)
                .orElse(psd.doorValue());
        final float doorOffset = (float) clampDoorValue(effectiveDoorValue);

        renderMesh(matrices, consumers, HEADER_MODEL, textures.whiteMetal(), light, false);
        renderHeaderSign(client, matrices, consumers, psd.displayProperties(), textures, light);

        // Rear sliding rail: opening leaves travel into the clear side pockets.
        matrices.push();
        matrices.translate(-doorOffset, 0.0F, 0.0F);
        renderDoorMeshes(
                matrices,
                consumers,
                LEFT_DOOR_WHITE_MODEL,
                LEFT_DOOR_DARK_MODEL,
                LEFT_DOOR_GLASS_MODEL,
                textures,
                light
        );
        renderCaution(matrices, consumers, textures, -0.5F, light);
        matrices.pop();

        matrices.push();
        matrices.translate(doorOffset, 0.0F, 0.0F);
        renderDoorMeshes(
                matrices,
                consumers,
                RIGHT_DOOR_WHITE_MODEL,
                RIGHT_DOOR_DARK_MODEL,
                RIGHT_DOOR_GLASS_MODEL,
                textures,
                light
        );
        renderCaution(matrices, consumers, textures, 0.5F, light);
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
        renderSideAssembly(matrices, consumers, textures, light);

        renderIndicator(matrices, consumers, textures, indicatorLit(psd.id(), effectiveDoorValue), light);
    }

    /** Adds the standard clean 1.5-block glass wings to a native two-door pack. */
    static void renderCompanionGlass(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light
    ) {
        renderMesh(matrices, consumers, HEADER_WINGS_MODEL, WHITE_METAL, light, false);
        renderSideAssembly(matrices, consumers, BULKY_WHITE_TEXTURES, light);
    }

    private static void renderSideAssembly(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            TextureSet textures,
            int light
    ) {
        renderMesh(matrices, consumers, SIDE_WHITE_MODEL, textures.whiteMetal(), light, false);
        renderMesh(matrices, consumers, SIDE_DARK_MODEL, textures.frame(), light, false);
        renderMesh(matrices, consumers, SIDE_GLASS_MODEL, textures.glass(), light, true);
    }

    private static void renderDoorMeshes(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier whiteModel,
            Identifier darkModel,
            Identifier glassModel,
            TextureSet textures,
            int light
    ) {
        renderMesh(matrices, consumers, whiteModel, textures.whiteMetal(), light, false);
        renderMesh(matrices, consumers, darkModel, textures.frame(), light, false);
        renderMesh(matrices, consumers, glassModel, textures.glass(), light, true);
    }

    private static void renderHeaderSign(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            PSDDisplayProperties properties,
            TextureSet textures,
            int light
    ) {
        // Blank editable fascia. All passenger information below is rendered live.
        renderFrontQuad(
                matrices,
                consumers,
                textures.header(),
                -2.5F,
                2.1F,
                2.5F,
                3.0F,
                0.181F,
                light,
                false
        );
        final int lineColor = properties.lineColorRgb();
        renderTintedQuad(matrices, consumers, textures.whiteMetal(), -2.5F, 2.93F,
                2.5F, 2.99F, 0.184F, lineColor, light);

        // Compact Seoul-style arrangement matching the supplied header reference.
        renderStation(client, matrices, consumers,
                properties.arrowRight() ? properties.previousStationKorean() : properties.nextStationKorean(),
                properties.arrowRight() ? properties.previousStationEnglish() : properties.nextStationEnglish(),
                properties.arrowRight() ? properties.previousStationChinese() : properties.nextStationChinese(),
                properties.arrowRight() ? properties.previousStationJapanese() : properties.nextStationJapanese(),
                -1.55F, light);
        renderTintedQuad(matrices, consumers, HEADER_BADGE, -0.90F, 2.43F,
                -0.62F, 2.71F, 0.185F, lineColor, light);
        renderCenteredText(client, matrices, consumers, properties.platformNumber(),
                -0.76F, 2.525F, 0.188F, 0.0065F, 0xFFFFFFFF, light);
        renderStation(client, matrices, consumers, properties.currentStationKorean(),
                properties.currentStationEnglish(), properties.currentStationChinese(),
                properties.currentStationJapanese(), -0.10F, light);
        renderCenteredText(client, matrices, consumers, properties.arrowRight() ? "→" : "←",
                0.78F, 2.51F, 0.188F, 0.014F, 0xFF111111, light);
        renderStation(client, matrices, consumers,
                properties.arrowRight() ? properties.nextStationKorean() : properties.previousStationKorean(),
                properties.arrowRight() ? properties.nextStationEnglish() : properties.previousStationEnglish(),
                properties.arrowRight() ? properties.nextStationChinese() : properties.previousStationChinese(),
                properties.arrowRight() ? properties.nextStationJapanese() : properties.previousStationJapanese(),
                1.55F, light);
    }

    private static void renderStation(MinecraftClient client, MatrixStack matrices,
                                      VertexConsumerProvider consumers, String ko, String en,
                                      String ch, String jp, float x, int light) {
        renderCenteredText(client, matrices, consumers, ko, x, 2.60F, 0.188F,
                0.010F, 0xFF111111, light);
        renderCenteredText(client, matrices, consumers, en, x, 2.48F, 0.188F,
                0.0045F, 0xFF222222, light);
        renderCenteredText(client, matrices, consumers, ch, x, 2.40F, 0.188F,
                0.0035F, 0xFF333333, light);
        renderCenteredText(client, matrices, consumers, jp, x, 2.34F, 0.188F,
                0.0035F, 0xFF333333, light);
    }

    private static void renderCaution(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            TextureSet textures,
            float centerX,
            int light
    ) {
        renderFrontQuad(
                matrices,
                consumers,
                textures.caution(),
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
            TextureSet textures,
            boolean lit,
            int light
    ) {
        renderFrontQuad(
                matrices,
                consumers,
                lit ? textures.indicatorOn() : textures.indicatorOff(),
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

    private static void renderTintedQuad(MatrixStack matrices, VertexConsumerProvider consumers,
                                         Identifier texture, float minX, float minY,
                                         float maxX, float maxY, float z, int rgb, int light) {
        final VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f position = entry.getPositionMatrix();
        final Matrix3f normal = entry.getNormalMatrix();
        final int red = rgb >> 16 & 255;
        final int green = rgb >> 8 & 255;
        final int blue = rgb & 255;
        tintedVertex(vertices, position, normal, minX, minY, z, 0, 1, red, green, blue, light);
        tintedVertex(vertices, position, normal, maxX, minY, z, 1, 1, red, green, blue, light);
        tintedVertex(vertices, position, normal, maxX, maxY, z, 1, 0, red, green, blue, light);
        tintedVertex(vertices, position, normal, minX, maxY, z, 0, 0, red, green, blue, light);
    }

    private static void tintedVertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                                     float x, float y, float z, float u, float v,
                                     int red, int green, int blue, int light) {
        consumer.vertex(position, x, y, z).color(red, green, blue, 255).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(light)
                .normal(normal, 0.0F, 0.0F, 1.0F).next();
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

    private static Identifier panelModel(String name) {
        return new Identifier(MetroBuilder.MOD_ID, PANEL_MODEL_ROOT + name + ".obj");
    }

    private static Identifier texture(String name) {
        return MetroBuilder.id("textures/psd/seoul_bulky_white/" + name + ".png");
    }

    private static Identifier temperedTexture(String name) {
        return MetroBuilder.id(
                "textures/psd/seoul_lines_5_7_tempered_white/" + name + ".png");
    }

    private record TextureSet(
            Identifier whiteMetal,
            Identifier frame,
            Identifier glass,
            Identifier header,
            Identifier caution,
            Identifier indicatorOn,
            Identifier indicatorOff
    ) {
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
