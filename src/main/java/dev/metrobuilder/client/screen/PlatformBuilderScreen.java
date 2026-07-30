package dev.metrobuilder.client.screen;

import dev.metrobuilder.network.MetroBuilderNetworking;
import dev.metrobuilder.platform.PlatformDesignManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlatformBuilderScreen extends Screen {
    private final List<String> originalRows;
    private final List<String> editingRows;
    private final List<TextFieldWidget> fields = new ArrayList<>();
    private int scrollOffset;
    private static final int ROW_HEIGHT = 34;

    public PlatformBuilderScreen(List<String> rows) {
        super(Text.literal("Platform Builder"));
        this.originalRows = new ArrayList<>(rows);
        this.editingRows = new ArrayList<>(rows);
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();
        fields.clear();

        int left = 28;
        int top = 58;
        int visibleRows = Math.max(1, (height - 140) / ROW_HEIGHT);
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, editingRows.size() - visibleRows)));

        for (int visible = 0; visible < visibleRows; visible++) {
            int index = scrollOffset + visible;
            if (index >= editingRows.size()) break;
            int y = top + visible * ROW_HEIGHT;

            final int rowIndex = index;
            addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> move(rowIndex, -1)).dimensions(left, y, 22, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> move(rowIndex, 1)).dimensions(left + 25, y, 22, 20).build());

            TextFieldWidget field = new TextFieldWidget(textRenderer, left + 54, y, Math.max(180, width / 3), 20, Text.literal("Block"));
            field.setText(editingRows.get(index));
            field.setMaxLength(128);
            field.setChangedListener(value -> editingRows.set(rowIndex, normalize(value)));
            addDrawableChild(field);
            fields.add(field);

            addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> remove(rowIndex))
                    .dimensions(left + 62 + Math.max(180, width / 3), y, 62, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add Row"), b -> addRow())
                .dimensions(left, height - 72, 92, 20).build());

        if (scrollOffset > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Previous"), b -> { scrollOffset--; rebuildWidgets(); })
                    .dimensions(left + 100, height - 72, 72, 20).build());
        }
        if (scrollOffset + visibleRows < editingRows.size()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Next"), b -> { scrollOffset++; rebuildWidgets(); })
                    .dimensions(left + 176, height - 72, 60, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> cancelAndClose())
                .dimensions(20, height - 32, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveAndClose())
                .dimensions(width - 110, height - 32, 90, 20).build());
    }

    private String normalize(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.contains(":") || trimmed.isEmpty() ? trimmed : "minecraft:" + trimmed;
    }

    private void move(int index, int direction) {
        int target = index + direction;
        if (target < 0 || target >= editingRows.size()) return;
        String value = editingRows.remove(index);
        editingRows.add(target, value);
        rebuildWidgets();
    }

    private void remove(int index) {
        if (editingRows.size() <= 1) return;
        editingRows.remove(index);
        rebuildWidgets();
    }

    private void addRow() {
        if (editingRows.size() >= PlatformDesignManager.MAX_ROWS) return;
        editingRows.add("minecraft:stone_bricks");
        scrollOffset = Math.max(0, editingRows.size() - Math.max(1, (height - 140) / ROW_HEIGHT));
        rebuildWidgets();
    }

    private void cancelAndClose() {
        editingRows.clear();
        editingRows.addAll(originalRows);
        close();
    }

    private void saveAndClose() {
        List<String> valid = new ArrayList<>();
        for (String row : editingRows) {
            Identifier id = Identifier.tryParse(normalize(row));
            if (id != null && Registries.BLOCK.containsId(id)) valid.add(id.toString());
        }
        if (valid.isEmpty()) return;

        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(valid.size());
        for (String row : valid) buf.writeString(row);
        ClientPlayNetworking.send(MetroBuilderNetworking.SAVE_PLATFORM_DESIGN, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Top = left side of platform", 28, 38, 0xB8D7FF);
        context.drawTextWithShadow(textRenderer, "Bottom = right side of platform", 28, 49, 0xB8D7FF);
        context.drawTextWithShadow(textRenderer, "Top view", width - 150, 40, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Width: " + editingRows.size() + " rows", width - 150, 52, 0xA0A0A0);

        int previewX = width - 118;
        int previewY = 72;
        int previewSize = Math.max(16, Math.min(28, (height - 150) / Math.max(1, editingRows.size())));
        for (int i = 0; i < editingRows.size(); i++) {
            Identifier id = Identifier.tryParse(normalize(editingRows.get(i)));
            Block block = id != null && Registries.BLOCK.containsId(id) ? Registries.BLOCK.get(id) : null;
            int y = previewY + i * previewSize;
            context.fill(previewX - 3, y - 2, previewX + previewSize + 3, y + previewSize + 2, 0x66000000);
            if (block != null) {
                ItemStack stack = new ItemStack(block.asItem());
                context.drawItem(stack, previewX + (previewSize - 16) / 2, y + (previewSize - 16) / 2);
            }
        }

        super.render(context, mouseX, mouseY, delta);
        renderSuggestions(context);
    }

    private void renderSuggestions(DrawContext context) {
        for (TextFieldWidget field : fields) {
            if (!field.isFocused()) continue;
            String query = normalize(field.getText());
            if (query.isBlank()) continue;
            int shown = 0;
            int x = field.getX();
            int y = field.getY() + 22;
            for (Identifier id : Registries.BLOCK.getIds()) {
                if (!id.toString().contains(query.replace("minecraft:", ""))) continue;
                context.fill(x, y + shown * 12, x + field.getWidth(), y + shown * 12 + 12, 0xE0101010);
                context.drawTextWithShadow(textRenderer, id.toString(), x + 3, y + 2 + shown * 12, 0xFFFFFF);
                shown++;
                if (shown >= 4) break;
            }
            break;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
