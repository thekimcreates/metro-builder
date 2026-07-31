package dev.thekimcreates.metrobuilder.command;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.precision.PrecisionManager;
import dev.thekimcreates.metrobuilder.precision.PrecisionSaveData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/** Diagnostic commands used while MetroBuilder is under active development. */
public final class MetroBuilderCommands {
    private MetroBuilderCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("metrobuilder")
                        .then(CommandManager.literal("version")
                                .executes(context -> {
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("MetroBuilder " + MetroBuilder.VERSION),
                                            false
                                    );
                                    return 1;
                                }))
                        .then(CommandManager.literal("precision")
                                .then(CommandManager.literal("status")
                                        .executes(context -> {
                                            final ServerWorld world = context.getSource().getWorld();
                                            final PrecisionManager manager = PrecisionSaveData.get(world).manager();
                                            context.getSource().sendFeedback(
                                                    () -> Text.literal(
                                                            "Precision engine READY | dimension="
                                                                    + world.getRegistryKey().getValue()
                                                                    + " | objects=" + manager.size()
                                                                    + " | preservedUnknown="
                                                                    + manager.preservedUnknownCount()
                                                    ),
                                                    false
                                            );
                                            return 1;
                                        })))));
    }
}
