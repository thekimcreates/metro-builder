package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.client.network.ClientPrecisionState;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders synchronized precision PSD objects directly into the client world. */
public final class PSDWorldRenderer {
    private static final double MAX_RENDER_DISTANCE = 192.0D;
    private static final double MAX_RENDER_DISTANCE_SQUARED = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    /*
     * TJMetro BMT proportions, matching the supplied in-game reference:
     * - total width: 2 blocks
     * - total height: 3 blocks
     * - glass door body: 2 blocks tall
     * - route/header enclosure: 1 block tall
     */
    private static final float HALF_WIDTH = 1.0F;
    private static final float DOOR_BODY_HEIGHT = 2.0F;
    private static final float HEADER_MIN_Y = 2.0F;
    private static final float HEADER_MAX_Y = 3.0F;

    /*
     * TJMetro's ModelSingleCube is not centered in the block. Its 2/16-deep
     * door leaves sit against the platform-facing side of the block. Keeping
     * that offset is essential to reproduce the real PSD silhouette.
     */
    private static final float DOOR_BACK_Z = 6.0F / 16.0F;
    private static final float DOOR_FRONT_Z = 8.0F / 16.0F;

    /* The BMT top enclosure projects behind the door while sharing its front face. */
    private static final float HEADER_BACK_Z = -2.0F / 16.0F;
    private static final float HEADER_FRONT_Z = 8.0F / 16.0F;

    /* Dark lower fascia visible beneath the BMT header in the real model. */
    private static final float FASCIA_MIN_Y = 1.9375F;
    private static final float FASCIA_MAX_Y = 2.0625F;
    private static final float FASCIA_BACK_Z = 4.75F / 16.0F;
    private static final float FASCIA_FRONT_Z = 8.25F / 16.0F;

    /* TJMetro's door cuboid declares a logical texture size of 36 x 18. */
    private static final float DOOR_TEXTURE_WIDTH = 36.0F;
    private static final float DOOR_TEXTURE_HEIGHT = 18.0F;

    private static final Identifier FALLBACK_TEXTURE = new Identifier("minecraft", "textures/block/iron_block.png");
    private static final Identifier BLACK_TEXTURE = new Identifier("minecraft", "textures/block/black_concrete.png");
    private static final Identifier TJ_BOTTOM_LEFT = new Identifier(
            "tjmetro",
            "textures/block/psd_door_tianjin_bottom_left.png"
    );
    private static final Identifier TJ_BOTTOM_RIGHT = new Identifier(
            "tjmetro",
            "textures/block/psd_door_tianjin_bottom_right.png"
    );
    private static final Identifier TJ_TOP_LEFT = new Identifier(
            "tjmetro",
            "textures/block/psd_door_tianjin_top_left.png"
    );
    private static final Identifier TJ_TOP_RIGHT = new Identifier(
            "tjmetro",
            "textures/block/psd_door_tianjin_top_right.png"
    );
    private static final Identifier TJ_HEADER = new Identifier(
            "tjmetro",
            "textures/block/psd_tianjin_bmt_top.png"
    );
    private static final Identifier TJ_HEADER_EDGE = new Identifier(
            "tjmetro",
            "textures/block/psd_tianjin_bmt_top_edge.png"
    );

    private static boolean initialized;

    private PSDWorldRenderer() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        WorldRenderEvents.AFTER_ENTITIES.register(PSDWorldRenderer::renderWorld);
        MetroBuilder.LOGGER.info("Precision PSD world renderer initialized");
    }

    private static void renderWorld(WorldRenderContext context) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final MatrixStack matrices = context.matrixStack();
        final VertexConsumerProvider consumers = context.consumers();

        if (client.world == null || matrices == null || consumers == null) {
            return;
        }

        final Identifier currentDimension = client.world.getRegistryKey().getValue();
        if (ClientPrecisionState.dimensionId().filter(currentDimension::equals).isEmpty()) {
            return;
        }

        final Vec3d cameraPosition = context.camera().getPos();
        for (ClientPSDObject psd : ClientPrecisionState.psds()) {
            if (psd.transform().squaredDistanceTo(cameraPosition) > MAX_RENDER_DISTANCE_SQUARED) {
                continue;
            }
            renderPsd(matrices, consumers, cameraPosition, psd);
        }
    }

    private static void renderPsd(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Vec3d cameraPosition,
            ClientPSDObject psd
    ) {
        final PrecisionTransform transform = psd.transform();
        matrices.push();
        matrices.translate(
                transform.x() - cameraPosition.x,
                transform.y() - cameraPosition.y,
                transform.z() - cameraPosition.z
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-transform.yaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(transform.pitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(transform.roll()));
        matrices.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());

        if (FabricLoader.getInstance().isModLoaded("tjmetro")) {
            /* Four 1 x 1 TJMetro door sections form the exact 2 x 2 lower body. */
            drawDoorSection(matrices, consumers, TJ_BOTTOM_LEFT, -HALF_WIDTH, 0.0F, 0.0F, 1.0F);
            drawDoorSection(matrices, consumers, TJ_BOTTOM_RIGHT, 0.0F, 0.0F, HALF_WIDTH, 1.0F);
            drawDoorSection(matrices, consumers, TJ_TOP_LEFT, -HALF_WIDTH, 1.0F, 0.0F, DOOR_BODY_HEIGHT);
            drawDoorSection(matrices, consumers, TJ_TOP_RIGHT, 0.0F, 1.0F, HALF_WIDTH, DOOR_BODY_HEIGHT);

            drawHeaderEnclosure(matrices, consumers);
            drawFullTextureCuboid(
                    matrices,
                    consumers,
                    BLACK_TEXTURE,
                    -HALF_WIDTH,
                    FASCIA_MIN_Y,
                    FASCIA_BACK_Z,
                    HALF_WIDTH,
                    FASCIA_MAX_Y,
                    FASCIA_FRONT_Z
            );
        } else {
            drawFullTextureCuboid(
                    matrices,
                    consumers,
                    FALLBACK_TEXTURE,
                    -HALF_WIDTH,
                    0.0F,
                    DOOR_BACK_Z,
                    HALF_WIDTH,
                    DOOR_BODY_HEIGHT,
                    DOOR_FRONT_Z
            );
            drawFullTextureCuboid(
                    matrices,
                    consumers,
                    FALLBACK_TEXTURE,
                    -HALF_WIDTH,
                    HEADER_MIN_Y,
                    HEADER_BACK_Z,
                    HALF_WIDTH,
                    HEADER_MAX_Y,
                    HEADER_FRONT_Z
            );
        }

        matrices.pop();
    }

    /**
     * Draws one 1 x 1 x 0.125 TJMetro door section.
     *
     * The supplied PNG is a 36 x 18 logical cuboid atlas stored at 2x pixel
     * resolution. The platform-facing surface corresponds to the atlas's
     * second 16 x 16 face. Using that face restores the gray threshold, thick
     * black mullions, and clear glass arrangement visible in TJMetro.
     */
    private static void drawDoorSection(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float minX,
            float minY,
            float maxX,
            float maxY
    ) {
        final float u0 = 0.0F / DOOR_TEXTURE_WIDTH;
        final float u1 = 2.0F / DOOR_TEXTURE_WIDTH;
        final float u2 = 18.0F / DOOR_TEXTURE_WIDTH;
        final float u3 = 20.0F / DOOR_TEXTURE_WIDTH;
        final float u4 = 34.0F / DOOR_TEXTURE_WIDTH;
        final float u5 = 36.0F / DOOR_TEXTURE_WIDTH;
        final float v0 = 0.0F / DOOR_TEXTURE_HEIGHT;
        final float v1 = 2.0F / DOOR_TEXTURE_HEIGHT;
        final float v2 = 18.0F / DOOR_TEXTURE_HEIGHT;

        final VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f positionMatrix = entry.getPositionMatrix();
        final Matrix3f normalMatrix = entry.getNormalMatrix();

        /* Platform-facing surface: second 16 x 16 atlas face. */
        quadZ(
                vertices,
                positionMatrix,
                normalMatrix,
                minX,
                minY,
                maxX,
                maxY,
                DOOR_FRONT_Z,
                u3,
                v1,
                u5,
                v2,
                1.0F
        );

        /* Track-facing surface: first 16 x 16 atlas face, mirrored by winding. */
        quadZ(
                vertices,
                positionMatrix,
                normalMatrix,
                maxX,
                minY,
                minX,
                maxY,
                DOOR_BACK_Z,
                u1,
                v1,
                u2,
                v2,
                -1.0F
        );

        /* Original cuboid edge strips. */
        quadX(
                vertices,
                positionMatrix,
                normalMatrix,
                minX,
                minY,
                maxY,
                DOOR_BACK_Z,
                DOOR_FRONT_Z,
                u0,
                v1,
                u1,
                v2,
                -1.0F
        );
        quadX(
                vertices,
                positionMatrix,
                normalMatrix,
                maxX,
                minY,
                maxY,
                DOOR_FRONT_Z,
                DOOR_BACK_Z,
                u2,
                v1,
                u3,
                v2,
                1.0F
        );
        quadY(
                vertices,
                positionMatrix,
                normalMatrix,
                minX,
                maxX,
                maxY,
                DOOR_FRONT_Z,
                DOOR_BACK_Z,
                u1,
                v0,
                u2,
                v1,
                1.0F
        );
        quadY(
                vertices,
                positionMatrix,
                normalMatrix,
                minX,
                maxX,
                minY,
                DOOR_BACK_Z,
                DOOR_FRONT_Z,
                u2,
                v0,
                u4,
                v1,
                -1.0F
        );
    }

    /** Draws the 2 x 1 BMT route/header enclosure above the two-block door body. */
    private static void drawHeaderEnclosure(MatrixStack matrices, VertexConsumerProvider consumers) {
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f positionMatrix = entry.getPositionMatrix();
        final Matrix3f normalMatrix = entry.getNormalMatrix();

        final VertexConsumer main = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TJ_HEADER));
        quadZ(
                main,
                positionMatrix,
                normalMatrix,
                -HALF_WIDTH,
                HEADER_MIN_Y,
                HALF_WIDTH,
                HEADER_MAX_Y,
                HEADER_FRONT_Z,
                0,
                0,
                1,
                1,
                1
        );
        quadZ(
                main,
                positionMatrix,
                normalMatrix,
                HALF_WIDTH,
                HEADER_MIN_Y,
                -HALF_WIDTH,
                HEADER_MAX_Y,
                HEADER_BACK_Z,
                0,
                0,
                1,
                1,
                -1
        );

        final VertexConsumer edges = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TJ_HEADER_EDGE));
        quadX(
                edges,
                positionMatrix,
                normalMatrix,
                -HALF_WIDTH,
                HEADER_MIN_Y,
                HEADER_MAX_Y,
                HEADER_BACK_Z,
                HEADER_FRONT_Z,
                0,
                0,
                1,
                1,
                -1
        );
        quadX(
                edges,
                positionMatrix,
                normalMatrix,
                HALF_WIDTH,
                HEADER_MIN_Y,
                HEADER_MAX_Y,
                HEADER_FRONT_Z,
                HEADER_BACK_Z,
                0,
                0,
                1,
                1,
                1
        );
        quadY(
                edges,
                positionMatrix,
                normalMatrix,
                -HALF_WIDTH,
                HALF_WIDTH,
                HEADER_MAX_Y,
                HEADER_FRONT_Z,
                HEADER_BACK_Z,
                0,
                0,
                1,
                1,
                1
        );
        quadY(
                edges,
                positionMatrix,
                normalMatrix,
                -HALF_WIDTH,
                HALF_WIDTH,
                HEADER_MIN_Y,
                HEADER_BACK_Z,
                HEADER_FRONT_Z,
                0,
                0,
                1,
                1,
                -1
        );
    }

    private static void drawFullTextureCuboid(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
    ) {
        final VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f positionMatrix = entry.getPositionMatrix();
        final Matrix3f normalMatrix = entry.getNormalMatrix();

        quadZ(vertices, positionMatrix, normalMatrix, minX, minY, maxX, maxY, maxZ, 0, 0, 1, 1, 1);
        quadZ(vertices, positionMatrix, normalMatrix, maxX, minY, minX, maxY, minZ, 0, 0, 1, 1, -1);
        quadX(vertices, positionMatrix, normalMatrix, minX, minY, maxY, minZ, maxZ, 0, 0, 1, 1, -1);
        quadX(vertices, positionMatrix, normalMatrix, maxX, minY, maxY, maxZ, minZ, 0, 0, 1, 1, 1);
        quadY(vertices, positionMatrix, normalMatrix, minX, maxX, maxY, maxZ, minZ, 0, 0, 1, 1, 1);
        quadY(vertices, positionMatrix, normalMatrix, minX, maxX, minY, minZ, maxZ, 0, 0, 1, 1, -1);
    }

    private static void quadZ(
            VertexConsumer vertices,
            Matrix4f positionMatrix,
            Matrix3f normalMatrix,
            float minX,
            float minY,
            float maxX,
            float maxY,
            float z,
            float minU,
            float minV,
            float maxU,
            float maxV,
            float normalZ
    ) {
        vertex(vertices, positionMatrix, normalMatrix, minX, minY, z, minU, maxV, 0, 0, normalZ);
        vertex(vertices, positionMatrix, normalMatrix, maxX, minY, z, maxU, maxV, 0, 0, normalZ);
        vertex(vertices, positionMatrix, normalMatrix, maxX, maxY, z, maxU, minV, 0, 0, normalZ);
        vertex(vertices, positionMatrix, normalMatrix, minX, maxY, z, minU, minV, 0, 0, normalZ);
    }

    private static void quadX(
            VertexConsumer vertices,
            Matrix4f positionMatrix,
            Matrix3f normalMatrix,
            float x,
            float minY,
            float maxY,
            float minZ,
            float maxZ,
            float minU,
            float minV,
            float maxU,
            float maxV,
            float normalX
    ) {
        vertex(vertices, positionMatrix, normalMatrix, x, minY, minZ, minU, maxV, normalX, 0, 0);
        vertex(vertices, positionMatrix, normalMatrix, x, minY, maxZ, maxU, maxV, normalX, 0, 0);
        vertex(vertices, positionMatrix, normalMatrix, x, maxY, maxZ, maxU, minV, normalX, 0, 0);
        vertex(vertices, positionMatrix, normalMatrix, x, maxY, minZ, minU, minV, normalX, 0, 0);
    }

    private static void quadY(
            VertexConsumer vertices,
            Matrix4f positionMatrix,
            Matrix3f normalMatrix,
            float minX,
            float maxX,
            float y,
            float minZ,
            float maxZ,
            float minU,
            float minV,
            float maxU,
            float maxV,
            float normalY
    ) {
        vertex(vertices, positionMatrix, normalMatrix, minX, y, minZ, minU, maxV, 0, normalY, 0);
        vertex(vertices, positionMatrix, normalMatrix, maxX, y, minZ, maxU, maxV, 0, normalY, 0);
        vertex(vertices, positionMatrix, normalMatrix, maxX, y, maxZ, maxU, minV, 0, normalY, 0);
        vertex(vertices, positionMatrix, normalMatrix, minX, y, maxZ, minU, minV, 0, normalY, 0);
    }

    private static void vertex(
            VertexConsumer vertices,
            Matrix4f positionMatrix,
            Matrix3f normalMatrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ
    ) {
        vertices.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .next();
    }
}
