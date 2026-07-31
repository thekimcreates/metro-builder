package dev.thekimcreates.metrobuilder.command;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import dev.thekimcreates.metrobuilder.network.PrecisionNetworking;
import dev.thekimcreates.metrobuilder.precision.PrecisionManager;
import dev.thekimcreates.metrobuilder.precision.PrecisionSaveData;
import dev.thekimcreates.metrobuilder.precision.PrecisionSelectionManager;
import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDManager;
import dev.thekimcreates.metrobuilder.psd.PSDObject;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

/** Diagnostic commands used while MetroBuilder is under active development. */
public final class MetroBuilderCommands {
    private MetroBuilderCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("metrobuilder")
                        .then(literal("version")
                                .executes(context -> {
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("MetroBuilder " + MetroBuilder.VERSION),
                                            false
                                    );
                                    return 1;
                                }))
                        .then(literal("precision")
                                .then(literal("status")
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
                                        })))
                        .then(literal("psd")
                                .then(literal("create")
                                        .executes(context -> createTestPsd(
                                                context.getSource().getPlayerOrThrow()
                                        )))
                                .then(literal("status")
                                        .executes(context -> showPsdStatus(
                                                context.getSource().getWorld(),
                                                context.getSource()
                                        )))
                                .then(literal("clear")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> clearPsds(
                                                context.getSource().getWorld(),
                                                context.getSource()
                                        ))))));
    }

    private static int createTestPsd(ServerPlayerEntity player) {
        final ServerWorld world = player.getServerWorld();
        final PrecisionTransform transform = PrecisionTransform
                .at(player.getPos())
                .withYaw(player.getYaw());
        final PSDObject psd = PSDManager.create(world, transform);

        PrecisionNetworking.broadcastSnapshot(world);
        player.sendMessage(
                Text.literal(
                        "Created PSD " + psd.id()
                                + " | pack=" + psd.packId()
                                + " | position=" + formatPosition(psd.transform())
                                + " | yaw=" + psd.transform().yaw()
                ),
                false
        );
        return 1;
    }

    private static int showPsdStatus(
            ServerWorld world,
            net.minecraft.server.command.ServerCommandSource source
    ) {
        final int count = PSDManager.count(world);
        source.sendFeedback(
                () -> Text.literal(
                        "PSD core READY | dimension=" + world.getRegistryKey().getValue()
                                + " | psds=" + count
                                + " | defaultPack=" + PSDObject.DEFAULT_PACK_ID
                ),
                false
        );
        return 1;
    }

    private static int clearPsds(
            ServerWorld world,
            net.minecraft.server.command.ServerCommandSource source
    ) {
        final int removed = PSDManager.clear(world);
        PrecisionSelectionManager.clearAll();
        PrecisionNetworking.broadcastSnapshot(world);
        source.sendFeedback(
                () -> Text.literal("Removed " + removed + " precision PSD object(s)"),
                true
        );
        return 1;
    }

    private static String formatPosition(PrecisionTransform transform) {
        return String.format(
                java.util.Locale.ROOT,
                "%.3f, %.3f, %.3f",
                transform.x(),
                transform.y(),
                transform.z()
        );
    }
}
