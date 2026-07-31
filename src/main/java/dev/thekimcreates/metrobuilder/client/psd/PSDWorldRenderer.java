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

    private static final float HALF_WIDTH = 1.5F;
    private static final float BODY_HEIGHT = 3.0F;
    private static final float HALF_HEIGHT = BODY_HEIGHT / 2.0F;
    private static final float TOP_HEIGHT = 0.45F;
    private static final float FACE_OFFSET = 0.015625F;

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

        final boolean tjMetroLoaded = FabricLoader.getInstance().isModLoaded("tjmetro");
        final Identifier bottomLeft = tjMetroLoaded ? TJ_BOTTOM_LEFT : FALLBACK_TEXTURE;
        final Identifier bottomRight = tjMetroLoaded ? TJ_BOTTOM_RIGHT : FALLBACK_TEXTURE;
        final Identifier topLeft = tjMetroLoaded ? TJ_TOP_LEFT : FALLBACK_TEXTURE;
        final Identifier topRight = tjMetroLoaded ? TJ_TOP_RIGHT : FALLBACK_TEXTURE;
        final Identifier header = tjMetroLoaded ? TJ_HEADER : FALLBACK_TEXTURE;

        drawDoubleSidedPanel(matrices, consumers, bottomLeft, -HALF_WIDTH, 0.0F, 0.0F, HALF_HEIGHT);
        drawDoubleSidedPanel(matrices, consumers, bottomRight, 0.0F, 0.0F, HALF_WIDTH, HALF_HEIGHT);
        drawDoubleSidedPanel(matrices, consumers, topLeft, -HALF_WIDTH, HALF_HEIGHT, 0.0F, BODY_HEIGHT);
        drawDoubleSidedPanel(matrices, consumers, topRight, 0.0F, HALF_HEIGHT, HALF_WIDTH, BODY_HEIGHT);
        drawDoubleSidedPanel(
                matrices,
                consumers,
                header,
                -HALF_WIDTH,
                BODY_HEIGHT,
                HALF_WIDTH,
                BODY_HEIGHT + TOP_HEIGHT
        );

        matrices.pop();
    }

    private static void drawDoubleSidedPanel(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float minX,
            float minY,
            float maxX,
            float maxY
    ) {
        final VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f positionMatrix = entry.getPositionMatrix();
        final Matrix3f normalMatrix = entry.getNormalMatrix();
        final int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        final int overlay = OverlayTexture.DEFAULT_UV;

        vertex(vertices, positionMatrix, normalMatrix, minX, minY, FACE_OFFSET, 0.0F, 1.0F, overlay, light, 1.0F);
        vertex(vertices, positionMatrix, normalMatrix, maxX, minY, FACE_OFFSET, 1.0F, 1.0F, overlay, light, 1.0F);
        vertex(vertices, positionMatrix, normalMatrix, maxX, maxY, FACE_OFFSET, 1.0F, 0.0F, overlay, light, 1.0F);
        vertex(vertices, positionMatrix, normalMatrix, minX, maxY, FACE_OFFSET, 0.0F, 0.0F, overlay, light, 1.0F);

        vertex(vertices, positionMatrix, normalMatrix, maxX, minY, -FACE_OFFSET, 1.0F, 1.0F, overlay, light, -1.0F);
        vertex(vertices, positionMatrix, normalMatrix, minX, minY, -FACE_OFFSET, 0.0F, 1.0F, overlay, light, -1.0F);
        vertex(vertices, positionMatrix, normalMatrix, minX, maxY, -FACE_OFFSET, 0.0F, 0.0F, overlay, light, -1.0F);
        vertex(vertices, positionMatrix, normalMatrix, maxX, maxY, -FACE_OFFSET, 1.0F, 0.0F, overlay, light, -1.0F);
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
            int overlay,
            int light,
            float normalZ
    ) {
        vertices.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(normalMatrix, 0.0F, 0.0F, normalZ)
                .next();
    }
}
