package dev.thekimcreates.metrobuilder.client.model.obj;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/** Hooks OBJ cache invalidation into F3+T and resource-pack reloads. */
public final class ObjMeshResourceLoader implements SimpleSynchronousResourceReloadListener {
    private static boolean initialized;

    private ObjMeshResourceLoader() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new ObjMeshResourceLoader());
    }

    @Override
    public Identifier getFabricId() {
        return MetroBuilder.id("obj_mesh_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        ObjMeshCache.reload(manager);
    }
}
