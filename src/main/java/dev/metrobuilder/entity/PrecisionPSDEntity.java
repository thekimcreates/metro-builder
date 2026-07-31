package dev.metrobuilder.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.world.World;

import java.util.UUID;

public final class PrecisionPSDEntity extends Entity {
    public static final String TYPE_DOOR = "door";
    public static final String TYPE_GLASS = "glass";

    private String psdType = TYPE_DOOR;
    private float doorValue;
    private boolean preview;
    private UUID assemblyId = UUID.randomUUID();

    public PrecisionPSDEntity(EntityType<? extends PrecisionPSDEntity> type, World world) {
        super(type, world);
        noClip = true;
    }

    @Override protected void initDataTracker() {}

    public String getPsdType() { return psdType; }
    public void setPsdType(String psdType) { this.psdType = TYPE_GLASS.equals(psdType) ? TYPE_GLASS : TYPE_DOOR; }
    public float getDoorValue() { return doorValue; }
    public void setDoorValue(float doorValue) { this.doorValue = Math.max(0, Math.min(1, doorValue)); }
    public boolean isPreview() { return preview; }
    public void setPreview(boolean preview) { this.preview = preview; setGlowing(preview); }
    public UUID getAssemblyId() { return assemblyId; }

    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        psdType = nbt.getString("PsdType");
        if (psdType.isEmpty()) psdType = TYPE_DOOR;
        doorValue = nbt.getFloat("DoorValue");
        preview = nbt.getBoolean("Preview");
        if (nbt.containsUuid("AssemblyId")) assemblyId = nbt.getUuid("AssemblyId");
        setGlowing(preview);
    }

    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("PsdType", psdType);
        nbt.putFloat("DoorValue", doorValue);
        nbt.putBoolean("Preview", preview);
        nbt.putUuid("AssemblyId", assemblyId);
    }

    @Override public boolean isCollidable() { return false; }
    @Override public boolean isAttackable() { return true; }
    @Override public EntitySpawnS2CPacket createSpawnPacket() { return new EntitySpawnS2CPacket(this); }
}
