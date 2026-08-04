package dev.thekimcreates.metrobuilder.client.screen;

import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDDisplayProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleConsumer;

/** Position editor with a drag-and-drop 1280x256 PNG header uploader. */
public final class PSDPropertiesScreen extends Screen {
    private final Identifier packId;
    private final PrecisionTransform initialTransform;
    private final SaveCallback saveCallback;
    private TextFieldWidget xField, yField, zField, rotationField;
    private byte[] uploadedPng;
    private String uploadedName;
    private String message = "";
    private int dropX, dropY, dropWidth, dropHeight;

    public PSDPropertiesScreen(String title, Identifier packId, PrecisionTransform transform,
                               PSDDisplayProperties properties, SaveCallback saveCallback) {
        this(title, packId, transform, properties, saveCallback, null, null);
    }

    public PSDPropertiesScreen(String title, Identifier packId, PrecisionTransform transform,
                               PSDDisplayProperties properties, SaveCallback saveCallback,
                               Runnable deleteCallback) {
        this(title, packId, transform, properties, saveCallback, deleteCallback, null);
    }

    public PSDPropertiesScreen(String title, Identifier packId, PrecisionTransform transform,
                               PSDDisplayProperties properties, SaveCallback saveCallback,
                               Runnable deleteCallback, DoubleConsumer doorValueCallback) {
        super(Text.literal(title));
        this.packId = Objects.requireNonNull(packId);
        this.initialTransform = Objects.requireNonNull(transform);
        this.saveCallback = Objects.requireNonNull(saveCallback);
        this.uploadedPng = Objects.requireNonNull(properties).headerPng();
        this.uploadedName = properties.hasHeaderPng() ? "Uploaded header.png" : "No header uploaded";
    }

    @Override
    protected void init() {
        final int panelWidth = Math.min(650, width - 20);
        final int panelLeft = (width - panelWidth) / 2;
        final int top = Math.max(12, (height - 285) / 2);
        final int leftX = panelLeft + 20;
        final int fieldX = leftX + 78;
        final int rightX = panelLeft + panelWidth / 2 + 15;
        final int fieldWidth = panelWidth / 2 - 115;

        xField = field(fieldX, top + 58, fieldWidth, initialTransform.x());
        yField = field(fieldX, top + 84, fieldWidth, initialTransform.y());
        zField = field(fieldX, top + 110, fieldWidth, initialTransform.z());
        rotationField = field(fieldX, top + 136, fieldWidth, initialTransform.yaw());

        dropX = rightX;
        dropY = top + 58;
        dropWidth = panelLeft + panelWidth - 20 - rightX;
        dropHeight = 112;
        final int actionY = top + 245;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(width / 2 - 96, actionY, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 + 6, actionY, 90, 20).build());
        setInitialFocus(xField);
    }

    private TextFieldWidget field(int x, int y, int width, double value) {
        final TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 20, Text.empty());
        field.setText(String.format(Locale.ROOT, "%.3f", value));
        field.setMaxLength(32);
        return addDrawableChild(field);
    }

    @Override
    public void filesDragged(List<Path> paths) {
        if (paths.size() != 1) {
            message = "Drop exactly one PNG file";
            return;
        }
        loadHeader(paths.get(0));
    }

    private void loadHeader(Path path) {
        try {
            if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
                throw new IllegalArgumentException("File must be a PNG");
            }
            final byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > PSDDisplayProperties.MAX_HEADER_BYTES) {
                throw new IllegalArgumentException("PNG must be 2 MiB or smaller");
            }
            try (NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes))) {
                if (image.getWidth() != 1280 || image.getHeight() != 256) {
                    throw new IllegalArgumentException("PNG must be exactly 1280x256 pixels");
                }
            }
            uploadedPng = bytes;
            uploadedName = path.getFileName().toString();
            message = "Header ready to save";
        } catch (Exception exception) {
            message = exception.getMessage() == null ? "Could not read PNG" : exception.getMessage();
        }
    }

    private void save() {
        try {
            final PrecisionTransform transform = new PrecisionTransform(
                    Double.parseDouble(xField.getText()), Double.parseDouble(yField.getText()),
                    Double.parseDouble(zField.getText()), initialTransform.pitch(),
                    Float.parseFloat(rotationField.getText()), initialTransform.roll(),
                    initialTransform.scaleX(), initialTransform.scaleY(), initialTransform.scaleZ());
            saveCallback.save(packId, transform, new PSDDisplayProperties(uploadedPng));
            close();
        } catch (IllegalArgumentException exception) {
            message = "Enter valid finite positioning values";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        final int panelWidth = Math.min(650, width - 20);
        final int left = (width - panelWidth) / 2;
        final int top = Math.max(12, (height - 285) / 2);
        context.fill(left, top, left + panelWidth, top + 280, 0xEE101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 12, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Positioning", left + 20, top + 36, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Upload Header", left + panelWidth / 2 + 15,
                top + 36, 0xFFFFFF);
        drawLabel(context, "X", left + 20, top + 64);
        drawLabel(context, "Y", left + 20, top + 90);
        drawLabel(context, "Z", left + 20, top + 116);
        drawLabel(context, "Rotation", left + 20, top + 142);

        final boolean hover = mouseX >= dropX && mouseX <= dropX + dropWidth
                && mouseY >= dropY && mouseY <= dropY + dropHeight;
        context.fill(dropX, dropY, dropX + dropWidth, dropY + dropHeight,
                hover ? 0xFF383838 : 0xFF242424);
        context.drawBorder(dropX, dropY, dropWidth, dropHeight, hover ? 0xFFFFFFFF : 0xFF777777);
        context.drawCenteredTextWithShadow(textRenderer, "Drop a PNG file here",
                dropX + dropWidth / 2, dropY + 31, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Required: 1280 x 256 px",
                dropX + dropWidth / 2, dropY + 51, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, uploadedName,
                dropX + dropWidth / 2, dropY + 77, uploadedPng.length > 0 ? 0x55FF55 : 0xAAAAAA);
        if (!message.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, message, width / 2, top + 222,
                    message.contains("ready") ? 0x55FF55 : 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLabel(DrawContext context, String label, int x, int y) {
        context.drawTextWithShadow(textRenderer, label, x, y, 0xD0D0D0);
    }

    @Override public boolean shouldPause() { return false; }

    @FunctionalInterface
    public interface SaveCallback {
        void save(Identifier packId, PrecisionTransform transform, PSDDisplayProperties properties);
    }
}
