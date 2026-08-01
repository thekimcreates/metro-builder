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
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.Optional;

/**
 * Renders each precision PSD as the exact six-part Tianjin BMT assembly.
 *
 * <p>The four door quarters use TJMetro's own runtime ModelSingleCube and
 * textures. The two top blocks use Minecraft's baked block-model pipeline with
 * the exact TJMetro block states requested by the project. MetroBuilder only
 * supplies the precision transform; it does not add any billboard or header
 * planes.</p>
 */
public final class PSDWorldRenderer {
    private static final double MAX_RENDER_DISTANCE = 192.0D;
    private static final double MAX_RENDER_DISTANCE_SQUARED = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    private static final Identifier TJ_DOOR_BLOCK = new Identifier("tjmetro", "psd_door_tianjin_bmt");
    private static final Identifier TJ_TOP_BLOCK = new Identifier("tjmetro", "psd_top_tianjin_bmt");

    private static final Identifier TJ_BOTTOM_LEFT_TEXTURE = new Identifier("tjmetro", "textures/block/psd_door_tianjin_bottom_left.png");
    private static final Identifier TJ_BOTTOM_RIGHT_TEXTURE = new Identifier("tjmetro", "textures/block/psd_door_tianjin_bottom_right.png");
    private static final Identifier TJ_TOP_LEFT_TEXTURE = new Identifier("tjmetro", "textures/block/psd_door_tianjin_top_left.png");
    private static final Identifier TJ_TOP_RIGHT_TEXTURE = new Identifier("tjmetro", "textures/block/psd_door_tianjin_top_right.png");

    private static final Identifier FALLBACK_BLOCK = new Identifier("minecraft", "iron_block");

    private static boolean initialized;
    private static boolean warnedMissingTjMetro;

    private PSDWorldRenderer() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        WorldRenderEvents.AFTER_ENTITIES.register(PSDWorldRenderer::renderWorld);
        MetroBuilder.LOGGER.info("Native six-part TJMetro PSD renderer initialized");
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
            renderPsd(client, matrices, consumers, cameraPosition, psd);
        }
    }

    private static void renderPsd(
            MinecraftClient client,
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

        if (FabricLoader.getInstance().isModLoaded("tjmetro")
                && Registries.BLOCK.containsId(TJ_DOOR_BLOCK)
                && Registries.BLOCK.containsId(TJ_TOP_BLOCK)) {
            renderTianjinAssembly(client, matrices, consumers, transform);
        } else {
            if (!warnedMissingTjMetro) {
                warnedMissingTjMetro = true;
                MetroBuilder.LOGGER.warn("TJMetro BMT PSD blocks are unavailable; rendering a simple iron-block fallback");
            }
            renderFallbackAssembly(client, matrices, consumers, transform);
        }

        matrices.pop();
    }

    /** Renders the requested 2-wide by 3-high six-block layout. */
    private static void renderTianjinAssembly(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            PrecisionTransform transform
    ) {
        final BlockState topLeft = createTopState("left");
        final BlockState topRight = createTopState("right");
        final BlockState upperLeft = createDoorState("upper", "left");
        final BlockState upperRight = createDoorState("upper", "right");
        final BlockState lowerLeft = createDoorState("lower", "left");
        final BlockState lowerRight = createDoorState("lower", "right");

        // Bottom row: left and right door quarters.
        renderDoorQuarter(client, matrices, consumers, transform, lowerLeft, TJ_BOTTOM_LEFT_TEXTURE, -0.5F, 0.0F, -0.5D, 0.5D, 0.0D);
        renderDoorQuarter(client, matrices, consumers, transform, lowerRight, TJ_BOTTOM_RIGHT_TEXTURE, 0.5F, 0.0F, 0.5D, 0.5D, 0.0D);

        // Middle row: left and right door quarters.
        renderDoorQuarter(client, matrices, consumers, transform, upperLeft, TJ_TOP_LEFT_TEXTURE, -0.5F, 1.0F, -0.5D, 1.5D, 0.0D);
        renderDoorQuarter(client, matrices, consumers, transform, upperRight, TJ_TOP_RIGHT_TEXTURE, 0.5F, 1.0F, 0.5D, 1.5D, 0.0D);

        // Top row: exact TJMetro multipart baked models. No extra front/back panes.
        renderBakedBlock(client, matrices, consumers, transform, topLeft, -1.0F, 2.0F, -0.5F, -0.5D, 2.5D, 0.0D);
        renderBakedBlock(client, matrices, consumers, transform, topRight, 0.0F, 2.0F, -0.5F, 0.5D, 2.5D, 0.0D);
    }

    private static BlockState createDoorState(String half, String side) {
        BlockState state = Registries.BLOCK.get(TJ_DOOR_BLOCK).getDefaultState();
        state = withProperty(state, "facing", "south");
        state = withProperty(state, "end", "false");
        state = withProperty(state, "half", half);
        state = withProperty(state, "side", side);
        state = withProperty(state, "unlocked", "true");
        return state;
    }

    private static BlockState createTopState(String side) {
        BlockState state = Registries.BLOCK.get(TJ_TOP_BLOCK).getDefaultState();
        state = withProperty(state, "facing", "south");
        state = withProperty(state, "air_left", "false");
        state = withProperty(state, "air_right", "false");
        state = withProperty(state, "arrow_direction", "1");
        state = withProperty(state, "side", side);
        state = withProperty(state, "style", "bmt");
        return state;
    }

    private static void renderDoorQuarter(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            PrecisionTransform transform,
            BlockState state,
            Identifier texture,
            float centerX,
            float baseY,
            double sampleLocalX,
            double sampleLocalY,
            double sampleLocalZ
    ) {
        final BlockPos samplePos = sampleWorldPos(transform, sampleLocalX, sampleLocalY, sampleLocalZ);
        final int light = WorldRenderer.getLightmapCoordinates(client.world, state, samplePos);

        if (!TJMetroRuntimeDoorBridge.renderQuarter(
                matrices,
                consumers,
                texture,
                centerX,
                baseY,
                light,
                OverlayTexture.DEFAULT_UV
        )) {
            TianjinBmtDoorModel.renderQuarter(
                    matrices,
                    consumers,
                    texture,
                    centerX,
                    baseY,
                    light,
                    OverlayTexture.DEFAULT_UV
            );
        }
    }

    private static void renderBakedBlock(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            PrecisionTransform transform,
            BlockState state,
            float renderX,
            float renderY,
            float renderZ,
            double sampleLocalX,
            double sampleLocalY,
            double sampleLocalZ
    ) {
        final BlockPos samplePos = sampleWorldPos(transform, sampleLocalX, sampleLocalY, sampleLocalZ);
        final BakedModel model = client.getBlockRenderManager().getModel(state);
        final VertexConsumer vertices = consumers.getBuffer(RenderLayers.getBlockLayer(state));

        matrices.push();
        matrices.translate(renderX, renderY, renderZ);
        client.getBlockRenderManager().getModelRenderer().render(
                client.world,
                model,
                state,
                samplePos,
                matrices,
                vertices,
                false,
                Random.create(state.getRenderingSeed(samplePos)),
                state.getRenderingSeed(samplePos),
                OverlayTexture.DEFAULT_UV
        );
        matrices.pop();
    }

    private static BlockPos sampleWorldPos(
            PrecisionTransform transform,
            double localX,
            double localY,
            double localZ
    ) {
        final double radians = Math.toRadians(-transform.yaw());
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);
        final double rotatedX = localX * cos + localZ * sin;
        final double rotatedZ = -localX * sin + localZ * cos;

        return BlockPos.ofFloored(
                transform.x() + rotatedX,
                transform.y() + localY,
                transform.z() + rotatedZ
        );
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
        MetroBuilder.LOGGER.warn(
                "TJMetro block {} does not expose expected property {}={}",
                Registries.BLOCK.getId(state.getBlock()),
                propertyName,
                valueName
        );
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState withParsedProperty(BlockState state, Property property, Object value) {
        return state.with(property, (Comparable) value);
    }

    private static void renderFallbackAssembly(
            MinecraftClient client,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            PrecisionTransform transform
    ) {
        final Block fallbackBlock = Registries.BLOCK.get(FALLBACK_BLOCK);
        final BlockState state = fallbackBlock.getDefaultState();
        for (int x = -1; x <= 0; x++) {
            for (int y = 0; y < 3; y++) {
                renderBakedBlock(
                        client,
                        matrices,
                        consumers,
                        transform,
                        state,
                        x,
                        y,
                        -0.5F,
                        x + 0.5D,
                        y + 0.5D,
                        0.0D
                );
            }
        }
    }
}
