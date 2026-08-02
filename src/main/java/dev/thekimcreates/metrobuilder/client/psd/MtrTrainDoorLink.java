package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.OptionalDouble;

/** Optional reflection bridge for following the nearest MTR train's door animation. */
final class MtrTrainDoorLink {
    private static final double LINK_DISTANCE = 5.0D;
    private static final double LINK_DISTANCE_SQUARED = LINK_DISTANCE * LINK_DISTANCE;

    private static boolean initializationAttempted;
    private static Method getClientData;
    private static Field vehiclesField;

    private MtrTrainDoorLink() {
    }

    static OptionalDouble findDoorValue(MinecraftClient client, ClientPSDObject psd) {
        if (!FabricLoader.getInstance().isModLoaded("mtr")) {
            return OptionalDouble.empty();
        }

        final double x = psd.transform().x();
        final double y = psd.transform().y() + 1.0D;
        final double z = psd.transform().z();

        final LinkedDoorValue modernValue = findModernMtrValue(x, y, z);
        if (modernValue.found) {
            return OptionalDouble.of(modernValue.value);
        }

        return findLegacyEntityValue(client, x, y, z);
    }

    private static LinkedDoorValue findModernMtrValue(double x, double y, double z) {
        initializeModernBridge();
        if (getClientData == null || vehiclesField == null) {
            return LinkedDoorValue.NOT_FOUND;
        }

        try {
            final Object clientData = getClientData.invoke(null);
            final Object vehicles = vehiclesField.get(clientData);
            if (!(vehicles instanceof Iterable<?> iterable)) {
                return LinkedDoorValue.NOT_FOUND;
            }

            double nearestDistance = LINK_DISTANCE_SQUARED;
            double nearestDoorValue = 0.0D;
            boolean found = false;
            for (Object vehicle : iterable) {
                final OptionalDouble doorValue = readVehicleDoorValue(vehicle);
                if (doorValue.isEmpty()) {
                    continue;
                }
                final double distance = nearestVehiclePositionSquared(vehicle, x, y, z);
                if (distance <= nearestDistance) {
                    nearestDistance = distance;
                    nearestDoorValue = clamp(doorValue.getAsDouble());
                    found = true;
                }
            }
            return found ? new LinkedDoorValue(true, nearestDoorValue) : LinkedDoorValue.NOT_FOUND;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disableModernBridge(exception);
            return LinkedDoorValue.NOT_FOUND;
        }
    }

    private static OptionalDouble findLegacyEntityValue(
            MinecraftClient client,
            double x,
            double y,
            double z
    ) {
        if (client.world == null) {
            return OptionalDouble.empty();
        }
        final Box searchBox = new Box(
                x - LINK_DISTANCE,
                y - 4.0D,
                z - LINK_DISTANCE,
                x + LINK_DISTANCE,
                y + 4.0D,
                z + LINK_DISTANCE
        );
        Entity nearest = null;
        double nearestDistance = LINK_DISTANCE_SQUARED;
        for (Entity entity : client.world.getOtherEntities(null, searchBox)) {
            final String className = entity.getClass().getName().toLowerCase();
            if (!className.contains("mtr") || !className.contains("train")) {
                continue;
            }
            final double distance = entity.squaredDistanceTo(x, y, z);
            if (distance < nearestDistance && readNumber(entity, "getDoorValue", "doorValue").isPresent()) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest == null
                ? OptionalDouble.empty()
                : readNumber(nearest, "getDoorValue", "doorValue").stream()
                        .map(MtrTrainDoorLink::clamp)
                        .findFirst();
    }

    private static void initializeModernBridge() {
        if (initializationAttempted) {
            return;
        }
        initializationAttempted = true;
        try {
            final Class<?> clientDataClass = Class.forName("org.mtr.mod.client.MinecraftClientData");
            getClientData = clientDataClass.getMethod("getInstance");
            vehiclesField = clientDataClass.getField("vehicles");
            MetroBuilder.LOGGER.info("MTR train-door bridge initialized");
        } catch (ReflectiveOperationException exception) {
            MetroBuilder.LOGGER.info("Modern MTR client data is unavailable; using legacy entity detection");
        }
    }

    private static OptionalDouble readVehicleDoorValue(Object vehicle) {
        try {
            final Field dataField = vehicle.getClass().getField("persistentVehicleData");
            return readNumber(dataField.get(vehicle), "getDoorValue", "doorValue");
        } catch (ReflectiveOperationException exception) {
            return readNumber(vehicle, "getDoorValue", "doorValue");
        }
    }

    private static double nearestVehiclePositionSquared(Object vehicle, double x, double y, double z) {
        try {
            final Method positionsMethod = vehicle.getClass().getMethod("getVehicleCarsAndPositions");
            final Object carPairs = positionsMethod.invoke(vehicle);
            if (!(carPairs instanceof Iterable<?> cars)) {
                return Double.POSITIVE_INFINITY;
            }

            double nearest = Double.POSITIVE_INFINITY;
            for (Object carPair : cars) {
                final Object positionPairs = pairValue(carPair, "right");
                if (!(positionPairs instanceof Iterable<?> positions)) {
                    continue;
                }
                for (Object positionPair : positions) {
                    nearest = Math.min(nearest, vectorDistanceSquared(pairValue(positionPair, "left"), x, y, z));
                    nearest = Math.min(nearest, vectorDistanceSquared(pairValue(positionPair, "right"), x, y, z));
                }
            }
            return nearest;
        } catch (ReflectiveOperationException exception) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static Object pairValue(Object pair, String side) throws ReflectiveOperationException {
        return pair.getClass().getMethod(side).invoke(pair);
    }

    private static double vectorDistanceSquared(Object vector, double x, double y, double z) {
        if (vector == null) {
            return Double.POSITIVE_INFINITY;
        }
        try {
            final double vectorX = ((Number) vector.getClass().getField("x").get(vector)).doubleValue();
            final double vectorY = ((Number) vector.getClass().getField("y").get(vector)).doubleValue();
            final double vectorZ = ((Number) vector.getClass().getField("z").get(vector)).doubleValue();
            final double deltaX = vectorX - x;
            final double deltaY = vectorY - y;
            final double deltaZ = vectorZ - z;
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static OptionalDouble readNumber(Object object, String methodName, String fieldName) {
        if (object == null) {
            return OptionalDouble.empty();
        }
        try {
            final Object value = object.getClass().getMethod(methodName).invoke(object);
            return value instanceof Number number
                    ? OptionalDouble.of(number.doubleValue())
                    : OptionalDouble.empty();
        } catch (ReflectiveOperationException ignored) {
            try {
                final Object value = object.getClass().getField(fieldName).get(object);
                return value instanceof Number number
                        ? OptionalDouble.of(number.doubleValue())
                        : OptionalDouble.empty();
            } catch (ReflectiveOperationException ignoredAgain) {
                return OptionalDouble.empty();
            }
        }
    }

    private static void disableModernBridge(Exception exception) {
        getClientData = null;
        vehiclesField = null;
        MetroBuilder.LOGGER.warn("MTR train-door bridge disabled after an incompatible API response", exception);
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private record LinkedDoorValue(boolean found, double value) {
        private static final LinkedDoorValue NOT_FOUND = new LinkedDoorValue(false, 0.0D);
    }
}
