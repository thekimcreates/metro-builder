package dev.thekimcreates.metrobuilder.psd;

import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

/** Editable Seoul Metro header information carried by one PSD assembly. */
public record PSDDisplayProperties(
        String lineColor,
        String platformNumber,
        boolean arrowRight,
        String currentStationKorean,
        String currentStationEnglish,
        String currentStationChinese,
        String currentStationJapanese,
        String nextStationKorean,
        String nextStationEnglish,
        String nextStationChinese,
        String nextStationJapanese,
        String previousStationKorean,
        String previousStationEnglish,
        String previousStationChinese,
        String previousStationJapanese
) {
    private static final int MAX_TEXT_LENGTH = 64;

    public PSDDisplayProperties {
        lineColor = normalizeColor(lineColor);
        platformNumber = clean(platformNumber);
        currentStationKorean = clean(currentStationKorean);
        currentStationEnglish = clean(currentStationEnglish);
        currentStationChinese = clean(currentStationChinese);
        currentStationJapanese = clean(currentStationJapanese);
        nextStationKorean = clean(nextStationKorean);
        nextStationEnglish = clean(nextStationEnglish);
        nextStationChinese = clean(nextStationChinese);
        nextStationJapanese = clean(nextStationJapanese);
        previousStationKorean = clean(previousStationKorean);
        previousStationEnglish = clean(previousStationEnglish);
        previousStationChinese = clean(previousStationChinese);
        previousStationJapanese = clean(previousStationJapanese);
    }

    public static PSDDisplayProperties defaults() {
        return new PSDDisplayProperties(
                "#996CAC", "512", true,
                "김포공항", "Gimpo Int'l Airport", "金浦机场", "キンポコンハン",
                "개화산", "Gaehwasan", "开花山", "ケファサン",
                "송정", "Songjeong", "松亭", "ソンジョン"
        );
    }

    public int lineColorRgb() {
        return Integer.parseInt(lineColor.substring(1), 16);
    }

    public NbtCompound toNbt() {
        final NbtCompound nbt = new NbtCompound();
        nbt.putString("LineColor", lineColor);
        nbt.putString("PlatformNumber", platformNumber);
        nbt.putBoolean("ArrowRight", arrowRight);
        putStation(nbt, "Current", currentStationKorean, currentStationEnglish,
                currentStationChinese, currentStationJapanese);
        putStation(nbt, "Next", nextStationKorean, nextStationEnglish,
                nextStationChinese, nextStationJapanese);
        putStation(nbt, "Previous", previousStationKorean, previousStationEnglish,
                previousStationChinese, previousStationJapanese);
        return nbt;
    }

    public static PSDDisplayProperties fromNbt(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");
        final PSDDisplayProperties d = defaults();
        return new PSDDisplayProperties(
                get(nbt, "LineColor", d.lineColor),
                get(nbt, "PlatformNumber",
                        get(nbt, "CurrentStationCode", d.platformNumber)),
                !nbt.contains("ArrowRight") || nbt.getBoolean("ArrowRight"),
                get(nbt, "CurrentStationKorean", d.currentStationKorean),
                get(nbt, "CurrentStationEnglish", d.currentStationEnglish),
                get(nbt, "CurrentStationChinese", d.currentStationChinese),
                get(nbt, "CurrentStationJapanese", d.currentStationJapanese),
                get(nbt, "NextStationKorean", d.nextStationKorean),
                get(nbt, "NextStationEnglish", d.nextStationEnglish),
                get(nbt, "NextStationChinese", d.nextStationChinese),
                get(nbt, "NextStationJapanese", d.nextStationJapanese),
                get(nbt, "PreviousStationKorean", d.previousStationKorean),
                get(nbt, "PreviousStationEnglish", d.previousStationEnglish),
                get(nbt, "PreviousStationChinese", d.previousStationChinese),
                get(nbt, "PreviousStationJapanese", d.previousStationJapanese)
        );
    }

    private static void putStation(NbtCompound nbt, String prefix,
                                   String ko, String en, String ch, String jp) {
        nbt.putString(prefix + "StationKorean", ko);
        nbt.putString(prefix + "StationEnglish", en);
        nbt.putString(prefix + "StationChinese", ch);
        nbt.putString(prefix + "StationJapanese", jp);
    }

    private static String get(NbtCompound nbt, String key, String fallback) {
        return nbt.contains(key) ? nbt.getString(key) : fallback;
    }

    private static String clean(String value) {
        final String result = Objects.requireNonNullElse(value, "").strip();
        return result.length() <= MAX_TEXT_LENGTH ? result : result.substring(0, MAX_TEXT_LENGTH);
    }

    private static String normalizeColor(String value) {
        String result = clean(value).toUpperCase();
        if (!result.startsWith("#")) result = "#" + result;
        if (!result.matches("#[0-9A-F]{6}")) return "#996CAC";
        return result;
    }
}
