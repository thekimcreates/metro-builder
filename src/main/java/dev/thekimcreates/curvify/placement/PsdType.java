package dev.thekimcreates.curvify.placement;

import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** The complete whitelist of placeable, built-in MTR PSD items. */
public enum PsdType {
    DOOR_1("mtr:psd_door", "mtr:psd_door", Kind.DOOR, 1, 2),
    DOOR_2("mtr:psd_door_2", "mtr:psd_door_2", Kind.DOOR, 2, 2),
    GLASS_1("mtr:psd_glass", "mtr:psd_glass", Kind.GLASS, 1, 1),
    GLASS_2("mtr:psd_glass_2", "mtr:psd_glass_2", Kind.GLASS, 2, 1),
    GLASS_END_1("mtr:psd_glass_end", "mtr:psd_glass_end", Kind.GLASS_END, 1, 1),
    GLASS_END_2("mtr:psd_glass_end_2", "mtr:psd_glass_end_2", Kind.GLASS_END, 2, 1);

    private static final Set<Identifier> ITEM_IDS = Arrays.stream(values())
            .map(PsdType::itemId)
            .collect(Collectors.toUnmodifiableSet());

    private final Identifier itemId;
    private final Identifier blockId;
    private final Kind kind;
    private final int style;
    private final int width;

    PsdType(String itemId, String blockId, Kind kind, int style, int width) {
        this.itemId = new Identifier(itemId);
        this.blockId = new Identifier(blockId);
        this.kind = kind;
        this.style = style;
        this.width = width;
    }

    public Identifier itemId() {
        return itemId;
    }

    public Identifier blockId() {
        return blockId;
    }

    public Kind kind() {
        return kind;
    }

    public int style() {
        return style;
    }

    public int width() {
        return width;
    }

    public String sectionLabel() {
        return switch (kind) {
            case DOOR -> "Door Section";
            case GLASS -> "Glass Section";
            case GLASS_END -> "Glass End Section";
        };
    }

    public static Optional<PsdType> fromItemId(Identifier itemId) {
        return Arrays.stream(values()).filter(value -> value.itemId.equals(itemId)).findFirst();
    }

    public static boolean isPsdItem(Identifier itemId) {
        return ITEM_IDS.contains(itemId);
    }

    public enum Kind {
        DOOR,
        GLASS,
        GLASS_END
    }
}
