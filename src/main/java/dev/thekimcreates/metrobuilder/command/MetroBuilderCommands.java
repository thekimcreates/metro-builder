package dev.thekimcreates.metrobuilder.command;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

/** Small diagnostic command used to verify that the clean rebuild loaded correctly. */
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
                                }))));
    }
}
