package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.client.network.ClientPrecisionState;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Optional;

/** Renders synchronized precision PSD objects directly into the client world. */
public final class PSDWorldRenderer {
    private static final double MAX_RENDER_DISTANCE = 192.0D;
    private static final double MAX_RENDER_DISTANCE_SQUARED = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    private static final Identifier FALLBACK_TEXTURE = new Identifier("minecraft", "textures/block/iron_block.png");
    private static final Identifier TJ_BOTTOM_LEFT = new Identifier("tjmetro", "textures/block/psd_door_tianjin_bottom_left.png");
    private static final Identifier TJ_BOTTOM_RIGHT = new Identifier("tjmetro", "textures/block/psd_door_tianjin_bottom_right.png");
    private static final Identifier TJ_TOP_LEFT = new Identifier("tjmetro", "textures/block/psd_door_tianjin_top_left.png");
    private static final Identifier TJ_TOP_RIGHT = new Identifier("tjmetro", "textures/block/psd_door_tianjin_top_right.png");
    private static final Identifier TJ_BMT_TOP_TEXTURE = new Identifier("tjmetro", "textures/block/psd_tianjin_bmt_top.png");
    private static final Identifier TJ_BMT_TOP_BLOCK = new Identifier("tjmetro", "psd_top_tianjin_bmt");

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
            renderExactTianjinDoor(matrices, consumers);
            if (!renderExactTianjinTop(matrices, consumers)) {
                renderFallbackHeader(matrices, consumers, FALLBACK_TEXTURE);
            }
        } else {
            renderFallbackBody(matrices, consumers);
            renderFallbackHeader(matrices, consumers, FALLBACK_TEXTURE);
        }

        matrices.pop();
    }

    /**
     * Reproduces the four real TJMetro BMT door block entities with the same
     * ModelPart cuboid and logical texture size used by RenderPSDDoorTianjinBMT.
     */
    private static void renderExactTianjinDoor(MatrixStack matrices, VertexConsumerProvider consumers) {
        // Render TJMetro's own ModelSingleCube at runtime. The local model is kept only
        // as a compatibility fallback when a different TJMetro build hides the class.
        renderTianjinQuarter(matrices, consumers, TJ_BOTTOM_LEFT, -0.5F, 0.0F);
        renderTianjinQuarter(matrices, consumers, TJ_BOTTOM_RIGHT, 0.5F, 0.0F);
        renderTianjinQuarter(matrices, consumers, TJ_TOP_LEFT, -0.5F, 1.0F);
        renderTianjinQuarter(matrices, consumers, TJ_TOP_RIGHT, 0.5F, 1.0F);
    }

    private static void renderTianjinQuarter(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float centerX,
            float baseY
    ) {
        if (!TJMetroRuntimeDoorBridge.renderQuarter(matrices, consumers, texture, centerX, baseY)) {
            TianjinBmtDoorModel.renderQuarter(matrices, consumers, texture, centerX, baseY);
        }
    }

    /**
     * Renders the real TJMetro multipart top block models. Two block states are
     * assembled exactly as an isolated two-block-wide BMT door would be in-world.
     */
    private static boolean renderExactTianjinTop(MatrixStack matrices, VertexConsumerProvider consumers) {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (!Registries.BLOCK.containsId(TJ_BMT_TOP_BLOCK)) {
            return false;
        }
        final Block topBlock = Registries.BLOCK.get(TJ_BMT_TOP_BLOCK);
        if (topBlock == null) {
            return false;
        }

        BlockState leftState = topBlock.getDefaultState();
        leftState = withProperty(leftState, "facing", "south");
        leftState = withProperty(leftState, "side", "left");
        leftState = withProperty(leftState, "air_left", "false");
        leftState = withProperty(leftState, "air_right", "false");
        leftState = withProperty(leftState, "style", "bmt");
        leftState = withProperty(leftState, "arrow_direction", "0");

        BlockState rightState = topBlock.getDefaultState();
        rightState = withProperty(rightState, "facing", "south");
        rightState = withProperty(rightState, "side", "right");
        rightState = withProperty(rightState, "air_left", "false");
        rightState = withProperty(rightState, "air_right", "false");
        rightState = withProperty(rightState, "style", "bmt");
        rightState = withProperty(rightState, "arrow_direction", "0");

        renderBlockModel(client, matrices, consumers, leftState, -1.0F, 2.0F, -0.5F);
        renderBlockModel(client, matrices, consumers, rightState, 0.0F, 2.0F, -0.5F);

        // The station-name/route panel is generated dynamically by TJMetro from MTR
        // platform data. Do not stretch psd_tianjin_bmt_top.png over this area; that
        // texture is only the casing/edge atlas and caused the broken header appearance.
        renderNeutralHeaderPanel(matrices, consumers);
        return true;
    }

    private static void renderBlockModel(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            BlockState state,
            float x,
            float y,
            float z
    ) {
        final BakedModel model = client.getBlockRenderManager().getModel(state);
        final VertexConsumer vertices = consumers.getBuffer(RenderLayers.getBlockLayer(state));
        matrices.push();
        matrices.translate(x, y, z);
        client.getBlockRenderManager().getModelRenderer().render(
                matrices.peek(),
                vertices,
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }

    private static BlockState withProperty(BlockState state, String propertyName, String valueName) {
        for (Property<?> property : state.getProperties()) {
            if (!property.getName().equals(propertyName)) {
                continue;
            }
            final Optional<?> parsed = property.parse(valueName);
            if (parsed.isPresent()) {
                return withParsedProperty(state, property, parsed.get());
            }
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState withParsedProperty(BlockState state, Property property, Object value) {
        return state.with(property, (Comparable) value);
    }

    private static void renderFallbackBody(MatrixStack matrices, VertexConsumerProvider consumers) {
        drawCuboid(matrices, consumers, FALLBACK_TEXTURE, -1.0F, 0.0F, 0.375F, 1.0F, 2.0F, 0.5F);
    }

    private static void renderFallbackHeader(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture
    ) {
        drawCuboid(matrices, consumers, texture, -1.0F, 2.0F, -0.5F, 1.0F, 3.0F, 0.5F);
    }

    private static void renderNeutralHeaderPanel(
            MatrixStack matrices,
            VertexConsumerProvider consumers
    ) {
        // TJMetro normally draws a generated station-name texture here. Beta 1 has no
        // linked MTR platform yet, so render a clean neutral panel in the exact opening
        // without abusing the casing atlas. Beta 2 will replace this with live data.
        final VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TJ_BMT_TOP_TEXTURE));
        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f positionMatrix = entry.getPositionMatrix();
        final Matrix3f normalMatrix = entry.getNormalMatrix();

        final float minX = -0.9921875F;
        final float maxX = 0.9921875F;
        // Exact RenderPSDTopTianjinBMT panel bounds:
        // topPadding=4.5/16, bottomPadding=1.5/16, z=(2-0.05)/16
        // after the original block-entity matrix transform.
        final float minY = 2.09375F;
        final float maxY = 2.71875F;
        final float frontZ = 0.621875F;
        final float backZ = -0.621875F;

        quadZ(vertices, positionMatrix, normalMatrix, minX, minY, maxX, maxY, frontZ, 0, 0, 1, 1, 1);
        quadZ(vertices, positionMatrix, normalMatrix, maxX, minY, minX, maxY, backZ, 0, 0, 1, 1, -1);
    }

    private static void drawCuboid(
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
