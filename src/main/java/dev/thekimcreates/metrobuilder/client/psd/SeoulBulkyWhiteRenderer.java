package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.psd.PSDDisplayProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Renders the native five-block Seoul Metro Bulky White PSD assembly. */
final class SeoulBulkyWhiteRenderer {
    static final double HALF_WIDTH = 2.5D;
    static final double HALF_DEPTH = 0.16D;

    private static final Identifier WHITE_METAL = MetroBuilder.id(
            "textures/psd/seoul_bulky_white/white_metal.png"
    );
    private static final Identifier DARK_FRAME = MetroBuilder.id(
            "textures/psd/seoul_bulky_white/dark_frame.png"
    );
    private static final Identifier GLASS = MetroBuilder.id(
            "textures/psd/seoul_bulky_white/glass.png"
    );
    private static final Identifier THRESHOLD = MetroBuilder.id(
            "textures/psd/seoul_bulky_white/threshold.png"
    );
    private static final Identifier CAUTION = MetroBuilder.id(
            "textures/psd/seoul_bulky_white/caution.png"
    );
    private static final Identifier INDICATOR_ON = MetroBuilder.id(
            "textures/psd/seoul_bulky_white/indicator_on.png"
    );
    private static final Identifier INDICATOR_OFF = MetroBuilder.id(
            "textures/psd/seoul_bulky_white/indicator_off.png"
    );

    private static final int ATLAS_SIZE = 256;
    private static final float FRAME_DEPTH = 0.22F;
    private static final float GLASS_DEPTH = 0.035F;
    private static final float DECAL_DEPTH = 0.008F;
    private static final float POST_WIDTH = 0.10F;
    private static final float RAIL_HEIGHT = 0.10F;

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
        final float doorOffset = (float) (Math.max(0.0D, Math.min(1.0D, psd.doorValue())) * 0.82D);

        // Single top row. The header spans the complete five-block assembly.
        renderCuboid(matrices, consumers, WHITE_METAL, -2.5F, 2.0F, -0.14F, 5.0F, 1.0F, 0.28F, light, false);
        renderCuboid(matrices, consumers, DARK_FRAME, -2.5F, 1.90F, -0.15F, 5.0F, 0.10F, 0.30F, light, false);

        // Fixed outer and separator frames.
        renderPost(matrices, consumers, -2.50F, light);
        renderPost(matrices, consumers, -1.00F, light);
        renderPost(matrices, consumers, 0.00F, light);
        renderPost(matrices, consumers, 1.00F, light);
        renderPost(matrices, consumers, 2.50F, light);
        renderCuboid(matrices, consumers, WHITE_METAL, -2.5F, 0.0F, -0.12F, 5.0F, RAIL_HEIGHT, 0.24F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, -2.5F, 1.90F, -0.12F, 5.0F, RAIL_HEIGHT, 0.24F, light, false);

        // Clean 1.5-block side glass panels: no text, decals, or platform numbers.
        renderGlassPanel(matrices, consumers, -2.45F, -1.05F, light);
        renderGlassPanel(matrices, consumers, 1.05F, 2.45F, light);

        // Sliding door leaves, each exactly one block wide when closed.
        renderDoorLeaf(
                matrices,
                consumers,
                -0.95F - doorOffset,
                -0.05F - doorOffset,
                -0.5F - doorOffset,
                true,
                psd.displayProperties(),
                light
        );
        renderDoorLeaf(
                matrices,
                consumers,
                0.05F + doorOffset,
                0.95F + doorOffset,
                0.5F + doorOffset,
                false,
                psd.displayProperties(),
                light
        );

        final boolean indicatorLit = indicatorLit(psd);
        renderCuboid(
                matrices,
                consumers,
                indicatorLit ? INDICATOR_ON : INDICATOR_OFF,
                0.72F,
                2.67F,
                -0.155F,
                0.22F,
                0.22F,
                DECAL_DEPTH,
                light,
                true
        );

        renderHeaderText(client, matrices, consumers, psd.displayProperties(), light);
    }

    /**
     * Adds the standard 1.5-block glass wings to a two-block door renderer.
     * Used by every legacy/native door pack so all placed PSD assemblies share
     * the same five-block footprint and center anchor.
     */
    static void renderCompanionGlass(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            int light
    ) {
        renderCuboid(matrices, consumers, WHITE_METAL, -2.5F, 2.0F, -0.14F, 1.5F, 1.0F, 0.28F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, 1.0F, 2.0F, -0.14F, 1.5F, 1.0F, 0.28F, light, false);
        renderPost(matrices, consumers, -2.50F, light);
        renderPost(matrices, consumers, -1.00F, light);
        renderPost(matrices, consumers, 1.00F, light);
        renderPost(matrices, consumers, 2.50F, light);
        renderCuboid(matrices, consumers, WHITE_METAL, -2.5F, 0.0F, -0.12F, 1.5F, RAIL_HEIGHT, 0.24F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, 1.0F, 0.0F, -0.12F, 1.5F, RAIL_HEIGHT, 0.24F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, -2.5F, 1.90F, -0.12F, 1.5F, RAIL_HEIGHT, 0.24F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, 1.0F, 1.90F, -0.12F, 1.5F, RAIL_HEIGHT, 0.24F, light, false);
        renderGlassPanel(matrices, consumers, -2.45F, -1.05F, light);
        renderGlassPanel(matrices, consumers, 1.05F, 2.45F, light);
    }

    private static void renderPost(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            float centerX,
            int light
    ) {
        renderCuboid(
                matrices,
                consumers,
                WHITE_METAL,
                centerX - POST_WIDTH / 2.0F,
                0.0F,
                -FRAME_DEPTH / 2.0F,
                POST_WIDTH,
                2.0F,
                FRAME_DEPTH,
                light,
                false
        );
    }

    private static void renderGlassPanel(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            float minX,
            float maxX,
            int light
    ) {
        renderCuboid(
                matrices,
                consumers,
                GLASS,
                minX,
                0.10F,
                -GLASS_DEPTH / 2.0F,
                maxX - minX,
                1.80F,
                GLASS_DEPTH,
                light,
                true
        );
    }

    private static void renderDoorLeaf(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            float minX,
            float maxX,
            float decalCenterX,
            boolean leftDoor,
            PSDDisplayProperties properties,
            int light
    ) {
        renderCuboid(
                matrices,
                consumers,
                GLASS,
                minX,
                0.10F,
                -GLASS_DEPTH / 2.0F,
                maxX - minX,
                1.80F,
                GLASS_DEPTH,
                light,
                true
        );

        // Moving door frame edges.
        renderCuboid(matrices, consumers, WHITE_METAL, minX, 0.0F, -0.12F, 0.08F, 2.0F, 0.24F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, maxX - 0.08F, 0.0F, -0.12F, 0.08F, 2.0F, 0.24F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, minX, 0.0F, -0.12F, maxX - minX, 0.09F, 0.24F, light, false);
        renderCuboid(matrices, consumers, WHITE_METAL, minX, 1.91F, -0.12F, maxX - minX, 0.09F, 0.24F, light, false);

        // Caution signs are present on both left and right doors.
        renderCuboid(
                matrices,
                consumers,
                CAUTION,
                decalCenterX - 0.16F,
                0.92F,
                -0.135F,
                0.32F,
                0.46F,
                DECAL_DEPTH,
                light,
                true
        );

        // Platform number appears below the warning sign on the right door only.
        if (!leftDoor && !properties.platformNumber().isBlank()) {
            renderCenteredText(
                    MinecraftClient.getInstance(),
                    matrices,
                    consumers,
                    properties.platformNumber(),
                    decalCenterX,
                    0.68F,
                    0.142F,
                    0.010F,
                    0xFF222222,
                    light
            );
        }

        // Brushed threshold beneath the moving door pair.
        renderCuboid(
                matrices,
                consumers,
                THRESHOLD,
                minX,
                0.0F,
                -0.145F,
                maxX - minX,
                0.08F,
                0.29F,
                light,
                false
        );
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

        final float previousX = arrowRight ? -1.72F : 1.72F;
        final float currentX = arrowRight ? -0.47F : 0.47F;
        final float arrowX = 0.52F;
        final float nextX = arrowRight ? 1.45F : -1.45F;

        renderStationPair(
                client,
                matrices,
                consumers,
                properties.previousStationKorean(),
                properties.previousStationEnglish(),
                previousX,
                0xFF7A7A7A,
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
                light
        );
        renderCenteredText(
                client,
                matrices,
                consumers,
                arrow,
                arrowX,
                2.50F,
                0.148F,
                0.015F,
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
                light
        );

        if (!properties.currentStationCode().isBlank()) {
            renderCenteredText(
                    client,
                    matrices,
                    consumers,
                    properties.currentStationCode(),
                    currentX - 0.48F,
                    2.49F,
                    0.149F,
                    0.0085F,
                    0xFF6F2DA8,
                    light
            );
        }
        if (!properties.lineNumber().isBlank()) {
            renderCenteredText(
                    client,
                    matrices,
                    consumers,
                    properties.lineNumber(),
                    -2.27F,
                    2.49F,
                    0.149F,
                    0.010F,
                    0xFF6F2DA8,
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
            int light
    ) {
        if (!korean.isBlank()) {
            renderCenteredText(
                    client,
                    matrices,
                    consumers,
                    korean,
                    centerX,
                    2.66F,
                    0.148F,
                    0.0095F,
                    color,
                    light
            );
        }
        if (!english.isBlank()) {
            renderCenteredText(
                    client,
                    matrices,
                    consumers,
                    english,
                    centerX,
                    2.43F,
                    0.148F,
                    0.0065F,
                    color,
                    light
            );
        }
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

    private static void renderCuboid(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            int light,
            boolean translucent
    ) {
        final ModelPart.Cuboid cuboid = new ModelPart.Cuboid(
                0,
                0,
                x * 16.0F,
                -(y + height) * 16.0F,
                z * 16.0F,
                width * 16.0F,
                height * 16.0F,
                depth * 16.0F,
                0.0F,
                0.0F,
                0.0F,
                false,
                ATLAS_SIZE,
                ATLAS_SIZE,
                EnumSet.allOf(Direction.class)
        );

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        final VertexConsumer vertices = consumers.getBuffer(
                translucent
                        ? RenderLayer.getEntityTranslucent(texture)
                        : RenderLayer.getEntityCutoutNoCull(texture)
        );
        cuboid.renderCuboid(
                matrices.peek(),
                vertices,
                light,
                OverlayTexture.DEFAULT_UV,
                1.0F,
                1.0F,
                1.0F,
                1.0F
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

        // Opening and closing: 0.5 seconds on, 0.5 seconds off, repeating.
        return ((now - memory.transitionStarted) / 500L) % 2L == 0L;
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
