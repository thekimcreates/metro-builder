package dev.thekimcreates.metrobuilder.client.screen;

import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDDisplayProperties;
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
import java.util.function.DoubleConsumer;

/** Non-pausing properties panel for a pending or placed PSD assembly. */
public final class PSDPropertiesScreen extends Screen {
    private final PrecisionTransform initialTransform;
    private final PSDDisplayProperties initialDisplayProperties;
    private final SaveCallback saveCallback;
    private final Runnable deleteCallback;
    private final DoubleConsumer doorValueCallback;

    private Identifier selectedPackId;
    private ButtonWidget packButton;
    private ButtonWidget arrowButton;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private TextFieldWidget yawField;
    private TextFieldWidget currentKoreanField;
    private TextFieldWidget currentEnglishField;
    private TextFieldWidget currentCodeField;
    private TextFieldWidget previousKoreanField;
    private TextFieldWidget previousEnglishField;
    private TextFieldWidget nextKoreanField;
    private TextFieldWidget nextEnglishField;
    private TextFieldWidget lineField;
    private TextFieldWidget platformField;
    private boolean arrowRight;
    private String errorMessage = "";

    public PSDPropertiesScreen(
            String title,
            Identifier packId,
            PrecisionTransform initialTransform,
            PSDDisplayProperties initialDisplayProperties,
            SaveCallback saveCallback
    ) {
        this(
                title,
                packId,
                initialTransform,
                initialDisplayProperties,
                saveCallback,
                null,
                null
        );
    }

    public PSDPropertiesScreen(
            String title,
            Identifier packId,
            PrecisionTransform initialTransform,
            PSDDisplayProperties initialDisplayProperties,
            SaveCallback saveCallback,
            Runnable deleteCallback
    ) {
        this(title, packId, initialTransform, initialDisplayProperties, saveCallback, deleteCallback, null);
    }

    public PSDPropertiesScreen(
            String title,
            Identifier packId,
            PrecisionTransform initialTransform,
            PSDDisplayProperties initialDisplayProperties,
            SaveCallback saveCallback,
            Runnable deleteCallback,
            DoubleConsumer doorValueCallback
    ) {
        super(Text.literal(title));
        selectedPackId = Objects.requireNonNull(packId, "packId");
        this.initialTransform = Objects.requireNonNull(initialTransform, "initialTransform");
        this.initialDisplayProperties = Objects.requireNonNull(
                initialDisplayProperties,
                "initialDisplayProperties"
        );
        this.saveCallback = Objects.requireNonNull(saveCallback, "saveCallback");
        this.deleteCallback = deleteCallback;
        this.doorValueCallback = doorValueCallback;
        arrowRight = initialDisplayProperties.arrowRight();
    }

    @Override
    protected void init() {
        final int centerX = width / 2;
        final int top = Math.max(8, height / 2 - 166);
        final int leftX = centerX - 216;
        final int rightX = centerX + 18;
        final int labelWidth = 78;
        final int fieldWidth = 136;

        packButton = addDrawableChild(ButtonWidget.builder(
                        packButtonText(),
                        button -> cyclePack()
                )
                .dimensions(centerX - 105, top + 30, 210, 20)
                .build());

        xField = addNumericField(leftX + labelWidth, top + 70, fieldWidth, initialTransform.x());
        yField = addNumericField(leftX + labelWidth, top + 96, fieldWidth, initialTransform.y());
        zField = addNumericField(leftX + labelWidth, top + 122, fieldWidth, initialTransform.z());
        yawField = addNumericField(leftX + labelWidth, top + 148, fieldWidth, initialTransform.yaw());

        currentKoreanField = addTextField(
                rightX + labelWidth,
                top + 70,
                fieldWidth,
                initialDisplayProperties.currentStationKorean()
        );
        currentEnglishField = addTextField(
                rightX + labelWidth,
                top + 96,
                fieldWidth,
                initialDisplayProperties.currentStationEnglish()
        );
        currentCodeField = addTextField(
                rightX + labelWidth,
                top + 122,
                fieldWidth,
                initialDisplayProperties.currentStationCode()
        );
        previousKoreanField = addTextField(
                rightX + labelWidth,
                top + 148,
                fieldWidth,
                initialDisplayProperties.previousStationKorean()
        );
        previousEnglishField = addTextField(
                rightX + labelWidth,
                top + 174,
                fieldWidth,
                initialDisplayProperties.previousStationEnglish()
        );
        nextKoreanField = addTextField(
                rightX + labelWidth,
                top + 200,
                fieldWidth,
                initialDisplayProperties.nextStationKorean()
        );
        nextEnglishField = addTextField(
                rightX + labelWidth,
                top + 226,
                fieldWidth,
                initialDisplayProperties.nextStationEnglish()
        );
        lineField = addTextField(
                leftX + labelWidth,
                top + 200,
                fieldWidth,
                initialDisplayProperties.lineNumber()
        );
        platformField = addTextField(
                leftX + labelWidth,
                top + 226,
                fieldWidth,
                initialDisplayProperties.platformNumber()
        );

        arrowButton = addDrawableChild(ButtonWidget.builder(
                        arrowButtonText(),
                        button -> {
                            arrowRight = !arrowRight;
                            arrowButton.setMessage(arrowButtonText());
                        }
                )
                .dimensions(leftX + labelWidth, top + 252, fieldWidth, 20)
                .build());

        if (doorValueCallback != null) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Open Doors"), button -> setDoorValue(1.0D))
                    .dimensions(centerX - 96, top + 292, 90, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Close Doors"), button -> setDoorValue(0.0D))
                    .dimensions(centerX + 6, top + 292, 90, 20)
                    .build());
        }

        final int actionY = doorValueCallback == null ? top + 292 : top + 316;
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(centerX - 96, actionY, 90, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(centerX + 6, actionY, 90, 20)
                .build());

        if (deleteCallback != null) {
            addDrawableChild(ButtonWidget.builder(
                            Text.literal("Delete PSD").formatted(Formatting.RED),
                            button -> deletePsd()
                    )
                    .dimensions(centerX - 96, actionY + 24, 192, 20)
                    .build());
        }

        setInitialFocus(xField);
    }

    private void setDoorValue(double value) {
        if (doorValueCallback != null) {
            doorValueCallback.accept(value);
        }
    }

    private TextFieldWidget addNumericField(int x, int y, int width, double value) {
        return addTextField(x, y, width, String.format(Locale.ROOT, "%.3f", value));
    }

    private TextFieldWidget addTextField(int x, int y, int width, String value) {
        final TextFieldWidget field = new TextFieldWidget(
                textRenderer,
                x,
                y,
                width,
                20,
                Text.empty()
        );
        field.setMaxLength(64);
        field.setText(value);
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

    private Text arrowButtonText() {
        return Text.literal(arrowRight ? "Right →" : "← Left");
    }

    private void save() {
        try {
            final PrecisionTransform updatedTransform = new PrecisionTransform(
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
            final PSDDisplayProperties updatedDisplayProperties = new PSDDisplayProperties(
                    currentKoreanField.getText(),
                    currentEnglishField.getText(),
                    currentCodeField.getText(),
                    previousKoreanField.getText(),
                    previousEnglishField.getText(),
                    nextKoreanField.getText(),
                    nextEnglishField.getText(),
                    lineField.getText(),
                    platformField.getText(),
                    arrowRight
            );
            saveCallback.save(selectedPackId, updatedTransform, updatedDisplayProperties);
            close();
        } catch (IllegalArgumentException exception) {
            errorMessage = "Enter valid finite position and rotation numbers";
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
        final int top = Math.max(8, height / 2 - 166);
        final int leftX = centerX - 216;
        final int rightX = centerX + 18;
        final int panelBottom = deleteCallback == null
                ? top + (doorValueCallback == null ? 326 : 350)
                : top + (doorValueCallback == null ? 350 : 374);

        context.fill(centerX - 244, top - 10, centerX + 244, panelBottom, 0xE0101010);
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, top, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "PSD Pack", centerX, top + 17, 0xD0D0D0);

        drawLabel(context, "X", leftX, top + 76);
        drawLabel(context, "Y", leftX, top + 102);
        drawLabel(context, "Z", leftX, top + 128);
        drawLabel(context, "Yaw", leftX, top + 154);
        drawLabel(context, "Line", leftX, top + 206);
        drawLabel(context, "Platform", leftX, top + 232);
        drawLabel(context, "Arrow", leftX, top + 258);

        drawLabel(context, "Current KR", rightX, top + 76);
        drawLabel(context, "Current EN", rightX, top + 102);
        drawLabel(context, "Code", rightX, top + 128);
        drawLabel(context, "Previous KR", rightX, top + 154);
        drawLabel(context, "Previous EN", rightX, top + 180);
        drawLabel(context, "Next KR", rightX, top + 206);
        drawLabel(context, "Next EN", rightX, top + 232);

        if (!errorMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(errorMessage),
                    centerX,
                    top + 280,
                    0xFF5555
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLabel(DrawContext context, String label, int x, int y) {
        context.drawTextWithShadow(textRenderer, label, x, y, 0xD0D0D0);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @FunctionalInterface
    public interface SaveCallback {
        void save(
                Identifier packId,
                PrecisionTransform transform,
                PSDDisplayProperties displayProperties
        );
    }
}
