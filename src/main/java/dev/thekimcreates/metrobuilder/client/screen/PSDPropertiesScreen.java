package dev.thekimcreates.metrobuilder.client.screen;

import dev.thekimcreates.metrobuilder.precision.PrecisionTransform;
import dev.thekimcreates.metrobuilder.psd.PSDDisplayProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleConsumer;

/** Two-column editor for positioning and the live-rendered Seoul header. */
public final class PSDPropertiesScreen extends Screen {
    private static final int ROW = 21;
    private final Identifier packId;
    private final PrecisionTransform initialTransform;
    private final PSDDisplayProperties initial;
    private final SaveCallback saveCallback;
    private final List<LabeledField> fields = new ArrayList<>();
    private TextFieldWidget xField, yField, zField, rotationField;
    private TextFieldWidget lineColorField, platformField;
    private TextFieldWidget currentKo, currentEn, currentCh, currentJp;
    private TextFieldWidget nextKo, nextEn, nextCh, nextJp;
    private TextFieldWidget previousKo, previousEn, previousCh, previousJp;
    private ButtonWidget directionButton;
    private boolean arrowRight;
    private String errorMessage = "";

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
        this.initial = Objects.requireNonNull(properties);
        this.saveCallback = Objects.requireNonNull(saveCallback);
        this.arrowRight = properties.arrowRight();
    }

    @Override
    protected void init() {
        fields.clear();
        final int panelWidth = Math.min(720, width - 20);
        final int left = (width - panelWidth) / 2;
        final int top = Math.max(8, (height - 390) / 2);
        final int leftFieldX = left + 105;
        final int rightLabelX = left + panelWidth / 2 + 16;
        final int rightFieldX = rightLabelX + 116;
        final int rightFieldWidth = left + panelWidth - 16 - rightFieldX;

        xField = field("X", left + 18, leftFieldX, top + 48, 190, number(initialTransform.x()));
        yField = field("Y", left + 18, leftFieldX, top + 48 + ROW, 190, number(initialTransform.y()));
        zField = field("Z", left + 18, leftFieldX, top + 48 + ROW * 2, 190, number(initialTransform.z()));
        rotationField = field("Rotation", left + 18, leftFieldX, top + 48 + ROW * 3, 190,
                number(initialTransform.yaw()));

        int y = top + 48;
        lineColorField = field("Line Color", rightLabelX, rightFieldX, y, rightFieldWidth, initial.lineColor());
        platformField = field("Platform Number", rightLabelX, rightFieldX, y += ROW,
                rightFieldWidth, initial.platformNumber());
        directionButton = addDrawableChild(ButtonWidget.builder(directionText(), button -> {
            arrowRight = !arrowRight;
            directionButton.setMessage(directionText());
        }).dimensions(rightFieldX, y += ROW, rightFieldWidth, 18).build());

        y += ROW + 5;
        currentKo = field("Current (ko)", rightLabelX, rightFieldX, y, rightFieldWidth, initial.currentStationKorean());
        currentEn = field("Current (en)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.currentStationEnglish());
        currentCh = field("Current (ch)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.currentStationChinese());
        currentJp = field("Current (jp)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.currentStationJapanese());
        nextKo = field("Next (ko)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.nextStationKorean());
        nextEn = field("Next (en)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.nextStationEnglish());
        nextCh = field("Next (ch)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.nextStationChinese());
        nextJp = field("Next (jp)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.nextStationJapanese());
        previousKo = field("Previous (ko)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.previousStationKorean());
        previousEn = field("Previous (en)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.previousStationEnglish());
        previousCh = field("Previous (ch)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.previousStationChinese());
        previousJp = field("Previous (jp)", rightLabelX, rightFieldX, y += ROW, rightFieldWidth, initial.previousStationJapanese());

        final int actionY = top + 356;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> save())
                .dimensions(width / 2 - 96, actionY, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 + 6, actionY, 90, 20).build());
        setInitialFocus(xField);
    }

    private TextFieldWidget field(String label, int labelX, int x, int y, int width, String value) {
        final TextFieldWidget widget = new TextFieldWidget(textRenderer, x, y, width, 18, Text.empty());
        widget.setMaxLength(64);
        widget.setText(value);
        fields.add(new LabeledField(label, labelX, y + 5, widget));
        return addDrawableChild(widget);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private Text directionText() {
        return Text.literal(arrowRight ? "Right" : "Left");
    }

    private void save() {
        try {
            final String color = lineColorField.getText().strip();
            if (!color.matches("#?[0-9a-fA-F]{6}")) {
                errorMessage = "Line Color must be a 6-digit hex code, such as #996CAC";
                return;
            }
            final PrecisionTransform transform = new PrecisionTransform(
                    Double.parseDouble(xField.getText()), Double.parseDouble(yField.getText()),
                    Double.parseDouble(zField.getText()), initialTransform.pitch(),
                    Float.parseFloat(rotationField.getText()), initialTransform.roll(),
                    initialTransform.scaleX(), initialTransform.scaleY(), initialTransform.scaleZ());
            final PSDDisplayProperties properties = new PSDDisplayProperties(
                    color, platformField.getText(), arrowRight,
                    currentKo.getText(), currentEn.getText(), currentCh.getText(), currentJp.getText(),
                    nextKo.getText(), nextEn.getText(), nextCh.getText(), nextJp.getText(),
                    previousKo.getText(), previousEn.getText(), previousCh.getText(), previousJp.getText());
            saveCallback.save(packId, transform, properties);
            close();
        } catch (IllegalArgumentException exception) {
            errorMessage = "Enter valid finite position and rotation numbers";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        final int panelWidth = Math.min(720, width - 20);
        final int left = (width - panelWidth) / 2;
        final int top = Math.max(8, (height - 390) / 2);
        context.fill(left, top, left + panelWidth, top + 386, 0xEE101010);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 10, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Positioning", left + 18, top + 30, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Header", left + panelWidth / 2 + 16, top + 30, 0xFFFFFF);
        for (LabeledField field : fields) {
            context.drawTextWithShadow(textRenderer, field.label(), field.labelX(), field.labelY(), 0xD0D0D0);
        }
        context.drawTextWithShadow(textRenderer, "Direction", left + panelWidth / 2 + 16,
                top + 48 + ROW * 2 + 5, 0xD0D0D0);
        if (!errorMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, errorMessage, width / 2, top + 342, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }

    private record LabeledField(String label, int labelX, int labelY, TextFieldWidget widget) {}

    @FunctionalInterface
    public interface SaveCallback {
        void save(Identifier packId, PrecisionTransform transform, PSDDisplayProperties properties);
    }
}
