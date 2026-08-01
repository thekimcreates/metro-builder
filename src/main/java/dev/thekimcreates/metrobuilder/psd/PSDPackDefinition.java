package dev.thekimcreates.metrobuilder.psd;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;

/** Immutable metadata describing one selectable PSD resource pack. */
public record PSDPackDefinition(
        Identifier id,
        int formatVersion,
        String displayName,
        Identifier rendererId,
        String requiredMod,
        Identifier openingSound,
        Identifier closingSound,
        float soundVolume,
        float soundPitch
) {
    public static final int CURRENT_FORMAT_VERSION = 1;

    public PSDPackDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(rendererId, "rendererId");
        requiredMod = requiredMod == null ? "" : requiredMod.trim();
        if (formatVersion <= 0) {
            throw new IllegalArgumentException("formatVersion must be positive");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }
        if (!Float.isFinite(soundVolume) || soundVolume < 0.0F) {
            throw new IllegalArgumentException("soundVolume must be finite and non-negative");
        }
        if (!Float.isFinite(soundPitch) || soundPitch <= 0.0F) {
            throw new IllegalArgumentException("soundPitch must be finite and positive");
        }
    }

    public Optional<String> optionalRequiredMod() {
        return requiredMod.isBlank() ? Optional.empty() : Optional.of(requiredMod);
    }

    public Optional<Identifier> optionalOpeningSound() {
        return Optional.ofNullable(openingSound);
    }

    public Optional<Identifier> optionalClosingSound() {
        return Optional.ofNullable(closingSound);
    }
}
