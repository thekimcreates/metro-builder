package dev.thekimcreates.metrobuilder.client.screen;

import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/** Lightweight non-pausing properties panel for a pending or placed PSD. */
public final class PSDPropertiesScreen extends Screen {
    private final Identifier packId;
    private final PrecisionTransform initialTransform;
    private final Consumer<PrecisionTransform> saveCallback;

    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private TextFieldWidget yawField;
    private String errorMessage = "";

    public PSDPropertiesScreen(
            String title,
            Identifier packId,
            PrecisionTransform initialTransform,
            Consumer<PrecisionTransform> saveCallback
    ) {
        super(Text.literal(title));
        this.packId = Objects.requireNonNull(packId, "packId");
        this.initialTransform = Objects.requireNonNull(initialTransform, "initialTransform");
        this.saveCallback = Objects.requireNonNull(saveCallback, "saveCallback");
    }

    @Override
    protected void init() {
        final int centerX = width / 2;
        final int top = height / 2 - 104;
        final int fieldX = centerX - 72;
        final int fieldWidth = 162;

        xField = addField(fieldX, top + 45, fieldWidth, initialTransform.x());
        yField = addField(fieldX, top + 72, fieldWidth, initialTransform.y());
        zField = addField(fieldX, top + 99, fieldWidth, initialTransform.z());
        yawField = addField(fieldX, top + 126, fieldWidth, initialTransform.yaw());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(centerX - 92, top + 166, 86, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(centerX + 6, top + 166, 86, 20)
                .build());

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
            saveCallback.accept(updated);
            close();
        } catch (IllegalArgumentException exception) {
            errorMessage = "Enter valid finite numbers";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        final int centerX = width / 2;
        final int top = height / 2 - 104;

        context.fill(centerX - 112, top - 16, centerX + 112, top + 200, 0xE0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, top - 7, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Pack: " + packId),
                centerX,
                top + 14,
                0xA0A0A0
        );

        context.drawTextWithShadow(textRenderer, "X", centerX - 96, top + 51, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, "Y", centerX - 96, top + 78, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, "Z", centerX - 96, top + 105, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, "Yaw", centerX - 104, top + 132, 0xD0D0D0);

        if (!errorMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(errorMessage),
                    centerX,
                    top + 151,
                    0xFF5555
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
