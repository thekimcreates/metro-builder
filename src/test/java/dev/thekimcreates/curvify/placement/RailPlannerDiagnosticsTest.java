package dev.thekimcreates.curvify.placement;

import org.junit.jupiter.api.Test;
import org.mtr.core.data.Rail;
import org.mtr.core.serializer.MessagePackReader;
import org.mtr.libraries.org.msgpack.core.MessagePack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class RailPlannerDiagnosticsTest {
    @Test
    void scansExistingRailsWhenRequested() throws Exception {
        final String railDirectory = System.getenv("CURVIFY_RAIL_DIRECTORY");
        assumeTrue(railDirectory != null);
        final List<String> failures = new ArrayList<>();
        final List<PlacementConfig> configurations = List.of(
                PlacementConfig.DEFAULT,
                new PlacementConfig(PlacementConfig.DEFAULT.pattern(), PlacementConfig.DEFAULT.platformBlockItemId(), false, true),
                new PlacementConfig(PlacementConfig.DEFAULT.pattern(), PlacementConfig.DEFAULT.platformBlockItemId(), true, false),
                new PlacementConfig(PlacementConfig.DEFAULT.pattern(), PlacementConfig.DEFAULT.platformBlockItemId(), true, true),
                new PlacementConfig(List.of(PsdType.values()), PlacementConfig.DEFAULT.platformBlockItemId(), false, false)
        );
        int total = 0;
        try (var paths = Files.walk(Path.of(railDirectory))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                total++;
                try (var inputStream = Files.newInputStream(path);
                     var unpacker = MessagePack.newDefaultUnpacker(inputStream)) {
                    final Rail rail = new Rail(new MessagePackReader(unpacker));
                    for (int configIndex = 0; configIndex < configurations.size(); configIndex++) {
                        try {
                            RailPlacementPlanner.create(rail, configurations.get(configIndex));
                        } catch (RuntimeException exception) {
                            failures.add(path.getFileName() + " config " + configIndex + ": " + exception.getMessage());
                        }
                    }
                } catch (RuntimeException exception) {
                    failures.add(path.getFileName() + " read: " + exception.getMessage());
                }
            }
        }
        System.out.printf("Scanned %d rails; %d planner failures%n", total, failures.size());
        failures.stream().limit(40).forEach(System.out::println);
        assertTrue(failures.isEmpty(), "Planner failed for " + failures.size() + " rails");
    }
}
