package dev.metrobuilder.client.render;

import dev.metrobuilder.entity.PrecisionPSDEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Optional;

public final class PrecisionPSDRenderer extends EntityRenderer<PrecisionPSDEntity> {
    public PrecisionPSDRenderer(EntityRendererFactory.Context context) { super(context); }

    @Override
    public void render(PrecisionPSDEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        if (PrecisionPSDEntity.TYPE_GLASS.equals(entity.getPsdType())) renderGlass(matrices, vertices, light);
        else renderDoor(entity, matrices, vertices, light);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }

    private static void renderDoor(PrecisionPSDEntity entity, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
        Block door = block("tjmetro:psd_door_tianjin_bmt");
        if (door == null) door = block("mtr:psd_door");
        Block top = block("tjmetro:psd_top_tianjin_bmt");
        if (door == null) return;
        float slide = entity.getDoorValue() * 0.46f;
        renderState(with(door.getDefaultState(), "half", "lower"), matrices, vertices, light, -slide, 0, 0);
        renderState(with(door.getDefaultState(), "half", "upper"), matrices, vertices, light, slide, 1, 0);
        if (top != null) renderState(top.getDefaultState(), matrices, vertices, light, 0, 2, 0);
    }

    private static void renderGlass(MatrixStack matrices, VertexConsumerProvider vertices, int light) {
        Block glass = block("mtr:psd_glass");
        Block top = block("mtr:psd_top");
        if (glass == null) return;
        for (int i=0;i<3;i++) {
            renderState(with(with(glass.getDefaultState(),"side","single"),"half","lower"),matrices,vertices,light,i,0,0);
            renderState(with(with(glass.getDefaultState(),"side","single"),"half","upper"),matrices,vertices,light,i,1,0);
            if(top!=null) renderState(with(with(top.getDefaultState(),"air_left","false"),"air_right","false"),matrices,vertices,light,i,2,0);
        }
    }

    private static void renderState(BlockState state, MatrixStack matrices, VertexConsumerProvider vertices, int light, double x, double y, double z) {
        matrices.push(); matrices.translate(x,y,z);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(state, matrices, vertices, light, 0);
        matrices.pop();
    }

    private static Block block(String id) {
        Identifier identifier=Identifier.tryParse(id);
        if(identifier==null||!Registries.BLOCK.containsId(identifier))return null;
        Block block=Registries.BLOCK.get(identifier); return block==Blocks.AIR?null:block;
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static BlockState with(BlockState state,String name,String value){
        for(Property property:state.getProperties()) if(property.getName().equals(name)) {
            Optional parsed=property.parse(value); if(parsed.isPresent()) return state.with(property,(Comparable)parsed.get());
        }
        return state;
    }

    @Override public Identifier getTexture(PrecisionPSDEntity entity) { return new Identifier("minecraft","textures/atlas/blocks.png"); }
}
