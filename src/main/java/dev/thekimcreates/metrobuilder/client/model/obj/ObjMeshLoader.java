package dev.thekimcreates.metrobuilder.client.model.obj;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small OBJ parser supporting positions, UVs, normals, and polygon triangulation. */
final class ObjMeshLoader {
    private ObjMeshLoader() {
    }

    static ObjMesh load(ResourceManager resourceManager, Identifier resourceId) throws IOException {
        final Resource resource = resourceManager.getResource(resourceId)
                .orElseThrow(() -> new IOException("Missing OBJ resource " + resourceId));

try (InputStream stream = resource.getInputStream();
     BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                     resource.getInputStream(),
                     StandardCharsets.UTF_8
             ))) {
            return parse(reader, resourceId);
        }
    }

    private static ObjMesh parse(BufferedReader reader, Identifier resourceId) throws IOException {
        final List<Vector3f> positions = new ArrayList<>();
        final List<Vector2f> textureCoordinates = new ArrayList<>();
        final List<Vector3f> normals = new ArrayList<>();
        final FloatCollector output = new FloatCollector();

        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            final String[] values = trimmed.split("\\s+");
            try {
                switch (values[0]) {
                    case "v" -> positions.add(new Vector3f(
                            parseFloat(values, 1),
                            parseFloat(values, 2),
                            parseFloat(values, 3)
                    ));
                    case "vt" -> textureCoordinates.add(new Vector2f(
                            parseFloat(values, 1),
                            parseFloat(values, 2)
                    ));
                    case "vn" -> normals.add(new Vector3f(
                            parseFloat(values, 1),
                            parseFloat(values, 2),
                            parseFloat(values, 3)
                    ).normalize());
                    case "f" -> parseFace(values, positions, textureCoordinates, normals, output);
                    default -> {
                        // Object names, smoothing groups, materials, and comments are optional metadata.
                    }
                }
            } catch (RuntimeException exception) {
                throw new IOException(
                        "Invalid OBJ data in " + resourceId + " at line " + lineNumber + ": " + trimmed,
                        exception
                );
            }
        }

        return new ObjMesh(output.toArray());
    }

    private static void parseFace(
            String[] values,
            List<Vector3f> positions,
            List<Vector2f> textureCoordinates,
            List<Vector3f> normals,
            FloatCollector output
    ) {
        if (values.length < 4) {
            throw new IllegalArgumentException("A face must contain at least three vertices");
        }

        final FaceVertex[] face = new FaceVertex[values.length - 1];
        for (int index = 1; index < values.length; index++) {
            face[index - 1] = parseFaceVertex(
                    values[index],
                    positions.size(),
                    textureCoordinates.size(),
                    normals.size()
            );
        }

        for (int index = 1; index < face.length - 1; index++) {
            appendTriangle(
                    face[0],
                    face[index],
                    face[index + 1],
                    positions,
                    textureCoordinates,
                    normals,
                    output
            );
        }
    }

    private static FaceVertex parseFaceVertex(
            String token,
            int positionCount,
            int textureCount,
            int normalCount
    ) {
        final String[] fields = token.split("/", -1);
        final int position = resolveIndex(fields[0], positionCount);
        final int texture = fields.length > 1 && !fields[1].isEmpty()
                ? resolveIndex(fields[1], textureCount)
                : -1;
        final int normal = fields.length > 2 && !fields[2].isEmpty()
                ? resolveIndex(fields[2], normalCount)
                : -1;
        return new FaceVertex(position, texture, normal);
    }

    private static int resolveIndex(String value, int size) {
        final int index = Integer.parseInt(value);
        final int resolved = index > 0 ? index - 1 : size + index;
        if (resolved < 0 || resolved >= size) {
            throw new IndexOutOfBoundsException("OBJ index " + index + " is outside a list of size " + size);
        }
        return resolved;
    }

    private static void appendTriangle(
            FaceVertex first,
            FaceVertex second,
            FaceVertex third,
            List<Vector3f> positions,
            List<Vector2f> textureCoordinates,
            List<Vector3f> normals,
            FloatCollector output
    ) {
        final Vector3f a = positions.get(first.position());
        final Vector3f b = positions.get(second.position());
        final Vector3f c = positions.get(third.position());

        final Vector3f generatedNormal = new Vector3f(b)
                .sub(a)
                .cross(new Vector3f(c).sub(a));
        if (generatedNormal.lengthSquared() < 1.0E-10F) {
            return;
        }
        generatedNormal.normalize();

        appendVertex(first, a, generatedNormal, textureCoordinates, normals, output);
        appendVertex(second, b, generatedNormal, textureCoordinates, normals, output);
        appendVertex(third, c, generatedNormal, textureCoordinates, normals, output);
    }

    private static void appendVertex(
            FaceVertex faceVertex,
            Vector3f position,
            Vector3f generatedNormal,
            List<Vector2f> textureCoordinates,
            List<Vector3f> normals,
            FloatCollector output
    ) {
        final Vector2f uv = faceVertex.texture() >= 0
                ? textureCoordinates.get(faceVertex.texture())
                : new Vector2f();
        final Vector3f normal = faceVertex.normal() >= 0
                ? normals.get(faceVertex.normal())
                : generatedNormal;

        output.add(position.x);
        output.add(position.y);
        output.add(position.z);
        output.add(uv.x);
        output.add(uv.y);
        output.add(normal.x);
        output.add(normal.y);
        output.add(normal.z);
    }

    private static float parseFloat(String[] values, int index) {
        if (index >= values.length) {
            throw new IllegalArgumentException("Missing numeric value");
        }
        return Float.parseFloat(values[index]);
    }

    private record FaceVertex(int position, int texture, int normal) {
    }

    private static final class FloatCollector {
        private float[] values = new float[256];
        private int size;

        void add(float value) {
            if (size == values.length) {
                final float[] expanded = new float[values.length * 2];
                System.arraycopy(values, 0, expanded, 0, values.length);
                values = expanded;
            }
            values[size++] = value;
        }

        float[] toArray() {
            final float[] result = new float[size];
            System.arraycopy(values, 0, result, 0, size);
            return result;
        }
    }
}
