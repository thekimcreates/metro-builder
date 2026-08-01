package dev.thekimcreates.metrobuilder.client.model.obj;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;

/** Immutable triangulated OBJ mesh ready for Minecraft's vertex pipeline. */
public final class ObjMesh {
    static final ObjMesh EMPTY = new ObjMesh(new float[0]);

    /** x, y, z, u, v, normalX, normalY, normalZ for each vertex. */
    private static final int STRIDE = 8;

    private final float[] vertices;

    ObjMesh(float[] vertices) {
        if (vertices.length % STRIDE != 0) {
            throw new IllegalArgumentException("OBJ vertex array has an invalid length");
        }
        this.vertices = Arrays.copyOf(vertices, vertices.length);
    }

    public boolean isEmpty() {
        return vertices.length == 0;
    }

    public int triangleCount() {
        return vertices.length / (STRIDE * 3);
    }

    /** Emits this mesh using the active MatrixStack transform. */
    public void render(MatrixStack matrices, VertexConsumer consumer, int light) {
        if (isEmpty()) {
            return;
        }

        final MatrixStack.Entry entry = matrices.peek();
        final Matrix4f positionMatrix = entry.getPositionMatrix();
        final Matrix3f normalMatrix = entry.getNormalMatrix();

        for (int index = 0; index < vertices.length; index += STRIDE) {
            consumer.vertex(
                            positionMatrix,
                            vertices[index],
                            vertices[index + 1],
                            vertices[index + 2]
                    )
                    .color(255, 255, 255, 255)
                    .texture(vertices[index + 3], 1.0F - vertices[index + 4])
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(
                            normalMatrix,
                            vertices[index + 5],
                            vertices[index + 6],
                            vertices[index + 7]
                    )
                    .next();
        }
    }
}
