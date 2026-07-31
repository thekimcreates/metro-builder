package dev.metrobuilder.entity;

import dev.metrobuilder.MetroBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class MetroBuilderEntities {
    public static final EntityType<PrecisionPSDEntity> PRECISION_PSD = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MetroBuilder.MOD_ID, "precision_psd"),
            FabricEntityTypeBuilder.<PrecisionPSDEntity>create(SpawnGroup.MISC, PrecisionPSDEntity::new)
                    .dimensions(EntityDimensions.fixed(3.0f, 3.0f))
                    .trackRangeBlocks(96)
                    .trackedUpdateRate(1)
                    .build()
    );
    private MetroBuilderEntities() {}
    public static void register() {}
}
