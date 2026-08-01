package dev.thekimcreates.metrobuilder.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime catalog of PSD pack definitions.
 *
 * <p>The common code always exposes a built-in TJMetro pack. Client resource
 * reloads may replace or extend the catalog with JSON definitions from resource
 * packs without changing persistent world data.</p>
 */
public final class PSDPackRegistry {
    public static final Identifier TJMETRO_BMT_RENDERER = MetroBuilder.id("tjmetro_bmt");

    private static final Map<Identifier, PSDPackDefinition> PACKS = new LinkedHashMap<>();

    static {
        resetToBuiltIns();
    }

    private PSDPackRegistry() {
    }

    public static synchronized void resetToBuiltIns() {
        PACKS.clear();
        PACKS.put(PSDObject.DEFAULT_PACK_ID, builtInTjMetroDefault());
    }

    public static synchronized void replaceResourceDefinitions(
            Collection<PSDPackDefinition> definitions
    ) {
        Objects.requireNonNull(definitions, "definitions");
        final Map<Identifier, PSDPackDefinition> replacements = new LinkedHashMap<>();
        replacements.put(PSDObject.DEFAULT_PACK_ID, builtInTjMetroDefault());

        definitions.stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .forEach(definition -> replacements.put(definition.id(), definition));

        PACKS.clear();
        PACKS.putAll(replacements);
    }

    public static synchronized Optional<PSDPackDefinition> find(Identifier packId) {
        return Optional.ofNullable(PACKS.get(Objects.requireNonNull(packId, "packId")));
    }

    public static synchronized PSDPackDefinition resolve(Identifier packId) {
        return find(packId).orElseGet(() -> PACKS.get(PSDObject.DEFAULT_PACK_ID));
    }

    public static synchronized List<PSDPackDefinition> all() {
        return List.copyOf(PACKS.values());
    }

    public static synchronized Identifier next(Identifier currentPackId) {
        final List<Identifier> ids = new ArrayList<>(PACKS.keySet());
        if (ids.isEmpty()) {
            return PSDObject.DEFAULT_PACK_ID;
        }

        final int currentIndex = ids.indexOf(currentPackId);
        return ids.get((currentIndex + 1 + ids.size()) % ids.size());
    }

    public static synchronized String displayName(Identifier packId) {
        final PSDPackDefinition definition = PACKS.get(packId);
        return definition == null ? packId.toString() : definition.displayName();
    }

    private static PSDPackDefinition builtInTjMetroDefault() {
        return new PSDPackDefinition(
                PSDObject.DEFAULT_PACK_ID,
                PSDPackDefinition.CURRENT_FORMAT_VERSION,
                "TJMetro BMT Default",
                TJMETRO_BMT_RENDERER,
                "tjmetro",
                null,
                null,
                1.0F,
                1.0F
        );
    }
}
