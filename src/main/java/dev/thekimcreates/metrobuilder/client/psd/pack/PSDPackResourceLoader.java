package dev.thekimcreates.metrobuilder.client.psd.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.psd.PSDPackDefinition;
import dev.thekimcreates.metrobuilder.psd.PSDPackRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loads data-driven PSD pack metadata from client resource packs. */
public final class PSDPackResourceLoader implements SimpleSynchronousResourceReloadListener {
    private static final String DIRECTORY = "psd_packs";
    private static final String JSON_SUFFIX = ".json";
    private static boolean initialized;

    private PSDPackResourceLoader() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new PSDPackResourceLoader());
    }

    @Override
    public Identifier getFabricId() {
        return MetroBuilder.id("psd_pack_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        final List<PSDPackDefinition> definitions = new ArrayList<>();
        final Map<Identifier, Resource> resources = manager.findResources(
                DIRECTORY,
                id -> id.getPath().endsWith(JSON_SUFFIX)
        );

        resources.forEach((resourceId, resource) -> {
            try (InputStreamReader reader = new InputStreamReader(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8
            )) {
                final JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    throw new IllegalArgumentException("root must be a JSON object");
                }

                final Identifier packId = packIdFromResource(resourceId);
                definitions.add(parseDefinition(packId, parsed.getAsJsonObject()));
            } catch (Exception exception) {
                MetroBuilder.LOGGER.warn("Failed to load PSD pack definition {}", resourceId, exception);
            }
        });

        PSDPackRegistry.replaceResourceDefinitions(definitions);
        MetroBuilder.LOGGER.info("Loaded {} PSD pack definition(s)", PSDPackRegistry.all().size());
    }

    private static Identifier packIdFromResource(Identifier resourceId) {
        final String prefix = DIRECTORY + "/";
        final String path = resourceId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(JSON_SUFFIX)) {
            throw new IllegalArgumentException("Invalid PSD pack resource path: " + resourceId);
        }
        final String packPath = path.substring(prefix.length(), path.length() - JSON_SUFFIX.length());
        return new Identifier(resourceId.getNamespace(), packPath);
    }

    private static PSDPackDefinition parseDefinition(Identifier packId, JsonObject json) {
        final int formatVersion = getInt(json, "format_version", 1);
        if (formatVersion > PSDPackDefinition.CURRENT_FORMAT_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported format_version " + formatVersion
                            + " (maximum " + PSDPackDefinition.CURRENT_FORMAT_VERSION + ")"
            );
        }

        final String displayName = getString(json, "name", packId.toString());
        final Identifier rendererId = new Identifier(
                getString(json, "renderer", PSDPackRegistry.TJMETRO_BMT_RENDERER.toString())
        );
        final String requiredMod = getString(json, "required_mod", "");
        final Identifier openingSound = getOptionalIdentifier(json, "opening_sound");
        final Identifier closingSound = getOptionalIdentifier(json, "closing_sound");
        final float volume = getFloat(json, "volume", 1.0F);
        final float pitch = getFloat(json, "pitch", 1.0F);

        return new PSDPackDefinition(
                packId,
                formatVersion,
                displayName,
                rendererId,
                requiredMod,
                openingSound,
                closingSound,
                volume,
                pitch
        );
    }

    private static String getString(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive()
                ? json.get(key).getAsString()
                : fallback;
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive()
                ? json.get(key).getAsInt()
                : fallback;
    }

    private static float getFloat(JsonObject json, String key, float fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive()
                ? json.get(key).getAsFloat()
                : fallback;
    }

    private static Identifier getOptionalIdentifier(JsonObject json, String key) {
        final String value = getString(json, key, "").trim();
        return value.isEmpty() ? null : new Identifier(value);
    }
}
