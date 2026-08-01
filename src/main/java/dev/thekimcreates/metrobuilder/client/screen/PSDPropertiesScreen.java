package dev.thekimcreates.metrobuilder.client.screen;

import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDPackRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Objects;

/** Lightweight non-pausing properties panel for a pending or placed PSD. */
public final class PSDPropertiesScreen extends Screen {
    private final PrecisionTransform initialTransform;
    private final SaveCallback saveCallback;
    private final Runnable deleteCallback;

    private Identifier selectedPackId;
    private ButtonWidget packButton;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private TextFieldWidget yawField;
    private String errorMessage = "";

    public PSDPropertiesScreen(
            String title,
            Identifier packId,
            PrecisionTransform initialTransform,
            SaveCallback saveCallback
    ) {
        this(title, packId, initialTransform, saveCallback, null);
    }

    public PSDPropertiesScreen(
            String title,
            Identifier packId,
            PrecisionTransform initialTransform,
            SaveCallback saveCallback,
            Runnable deleteCallback
    ) {
        super(Text.literal(title));
        selectedPackId = Objects.requireNonNull(packId, "packId");
        this.initialTransform = Objects.requireNonNull(initialTransform, "initialTransform");
        this.saveCallback = Objects.requireNonNull(saveCallback, "saveCallback");
        this.deleteCallback = deleteCallback;
    }

    @Override
    protected void init() {
        final int centerX = width / 2;
        final int top = height / 2 - 126;
        final int fieldX = centerX - 72;
        final int fieldWidth = 162;

        packButton = addDrawableChild(ButtonWidget.builder(
                        packButtonText(),
                        button -> cyclePack()
                )
                .dimensions(fieldX, top + 46, fieldWidth, 20)
                .build());

        xField = addField(fieldX, top + 80, fieldWidth, initialTransform.x());
        yField = addField(fieldX, top + 107, fieldWidth, initialTransform.y());
        zField = addField(fieldX, top + 134, fieldWidth, initialTransform.z());
        yawField = addField(fieldX, top + 161, fieldWidth, initialTransform.yaw());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(centerX - 92, top + 201, 86, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(centerX + 6, top + 201, 86, 20)
                .build());

        if (deleteCallback != null) {
            addDrawableChild(ButtonWidget.builder(
                            Text.literal("Delete PSD").formatted(Formatting.RED),
                            button -> deletePsd()
                    )
                    .dimensions(centerX - 92, top + 225, 184, 20)
                    .build());
        }

        setInitialFocus(xField);
    }

    private TextFieldWidget addField(int x, int y, int width, double value) {
        final TextFieldWidget field = new TextFieldWidget(
                textRenderer,
                x,
                y,
                width,
                20,
                Text.empty()
        );
        field.setMaxLength(32);
        field.setText(String.format(Locale.ROOT, "%.3f", value));
        addDrawableChild(field);
        return field;
    }

    private void cyclePack() {
        selectedPackId = PSDPackRegistry.next(selectedPackId);
        packButton.setMessage(packButtonText());
    }

    private Text packButtonText() {
        return Text.literal(PSDPackRegistry.displayName(selectedPackId));
    }

    private void save() {
        try {
            final PrecisionTransform updated = new PrecisionTransform(
                    Double.parseDouble(xField.getText()),
                    Double.parseDouble(yField.getText()),
                    Double.parseDouble(zField.getText()),
                    initialTransform.pitch(),
                    Float.parseFloat(yawField.getText()),
                    initialTransform.roll(),
                    initialTransform.scaleX(),
                    initialTransform.scaleY(),
                    initialTransform.scaleZ()
            );
            saveCallback.save(selectedPackId, updated);
            close();
        } catch (IllegalArgumentException exception) {
            errorMessage = "Enter valid finite numbers";
        }
    }

    private void deletePsd() {
        if (deleteCallback == null) {
            return;
        }
        deleteCallback.run();
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        final int centerX = width / 2;
        final int top = height / 2 - 126;

        final int panelBottom = deleteCallback == null ? top + 235 : top + 259;
        context.fill(centerX - 112, top - 16, centerX + 112, panelBottom, 0xE0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, top - 7, 0xFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer, "PSD Pack", centerX, top + 31, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, "X", centerX - 96, top + 86, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, "Y", centerX - 96, top + 113, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, "Z", centerX - 96, top + 140, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, "Yaw", centerX - 104, top + 167, 0xD0D0D0);

        if (!errorMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(errorMessage),
                    centerX,
                    top + 188,
                    0xFF5555
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @FunctionalInterface
    public interface SaveCallback {
        void save(Identifier packId, PrecisionTransform transform);
    }
}
