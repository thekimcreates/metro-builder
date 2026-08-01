package dev.thekimcreates.metrobuilder.client.psd;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.EnumSet;

/**
 * Exact vanilla-model equivalent of TJMetro's RenderPSDDoorTianjinBMT.ModelSingleCube.
 *
 * <p>TJMetro creates each door quarter with a 36 x 18 logical texture atlas and a
 * 16 x 16 x 2 model cuboid at (-8, -16, -8). Rendering the same ModelPart.Cuboid
 * preserves Mojang's cuboid UV layout exactly, including the original 2/16-block
 * depth and all thin edge strips.</p>
 */
public final class TianjinBmtDoorModel {
    private static final int TEXTURE_WIDTH = 36;
    private static final int TEXTURE_HEIGHT = 18;

    private static final ModelPart.Cuboid DOOR_CUBOID = new ModelPart.Cuboid(
            0,
            0,
            -8.0F,
            -16.0F,
            -8.0F,
            16.0F,
            16.0F,
            2.0F,
            0.0F,
            0.0F,
            0.0F,
            false,
            TEXTURE_WIDTH,
            TEXTURE_HEIGHT,
            EnumSet.allOf(Direction.class)
    );

    private TianjinBmtDoorModel() {
    }

    /** Renders one one-block-wide, one-block-tall door quarter. */
    public static void renderQuarter(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float centerX,
            float baseY
    ) {
        matrices.push();
        matrices.translate(centerX, baseY, 0.0F);

        // This is the same 180-degree X rotation used by TJMetro's block entity renderer.
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));

        final VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        DOOR_CUBOID.renderCuboid(
                matrices.peek(),
                vertices,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
        matrices.pop();
    }
}
