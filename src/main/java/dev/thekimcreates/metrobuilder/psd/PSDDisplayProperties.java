package dev.thekimcreates.metrobuilder.psd;

import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;
import java.util.Objects;

/** The uploaded 1280x256 PNG used by one PSD header. */
public record PSDDisplayProperties(byte[] headerPng) {
    public static final int MAX_HEADER_BYTES = 2 * 1024 * 1024;

    public PSDDisplayProperties {
        headerPng = Objects.requireNonNullElseGet(headerPng, () -> new byte[0]).clone();
        if (headerPng.length > MAX_HEADER_BYTES) {
            throw new IllegalArgumentException("Header PNG exceeds 2 MiB");
        }
    }

    @Override
    public byte[] headerPng() {
        return headerPng.clone();
    }

    public boolean hasHeaderPng() {
        return headerPng.length > 0;
    }

    public static PSDDisplayProperties defaults() {
        return new PSDDisplayProperties(new byte[0]);
    }

    public NbtCompound toNbt() {
        final NbtCompound nbt = new NbtCompound();
        nbt.putByteArray("HeaderPng", headerPng);
        return nbt;
    }

    public static PSDDisplayProperties fromNbt(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");
        return new PSDDisplayProperties(nbt.contains("HeaderPng")
                ? nbt.getByteArray("HeaderPng") : new byte[0]);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PSDDisplayProperties properties
                && Arrays.equals(headerPng, properties.headerPng);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(headerPng);
    }
}
