package dev.thekimcreates.metrobuilder.precision;

import net.minecraft.nbt.NbtCompound;

import java.util.Objects;
import java.util.UUID;

/** Base class for every server-authoritative MetroBuilder precision object. */
public abstract class PrecisionObject {
    private static final String TYPE_KEY = "Type";
    private static final String ID_KEY = "Id";
    private static final String REVISION_KEY = "Revision";
    private static final String TRANSFORM_KEY = "Transform";
    private static final String DATA_KEY = "Data";

    private final UUID id;
    private final PrecisionObjectType<? extends PrecisionObject> type;
    private PrecisionTransform transform;
    private long revision;

    protected PrecisionObject(
            PrecisionObjectType<? extends PrecisionObject> type,
            UUID id,
            PrecisionTransform transform
    ) {
        this(type, id, transform, 0L);
    }

    protected PrecisionObject(
            PrecisionObjectType<? extends PrecisionObject> type,
            UUID id,
            PrecisionTransform transform,
            long revision
    ) {
        this.type = Objects.requireNonNull(type, "type");
        this.id = Objects.requireNonNull(id, "id");
        this.transform = Objects.requireNonNull(transform, "transform");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
        this.revision = revision;
    }

    public final UUID id() {
        return id;
    }

    public final PrecisionObjectType<? extends PrecisionObject> type() {
        return type;
    }

    public final PrecisionTransform transform() {
        return transform;
    }

    public final long revision() {
        return revision;
    }

    /**
     * Replaces the immutable transform and increments this object's revision.
     *
     * @return {@code true} when the transform changed
     */
    final boolean replaceTransform(PrecisionTransform newTransform) {
        Objects.requireNonNull(newTransform, "newTransform");
        if (transform.equals(newTransform)) {
            return false;
        }

        transform = newTransform;
        revision++;
        return true;
    }

    public final NbtCompound writeNbt() {
        final NbtCompound root = new NbtCompound();
        root.putString(TYPE_KEY, type.id().toString());
        root.putString(ID_KEY, id.toString());
        root.putLong(REVISION_KEY, revision);
        root.put(TRANSFORM_KEY, transform.writeNbt());

        final NbtCompound data = new NbtCompound();
        writeData(data);
        root.put(DATA_KEY, data);
        return root;
    }

    /** Writes only fields owned by the concrete precision-object type. */
    protected abstract void writeData(NbtCompound data);

    static String typeKey() {
        return TYPE_KEY;
    }

    static String idKey() {
        return ID_KEY;
    }

    static String revisionKey() {
        return REVISION_KEY;
    }

    static String transformKey() {
        return TRANSFORM_KEY;
    }

    static String dataKey() {
        return DATA_KEY;
    }
}
