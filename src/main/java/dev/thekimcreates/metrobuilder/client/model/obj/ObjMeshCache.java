package dev.thekimcreates.metrobuilder.client.model.obj;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Lazily loads OBJ assets and invalidates them on client resource reload. */
public final class ObjMeshCache {
    private static final Map<Identifier, ObjMesh> CACHE = new HashMap<>();
    private static ResourceManager resourceManager;

    private ObjMeshCache() {
    }

    public static synchronized ObjMesh get(Identifier resourceId) {
        return CACHE.computeIfAbsent(resourceId, ObjMeshCache::load);
    }

    static synchronized void reload(ResourceManager manager) {
        resourceManager = manager;
        CACHE.clear();
        MetroBuilder.LOGGER.info("MetroBuilder OBJ mesh cache cleared");
    }

    private static ObjMesh load(Identifier resourceId) {
        final ResourceManager manager = resourceManager != null
                ? resourceManager
                : MinecraftClient.getInstance().getResourceManager();
        try {
            final ObjMesh mesh = ObjMeshLoader.load(manager, resourceId);
            MetroBuilder.LOGGER.debug(
                    "Loaded OBJ mesh {} ({} triangle(s))",
                    resourceId,
                    mesh.triangleCount()
            );
            return mesh;
        } catch (Exception exception) {
            MetroBuilder.LOGGER.error("Failed to load OBJ mesh {}", resourceId, exception);
            return ObjMesh.EMPTY;
        }
    }
}
