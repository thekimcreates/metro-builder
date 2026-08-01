package dev.thekimcreates.metrobuilder.precision;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/**
 * Immutable position, rotation, and scale for a precision object.
 *
 * <p>Rotation values are stored in degrees. MetroBuilder intentionally does
 * not snap or normalize them, allowing exact user-entered angles.</p>
 */
public record PrecisionTransform(
        double x,
        double y,
        double z,
        float pitch,
        float yaw,
        float roll,
        float scaleX,
        float scaleY,
        float scaleZ
) {
    private static final String POSITION_X_KEY = "X";
    private static final String POSITION_Y_KEY = "Y";
    private static final String POSITION_Z_KEY = "Z";
    private static final String PITCH_KEY = "Pitch";
    private static final String YAW_KEY = "Yaw";
    private static final String ROLL_KEY = "Roll";
    private static final String SCALE_X_KEY = "ScaleX";
    private static final String SCALE_Y_KEY = "ScaleY";
    private static final String SCALE_Z_KEY = "ScaleZ";

    public PrecisionTransform {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(pitch, "pitch");
        requireFinite(yaw, "yaw");
        requireFinite(roll, "roll");
        requirePositiveFinite(scaleX, "scaleX");
        requirePositiveFinite(scaleY, "scaleY");
        requirePositiveFinite(scaleZ, "scaleZ");
    }

    public static PrecisionTransform identity() {
        return new PrecisionTransform(0.0, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    public static PrecisionTransform at(Vec3d position) {
        Objects.requireNonNull(position, "position");
        return identity().withPosition(position);
    }

    public Vec3d position() {
        return new Vec3d(x, y, z);
    }

    public PrecisionTransform withPosition(Vec3d position) {
        Objects.requireNonNull(position, "position");
        return withPosition(position.x, position.y, position.z);
    }

    public PrecisionTransform withPosition(double newX, double newY, double newZ) {
        return new PrecisionTransform(newX, newY, newZ, pitch, yaw, roll, scaleX, scaleY, scaleZ);
    }

    public PrecisionTransform translated(double deltaX, double deltaY, double deltaZ) {
        return withPosition(x + deltaX, y + deltaY, z + deltaZ);
    }

    public PrecisionTransform withRotation(float newPitch, float newYaw, float newRoll) {
        return new PrecisionTransform(x, y, z, newPitch, newYaw, newRoll, scaleX, scaleY, scaleZ);
    }

    public PrecisionTransform withPitch(float newPitch) {
        return withRotation(newPitch, yaw, roll);
    }

    public PrecisionTransform withYaw(float newYaw) {
        return withRotation(pitch, newYaw, roll);
    }

    public PrecisionTransform withRoll(float newRoll) {
        return withRotation(pitch, yaw, newRoll);
    }

    public PrecisionTransform rotated(float pitchDelta, float yawDelta, float rollDelta) {
        return withRotation(pitch + pitchDelta, yaw + yawDelta, roll + rollDelta);
    }

    public PrecisionTransform withScale(float uniformScale) {
        return withScale(uniformScale, uniformScale, uniformScale);
    }

    public PrecisionTransform withScale(float newScaleX, float newScaleY, float newScaleZ) {
        return new PrecisionTransform(x, y, z, pitch, yaw, roll, newScaleX, newScaleY, newScaleZ);
    }

    public double squaredDistanceTo(Vec3d point) {
        Objects.requireNonNull(point, "point");
        final double deltaX = x - point.x;
        final double deltaY = y - point.y;
        final double deltaZ = z - point.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public NbtCompound writeNbt() {
        final NbtCompound nbt = new NbtCompound();
        nbt.putDouble(POSITION_X_KEY, x);
        nbt.putDouble(POSITION_Y_KEY, y);
        nbt.putDouble(POSITION_Z_KEY, z);
        nbt.putFloat(PITCH_KEY, pitch);
        nbt.putFloat(YAW_KEY, yaw);
        nbt.putFloat(ROLL_KEY, roll);
        nbt.putFloat(SCALE_X_KEY, scaleX);
        nbt.putFloat(SCALE_Y_KEY, scaleY);
        nbt.putFloat(SCALE_Z_KEY, scaleZ);
        return nbt;
    }

    public static PrecisionTransform fromNbt(NbtCompound nbt) {
        Objects.requireNonNull(nbt, "nbt");

        final float storedScaleX = nbt.contains(SCALE_X_KEY) ? nbt.getFloat(SCALE_X_KEY) : 1.0F;
        final float storedScaleY = nbt.contains(SCALE_Y_KEY) ? nbt.getFloat(SCALE_Y_KEY) : 1.0F;
        final float storedScaleZ = nbt.contains(SCALE_Z_KEY) ? nbt.getFloat(SCALE_Z_KEY) : 1.0F;

        return new PrecisionTransform(
                nbt.getDouble(POSITION_X_KEY),
                nbt.getDouble(POSITION_Y_KEY),
                nbt.getDouble(POSITION_Z_KEY),
                nbt.getFloat(PITCH_KEY),
                nbt.getFloat(YAW_KEY),
                nbt.getFloat(ROLL_KEY),
                storedScaleX,
                storedScaleY,
                storedScaleZ
        );
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requirePositiveFinite(float value, String name) {
        requireFinite(value, name);
        if (value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}
