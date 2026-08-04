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

/** Runtime catalog of selectable PSD packs. */
public final class PSDPackRegistry {
    public static final Identifier TJMETRO_BMT_RENDERER = MetroBuilder.id("tjmetro_bmt");
    public static final Identifier SEOUL_BULKY_WHITE_RENDERER = MetroBuilder.id("seoul_bulky_white");
    public static final Identifier SEOUL_BULKY_WHITE_PACK = MetroBuilder.id("seoul_bulky_white");
    public static final Identifier SEOUL_LINES_5_7_TEMPERED_WHITE_PACK =
            MetroBuilder.id("seoul_lines_5_7_tempered_white");

    private static final Map<Identifier, PSDPackDefinition> PACKS = new LinkedHashMap<>();

    static {
        resetToBuiltIns();
    }

    private PSDPackRegistry() {
    }

    public static synchronized void resetToBuiltIns() {
        PACKS.clear();
        addBuiltIns(PACKS);
    }

    public static synchronized void replaceResourceDefinitions(
            Collection<PSDPackDefinition> definitions
    ) {
        Objects.requireNonNull(definitions, "definitions");
        final Map<Identifier, PSDPackDefinition> replacements = new LinkedHashMap<>();
        addBuiltIns(replacements);

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

    private static void addBuiltIns(Map<Identifier, PSDPackDefinition> destination) {
        destination.put(PSDObject.DEFAULT_PACK_ID, builtInTjMetroDefault());
        destination.put(SEOUL_BULKY_WHITE_PACK, builtInSeoulBulkyWhite());
        destination.put(SEOUL_LINES_5_7_TEMPERED_WHITE_PACK, builtInSeoulLines57TemperedWhite());
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

    private static PSDPackDefinition builtInSeoulBulkyWhite() {
        return new PSDPackDefinition(
                SEOUL_BULKY_WHITE_PACK,
                PSDPackDefinition.CURRENT_FORMAT_VERSION,
                "Seoul Metro Bulky White",
                SEOUL_BULKY_WHITE_RENDERER,
                "",
                null,
                null,
                1.0F,
                1.0F
        );
    }

    private static PSDPackDefinition builtInSeoulLines57TemperedWhite() {
        return new PSDPackDefinition(
                SEOUL_LINES_5_7_TEMPERED_WHITE_PACK,
                PSDPackDefinition.CURRENT_FORMAT_VERSION,
                "Seoul Metro Lines 5, 7 Tempered White",
                SEOUL_BULKY_WHITE_RENDERER,
                "",
                null,
                null,
                1.0F,
                1.0F
        );
    }
}
