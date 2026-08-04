package dev.thekimcreates.metrobuilder.client.psd;

import dev.thekimcreates.metrobuilder.client.model.obj.ObjMeshCache;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;

final class SingleGlassPanelRenderer {
    static final String PACK_ID = "metrobuilder:seoul_bulky_glass_panel";
    private static final class_2960 WHITE_MODEL = id("panel_white.obj");
    private static final class_2960 DARK_MODEL = id("panel_dark.obj");
    private static final class_2960 GLASS_MODEL = id("panel_glass.obj");
    private static final class_2960 WHITE_TEXTURE = texture("white_metal.png");
    private static final class_2960 DARK_TEXTURE = texture("dark_frame.png");
    private static final class_2960 GLASS_TEXTURE = texture("glass.png");

    private SingleGlassPanelRenderer() {}

    static boolean renderIfPanel(class_4587 matrices, class_4597 consumers,
                                 ClientPSDObject psd, int light) {
        if (!PACK_ID.equals(psd.packId().toString())) return false;
        render(matrices, consumers, WHITE_MODEL, WHITE_TEXTURE, light, false);
        render(matrices, consumers, DARK_MODEL, DARK_TEXTURE, light, false);
        render(matrices, consumers, GLASS_MODEL, GLASS_TEXTURE, light, true);
        return true;
    }

    private static void render(class_4587 matrices, class_4597 consumers, class_2960 model,
                               class_2960 texture, int light, boolean translucent) {
        class_4588 vertices = consumers.getBuffer(translucent
                ? class_1921.method_23580(texture) : class_1921.method_23578(texture));
        ObjMeshCache.get(model).render(matrices, vertices, light);
    }

    private static class_2960 id(String file) {
        return new class_2960("metrobuilder", "models/psd/seoul_bulky_glass_panel/" + file);
    }

    private static class_2960 texture(String file) {
        return new class_2960("metrobuilder", "textures/psd/seoul_bulky_white/" + file);
    }
}
