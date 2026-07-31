package dev.metrobuilder.precision;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Base class for every server-owned precision object.
 *
 * <p>The common transform is deliberately independent from Minecraft blocks and
 * entities. Concrete object types only need to serialize their own data in the
 * custom-data compound.</p>
 */
public abstract class PrecisionObject {
    public static final double MAX_ABSOLUTE_POSITION = 30_000_000.0;
    public static final float MIN_SCALE = 0.0001f;
    public static final float MAX_SCALE = 1024.0f;

    private static final String KEY_ID = "id";
    private static final String KEY_TYPE = "type";
    private static final String KEY_POSITION = "position";
    private static final String KEY_ROTATION = "rotation";
    private static final String KEY_SCALE = "scale";
    private static final String KEY_REVISION = "revision";
    private static final String KEY_CUSTOM_DATA = "data";

    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private static final String KEY_Z = "z";
    private static final String KEY_W = "w";

    private final UUID id;
    private final Identifier typeId;

    private Vec3d position;
    private Quaternionf rotation;
    private Vector3f scale;
    private long revision;

    protected PrecisionObject(UUID id, Identifier typeId) {
        this(id, typeId, Vec3d.ZERO, new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f));
    }

    protected PrecisionObject(UUID id, Identifier typeId, Vec3d position, Quaternionf rotation, Vector3f scale) {
        this.id = Objects.requireNonNull(id, "id");
        this.typeId = Objects.requireNonNull(typeId, "typeId");
        this.position = sanitizePosition(position);
        this.rotation = sanitizeRotation(rotation);
        this.scale = sanitizeScale(scale);
    }

    public final UUID getId() {
        return id;
    }

    public final Identifier getTypeId() {
        return typeId;
    }

    public final Vec3d getPosition() {
        return position;
    }

    public final Quaternionf getRotation() {
        return new Quaternionf(rotation);
    }

    public final Vector3f getScale() {
        return new Vector3f(scale);
    }

    public final long getRevision() {
        return revision;
    }

    public final void setPosition(Vec3d position) {
        Vec3d sanitized = sanitizePosition(position);
        if (!this.position.equals(sanitized)) {
            this.position = sanitized;
            touch();
        }
    }

    public final void move(double x, double y, double z) {
        setPosition(position.add(x, y, z));
    }

    public final void setRotation(Quaternionf rotation) {
        Quaternionf sanitized = sanitizeRotation(rotation);
        if (!this.rotation.equals(sanitized)) {
            this.rotation = sanitized;
            touch();
        }
    }

    public final void setScale(Vector3f scale) {
        Vector3f sanitized = sanitizeScale(scale);
        if (!this.scale.equals(sanitized)) {
            this.scale = sanitized;
            touch();
        }
    }

    public final double squaredDistanceTo(Vec3d point) {
        return position.squaredDistanceTo(Objects.requireNonNull(point, "point"));
    }

    /**
     * Serializes the complete object, including the stable ID and common transform.
     */
    public final NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString(KEY_ID, id.toString());
        nbt.putString(KEY_TYPE, typeId.toString());
        nbt.put(KEY_POSITION, writeVec3d(position));
        nbt.put(KEY_ROTATION, writeQuaternion(rotation));
        nbt.put(KEY_SCALE, writeVector3f(scale));
        nbt.putLong(KEY_REVISION, revision);

        NbtCompound customData = new NbtCompound();
        writeCustomNbt(customData);
        nbt.put(KEY_CUSTOM_DATA, customData);
        return nbt;
    }

    /**
     * Loads transform and custom data into an already-created object.
     * The stored ID and type must match the object created by its registered factory.
     */
    public final void loadNbt(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");

        UUID storedId = readId(nbt).orElseThrow(() -> new IllegalArgumentException("Precision object NBT has no valid id"));
        Identifier storedType = readTypeId(nbt).orElseThrow(() -> new IllegalArgumentException("Precision object NBT has no valid type"));
        if (!id.equals(storedId)) {
            throw new IllegalArgumentException("Precision object id mismatch: expected " + id + ", got " + storedId);
        }
        if (!typeId.equals(storedType)) {
            throw new IllegalArgumentException("Precision object type mismatch: expected " + typeId + ", got " + storedType);
        }

        position = sanitizePosition(readVec3d(nbt.getCompound(KEY_POSITION), Vec3d.ZERO));
        rotation = sanitizeRotation(readQuaternion(nbt.getCompound(KEY_ROTATION), new Quaternionf()));
        scale = sanitizeScale(readVector3f(nbt.getCompound(KEY_SCALE), new Vector3f(1.0f, 1.0f, 1.0f)));
        revision = Math.max(0L, nbt.getLong(KEY_REVISION));
        readCustomNbt(nbt.getCompound(KEY_CUSTOM_DATA));
    }

    protected abstract void writeCustomNbt(NbtCompound nbt);

    protected abstract void readCustomNbt(NbtCompound nbt);

    protected final void touch() {
        if (revision < Long.MAX_VALUE) {
            revision++;
        }
    }

    public static Optional<UUID> readId(NbtCompound nbt) {
        try {
            String value = nbt.getString(KEY_ID);
            return value.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<Identifier> readTypeId(NbtCompound nbt) {
        return Optional.ofNullable(Identifier.tryParse(nbt.getString(KEY_TYPE)));
    }

    private static NbtCompound writeVec3d(Vec3d value) {
        NbtCompound nbt = new NbtCompound();
        nbt.putDouble(KEY_X, value.x);
        nbt.putDouble(KEY_Y, value.y);
        nbt.putDouble(KEY_Z, value.z);
        return nbt;
    }

    private static Vec3d readVec3d(NbtCompound nbt, Vec3d fallback) {
        if (!nbt.contains(KEY_X) || !nbt.contains(KEY_Y) || !nbt.contains(KEY_Z)) {
            return fallback;
        }
        return new Vec3d(nbt.getDouble(KEY_X), nbt.getDouble(KEY_Y), nbt.getDouble(KEY_Z));
    }

    private static NbtCompound writeQuaternion(Quaternionf value) {
        NbtCompound nbt = new NbtCompound();
        nbt.putFloat(KEY_X, value.x());
        nbt.putFloat(KEY_Y, value.y());
        nbt.putFloat(KEY_Z, value.z());
        nbt.putFloat(KEY_W, value.w());
        return nbt;
    }

    private static Quaternionf readQuaternion(NbtCompound nbt, Quaternionf fallback) {
        if (!nbt.contains(KEY_X) || !nbt.contains(KEY_Y) || !nbt.contains(KEY_Z) || !nbt.contains(KEY_W)) {
            return fallback;
        }
        return new Quaternionf(nbt.getFloat(KEY_X), nbt.getFloat(KEY_Y), nbt.getFloat(KEY_Z), nbt.getFloat(KEY_W));
    }

    private static NbtCompound writeVector3f(Vector3f value) {
        NbtCompound nbt = new NbtCompound();
        nbt.putFloat(KEY_X, value.x());
        nbt.putFloat(KEY_Y, value.y());
        nbt.putFloat(KEY_Z, value.z());
        return nbt;
    }

    private static Vector3f readVector3f(NbtCompound nbt, Vector3f fallback) {
        if (!nbt.contains(KEY_X) || !nbt.contains(KEY_Y) || !nbt.contains(KEY_Z)) {
            return fallback;
        }
        return new Vector3f(nbt.getFloat(KEY_X), nbt.getFloat(KEY_Y), nbt.getFloat(KEY_Z));
    }

    private static Vec3d sanitizePosition(Vec3d value) {
        Objects.requireNonNull(value, "position");
        return new Vec3d(
                clampFinite(value.x, -MAX_ABSOLUTE_POSITION, MAX_ABSOLUTE_POSITION, 0.0),
                clampFinite(value.y, -MAX_ABSOLUTE_POSITION, MAX_ABSOLUTE_POSITION, 0.0),
                clampFinite(value.z, -MAX_ABSOLUTE_POSITION, MAX_ABSOLUTE_POSITION, 0.0)
        );
    }

    private static Quaternionf sanitizeRotation(Quaternionf value) {
        Objects.requireNonNull(value, "rotation");
        float x = finiteOr(value.x(), 0.0f);
        float y = finiteOr(value.y(), 0.0f);
        float z = finiteOr(value.z(), 0.0f);
        float w = finiteOr(value.w(), 1.0f);
        float lengthSquared = x * x + y * y + z * z + w * w;
        if (!Float.isFinite(lengthSquared) || lengthSquared < 1.0E-12f) {
            return new Quaternionf();
        }
        return new Quaternionf(x, y, z, w).normalize();
    }

    private static Vector3f sanitizeScale(Vector3f value) {
        Objects.requireNonNull(value, "scale");
        return new Vector3f(
                clampFinite(value.x(), MIN_SCALE, MAX_SCALE, 1.0f),
                clampFinite(value.y(), MIN_SCALE, MAX_SCALE, 1.0f),
                clampFinite(value.z(), MIN_SCALE, MAX_SCALE, 1.0f)
        );
    }

    private static double clampFinite(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFinite(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
