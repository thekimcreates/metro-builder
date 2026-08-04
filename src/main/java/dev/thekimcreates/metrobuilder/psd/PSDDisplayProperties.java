package dev.thekimcreates.metrobuilder.psd;

import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

/**
 * Editable passenger-information and label data carried by one PSD assembly.
 *
 * <p>The data is intentionally independent of the active pack. Packs that do
 * not use a field simply ignore it, while a later pack can reuse the same
 * saved values without a world migration.</p>
 */
public record PSDDisplayProperties(
        String currentStationKorean,
        String currentStationEnglish,
        String currentStationCode,
        String previousStationKorean,
        String previousStationEnglish,
        String nextStationKorean,
        String nextStationEnglish,
        String lineNumber,
        String platformNumber,
        boolean arrowRight
) {
    private static final int MAX_TEXT_LENGTH = 64;

    public PSDDisplayProperties {
        currentStationKorean = clean(currentStationKorean);
        currentStationEnglish = clean(currentStationEnglish);
        currentStationCode = clean(currentStationCode);
        previousStationKorean = clean(previousStationKorean);
        previousStationEnglish = clean(previousStationEnglish);
        nextStationKorean = clean(nextStationKorean);
        nextStationEnglish = clean(nextStationEnglish);
        lineNumber = clean(lineNumber);
        platformNumber = clean(platformNumber);
    }

    public static PSDDisplayProperties defaults() {
        return new PSDDisplayProperties(
                "김포공항",
                "Gimpo Int'l Airport",
                "512",
                "송정",
                "Songjeong",
                "개화산",
                "Gaehwasan",
                "5",
                "1-1",
                true
        );
    }

    public NbtCompound toNbt() {
        final NbtCompound nbt = new NbtCompound();
        nbt.putString("CurrentStationKorean", currentStationKorean);
        nbt.putString("CurrentStationEnglish", currentStationEnglish);
        nbt.putString("CurrentStationCode", currentStationCode);
        nbt.putString("PreviousStationKorean", previousStationKorean);
        nbt.putString("PreviousStationEnglish", previousStationEnglish);
        nbt.putString("NextStationKorean", nextStationKorean);
        nbt.putString("NextStationEnglish", nextStationEnglish);
        nbt.putString("LineNumber", lineNumber);
        nbt.putString("PlatformNumber", platformNumber);
        nbt.putBoolean("ArrowRight", arrowRight);
        return nbt;
    }

    public static PSDDisplayProperties fromNbt(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");
        final PSDDisplayProperties defaults = defaults();
        return new PSDDisplayProperties(
                get(nbt, "CurrentStationKorean", defaults.currentStationKorean),
                get(nbt, "CurrentStationEnglish", defaults.currentStationEnglish),
                get(nbt, "CurrentStationCode", defaults.currentStationCode),
                get(nbt, "PreviousStationKorean", defaults.previousStationKorean),
                get(nbt, "PreviousStationEnglish", defaults.previousStationEnglish),
                get(nbt, "NextStationKorean", defaults.nextStationKorean),
                get(nbt, "NextStationEnglish", defaults.nextStationEnglish),
                get(nbt, "LineNumber", defaults.lineNumber),
                get(nbt, "PlatformNumber", defaults.platformNumber),
                !nbt.contains("ArrowRight") || nbt.getBoolean("ArrowRight")
        );
    }

    private static String get(NbtCompound nbt, String key, String fallback) {
        return nbt.contains(key) ? nbt.getString(key) : fallback;
    }

    private static String clean(String value) {
        final String result = Objects.requireNonNullElse(value, "").strip();
        return result.length() <= MAX_TEXT_LENGTH
                ? result
                : result.substring(0, MAX_TEXT_LENGTH);
    }
}
