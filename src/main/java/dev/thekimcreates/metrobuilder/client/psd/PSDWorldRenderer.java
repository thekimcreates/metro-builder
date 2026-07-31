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

/** Renders synchronized PSD precision objects directly into the client world. */
public final class PSDWorldRenderer {
    private static final double MAX_RENDER_DISTANCE = 192.0D;
    private static final double MAX_RENDER_DISTANCE_SQUARED = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    /*
     * The assembled PSD is exactly 2 blocks wide and 3 blocks tall:
     * - Four 1 x 1 door-leaf cuboids form a 2 x 2 door body.
     * - One 2 x 1 header cuboid occupies the third block of height.
     */
    private static final float HALF_WIDTH = 1.0F;
    private static final float DOOR_BODY_HEIGHT = 2.0F;
    private static final float HEADER_MIN_Y = 2.0F;
    private static final float HEADER_MAX_Y = 3.0F;

    /* Matches TJMetro's MODEL_PSD depth of 2 texture/model units (2 / 16 block). */
    private static final float DOOR_HALF_DEPTH = 1.0F / 16.0F;
    private static final float HEADER_HALF_DEPTH = 5.0F / 16.0F;

    /* TJMetro's door cuboid declares a logical texture size of 36 x 18. */
    private static final float DOOR_TEXTURE_WIDTH = 36.0F;
    private static final float DOOR_TEXTURE_HEIGHT = 18.0F;

    private static final Identifier FALLBACK_TEXTURE = new Identifier("minecraft", "textures/block/iron_block.png");
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
            /*
             * Each TJMetro door texture is a cuboid atlas, not a flat full-face image.
             * Reproducing the original 36 x 18 cuboid UV layout fixes the stretched,
             * duplicated, and transparent-looking textures from the first renderer.
             */
            drawDoorLeafCuboid(matrices, consumers, TJ_BOTTOM_LEFT, -HALF_WIDTH, 0.0F, 0.0F, 1.0F);
            drawDoorLeafCuboid(matrices, consumers, TJ_BOTTOM_RIGHT, 0.0F, 0.0F, HALF_WIDTH, 1.0F);
            drawDoorLeafCuboid(matrices, consumers, TJ_TOP_LEFT, -HALF_WIDTH, 1.0F, 0.0F, DOOR_BODY_HEIGHT);
            drawDoorLeafCuboid(matrices, consumers, TJ_TOP_RIGHT, 0.0F, 1.0F, HALF_WIDTH, DOOR_BODY_HEIGHT);
            drawHeaderCuboid(matrices, consumers);
        } else {
            drawFullTextureCuboid(
                    matrices,
                    consumers,
                    FALLBACK_TEXTURE,
                    -HALF_WIDTH,
                    0.0F,
                    -DOOR_HALF_DEPTH,
                    HALF_WIDTH,
                    DOOR_BODY_HEIGHT,
                    DOOR_HALF_DEPTH
            );
            drawFullTextureCuboid(
                    matrices,
                    consumers,
                    FALLBACK_TEXTURE,
                    -HALF_WIDTH,
                    HEADER_MIN_Y,
                    -HEADER_HALF_DEPTH,
                    HALF_WIDTH,
                    HEADER_MAX_Y,
                    HEADER_HALF_DEPTH
            );
        }

        matrices.pop();
    }

    /** Draws one 1 x 1 x 0.125 TJMetro door section with the original cuboid-atlas UVs. */
    private static void drawDoorLeafCuboid(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float minX,
            float minY,
            float maxX,
            float maxY
    ) {
        final float minZ = -DOOR_HALF_DEPTH;
        final float maxZ = DOOR_HALF_DEPTH;

        /* Standard ModelPart cuboid net for 16 x 16 x 2 on a logical 36 x 18 texture. */
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

        /* Front (+Z): logical atlas region x=2..18, y=2..18. */
        quadZ(vertices, positionMatrix, normalMatrix, minX, minY, maxX, maxY, maxZ, u1, v1, u2, v2, 1.0F);
        /* Back (-Z): logical atlas region x=20..36, y=2..18. */
        quadZ(vertices, positionMatrix, normalMatrix, maxX, minY, minX, maxY, minZ, u3, v1, u5, v2, -1.0F);
        /* Left (-X): x=0..2, y=2..18. */
        quadX(vertices, positionMatrix, normalMatrix, minX, minY, maxY, minZ, maxZ, u0, v1, u1, v2, -1.0F);
        /* Right (+X): x=18..20, y=2..18. */
        quadX(vertices, positionMatrix, normalMatrix, maxX, minY, maxY, maxZ, minZ, u2, v1, u3, v2, 1.0F);
        /* Top (+Y): x=2..18, y=0..2. */
        quadY(vertices, positionMatrix, normalMatrix, minX, maxX, maxY, maxZ, minZ, u1, v0, u2, v1, 1.0F);
        /* Bottom (-Y): x=18..34, y=0..2. */
        quadY(vertices, positionMatrix, normalMatrix, minX, maxX, minY, minZ, maxZ, u2, v0, u4, v1, -1.0F);
    }

    /** Draws the full 2 x 1 x 0.625 BMT header, completing the exact 2 x 3 silhouette. */
    private static void drawHeaderCuboid(MatrixStack matrices, VertexConsumerProvider consumers) {
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f positionMatrix = entry.getPositionMatrix();
        final Matrix3f normalMatrix = entry.getNormalMatrix();

        final float minX = -HALF_WIDTH;
        final float maxX = HALF_WIDTH;
        final float minY = HEADER_MIN_Y;
        final float maxY = HEADER_MAX_Y;
        final float minZ = -HEADER_HALF_DEPTH;
        final float maxZ = HEADER_HALF_DEPTH;

        final VertexConsumer main = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TJ_HEADER));
        quadZ(main, positionMatrix, normalMatrix, minX, minY, maxX, maxY, maxZ, 0, 0, 1, 1, 1);
        quadZ(main, positionMatrix, normalMatrix, maxX, minY, minX, maxY, minZ, 0, 0, 1, 1, -1);
        quadY(main, positionMatrix, normalMatrix, minX, maxX, maxY, maxZ, minZ, 0, 0, 1, 1, 1);
        quadY(main, positionMatrix, normalMatrix, minX, maxX, minY, minZ, maxZ, 0, 0, 1, 1, -1);

        final VertexConsumer edges = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TJ_HEADER_EDGE));
        quadX(edges, positionMatrix, normalMatrix, minX, minY, maxY, minZ, maxZ, 0, 0, 1, 1, -1);
        quadX(edges, positionMatrix, normalMatrix, maxX, minY, maxY, maxZ, minZ, 0, 0, 1, 1, 1);
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
