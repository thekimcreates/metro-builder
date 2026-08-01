package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.MetroBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Uses TJMetro's own runtime model object for the BMT PSD door.
 *
 * <p>This intentionally uses reflection so MetroBuilder can still compile and
 * launch without bundling MTR or TJMetro. When both mods are installed, the
 * exact {@code RenderPSDDoorTianjinBMT.MODEL_PSD} model is rendered with the
 * original MTR graphics path. This removes all guessed UV and cuboid logic.</p>
 */
final class TJMetroRuntimeDoorBridge {
    private static final String[] RENDERER_CLASS_NAMES = {
            "fabric.ziyue.tjmetro.mod.render.RenderPSDDoorTianjinBMT",
            "ziyue.tjmetro.mod.render.RenderPSDDoorTianjinBMT"
    };

    private static boolean initialized;
    private static boolean available;
    private static Object modelPsd;
    private static Constructor<?> mtrIdentifierConstructor;
    private static Method getEntityCutout;
    private static Method createGraphicsHolder;
    private static Method createVertexConsumer;
    private static Method renderModel;

    private TJMetroRuntimeDoorBridge() {
    }

    static boolean renderQuarter(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Identifier texture,
            float centerX,
            float baseY
    ) {
        initialize();
        if (!available) {
            return false;
        }

        matrices.push();
        try {
            matrices.translate(centerX, baseY, 0.0F);
            // This is exactly the transform used by TJMetro's block-entity renderer.
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));

            final Object mtrIdentifier = mtrIdentifierConstructor.newInstance(texture.toString());
            final Object mtrRenderLayer = getEntityCutout.invoke(null, mtrIdentifier);

            final Consumer<Object> renderCallback = graphicsHolder -> {
                try {
                    createVertexConsumer.invoke(graphicsHolder, mtrRenderLayer);
                    renderModel.invoke(
                            modelPsd,
                            graphicsHolder,
                            LightmapTextureManager.MAX_LIGHT_COORDINATE,
                            OverlayTexture.DEFAULT_UV,
                            1.0F,
                            1.0F,
                            1.0F,
                            1.0F
                    );
                } catch (ReflectiveOperationException exception) {
                    throw new RuntimeException(exception);
                }
            };

            createGraphicsHolder.invoke(null, matrices, consumers, renderCallback);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable("TJMetro exact PSD model rendering failed", exception);
            return false;
        } finally {
            matrices.pop();
        }
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            final Class<?> rendererClass = findClass(RENDERER_CLASS_NAMES);
            final Class<?> graphicsHolderClass = Class.forName("org.mtr.mapping.mapper.GraphicsHolder");
            final Class<?> mtrIdentifierClass = Class.forName("org.mtr.mapping.holder.Identifier");
            final Class<?> mtrRenderLayerClass = Class.forName("org.mtr.mapping.holder.RenderLayer");

            final Field modelField = rendererClass.getDeclaredField("MODEL_PSD");
            modelField.setAccessible(true);
            modelPsd = modelField.get(null);

            mtrIdentifierConstructor = mtrIdentifierClass.getConstructor(String.class);
            getEntityCutout = mtrRenderLayerClass.getMethod("getEntityCutout", mtrIdentifierClass);

            createGraphicsHolder = Arrays.stream(graphicsHolderClass.getMethods())
                    .filter(method -> method.getName().equals("createInstanceSafe"))
                    .filter(method -> Modifier.isStatic(method.getModifiers()))
                    .filter(method -> method.getParameterCount() == 3)
                    .filter(method -> Consumer.class.isAssignableFrom(method.getParameterTypes()[2]))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("GraphicsHolder.createInstanceSafe(MatrixStack, VertexConsumerProvider, Consumer)"));

            createVertexConsumer = graphicsHolderClass.getMethod("createVertexConsumer", mtrRenderLayerClass);
            renderModel = Arrays.stream(modelPsd.getClass().getMethods())
                    .filter(method -> method.getName().equals("render"))
                    .filter(method -> method.getParameterCount() == 7)
                    .filter(method -> method.getParameterTypes()[0].equals(graphicsHolderClass))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("TJMetro ModelSingleCube.render"));

            available = true;
            MetroBuilder.LOGGER.info("Using TJMetro's exact BMT PSD door model at runtime");
        } catch (ReflectiveOperationException | LinkageError exception) {
            disable("TJMetro exact PSD model is unavailable; using MetroBuilder fallback", exception);
        }
    }

    private static Class<?> findClass(String[] names) throws ClassNotFoundException {
        ClassNotFoundException lastException = null;
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? new ClassNotFoundException("TJMetro PSD renderer") : lastException;
    }

    private static void disable(String message, Throwable exception) {
        available = false;
        MetroBuilder.LOGGER.warn(message, exception);
    }
}
