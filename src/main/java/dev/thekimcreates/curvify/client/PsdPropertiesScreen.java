package dev.thekimcreates.curvify.client;

import dev.thekimcreates.curvify.placement.PlacementConfig;
import dev.thekimcreates.curvify.placement.PsdType;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** A ghost-slot editor: selecting entries never consumes or moves the player's inventory. */
public final class PsdPropertiesScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 240;
    private static final int SLOT = 20;
    private static final int RESULT_COLUMNS = 14;
    private static final int RESULT_ROWS = 2;
    private static final int RESULTS_PER_PAGE = RESULT_COLUMNS * RESULT_ROWS;

    private final Screen previous;
    private final String railId;
    private final List<Identifier> patternSlots = new ArrayList<>();
    private final List<Candidate> allCandidates = new ArrayList<>();
    private final List<Candidate> filteredCandidates = new ArrayList<>();

    private TextFieldWidget searchField;
    private ButtonWidget frontBackButton;
    private ButtonWidget leftRightButton;
    private ButtonWidget deleteButton;
    private ButtonWidget saveButton;
    private Identifier platformItemId = PlacementConfig.DEFAULT.platformBlockItemId();
    private boolean startFar;
    private boolean leftSide;
    private boolean requestedConfig;
    private boolean waitingForConfig = true;
    private int resultOffset;

    private Identifier draggedItemId;
    private int draggedFromPattern = -1;
    private boolean draggedFromPlatform;

    public PsdPropertiesScreen(Screen previous, String railId) {
        super(Text.translatable("curvify.psd_properties"));
        this.previous = previous;
        this.railId = railId;
        for (int index = 0; index < PlacementConfig.MAX_PATTERN_SLOTS; index++) {
            patternSlots.add(null);
        }
        applyConfig(PlacementConfig.DEFAULT);
        collectCandidates();
    }

    @Override
    protected void init() {
        final int left = panelLeft();
        final int top = panelTop();
        final int controlWidth = (PANEL_WIDTH - 28) / 3;

        frontBackButton = addDrawableChild(ButtonWidget.builder(frontBackText(), button -> {
            startFar = !startFar;
            button.setMessage(frontBackText());
        }).dimensions(left + 8, top + 24, controlWidth, 20).build());
        frontBackButton.active = !waitingForConfig;

        leftRightButton = addDrawableChild(ButtonWidget.builder(leftRightText(), button -> {
            leftSide = !leftSide;
            button.setMessage(leftRightText());
        }).dimensions(left + 12 + controlWidth, top + 24, controlWidth, 20).build());
        leftRightButton.active = !waitingForConfig;

        deleteButton = addDrawableChild(ButtonWidget.builder(Text.translatable("curvify.delete"), button -> {
            CurvifyClientNetworking.delete(railId);
            returnToPrevious();
        }).dimensions(left + 16 + controlWidth * 2, top + 24, controlWidth, 20).build());
        deleteButton.active = !waitingForConfig;

        searchField = new TextFieldWidget(textRenderer, left + 8, top + 51, PANEL_WIDTH - 16, 20, Text.translatable("curvify.search"));
        searchField.setMaxLength(80);
        searchField.setPlaceholder(Text.translatable("curvify.search"));
        searchField.setChangedListener(query -> filterCandidates());
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("curvify.cancel"), button -> returnToPrevious())
                .dimensions(left + PANEL_WIDTH - 172, top + PANEL_HEIGHT - 28, 80, 20)
                .build());
        saveButton = addDrawableChild(ButtonWidget.builder(Text.translatable("curvify.save"), button -> save())
                .dimensions(left + PANEL_WIDTH - 88, top + PANEL_HEIGHT - 28, 80, 20)
                .build());
        saveButton.active = !waitingForConfig;

        if (!requestedConfig) {
            requestedConfig = true;
            CurvifyClientNetworking.requestConfig(railId);
        }
    }

    public void acceptServerConfig(String responseRailId, PlacementConfig config) {
        if (!railId.equals(responseRailId)) {
            return;
        }
        applyConfig(config);
        waitingForConfig = false;
        if (frontBackButton != null) {
            frontBackButton.setMessage(frontBackText());
            frontBackButton.active = true;
        }
        if (leftRightButton != null) {
            leftRightButton.setMessage(leftRightText());
            leftRightButton.active = true;
        }
        if (deleteButton != null) {
            deleteButton.active = true;
        }
        if (saveButton != null) {
            saveButton.active = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        final int left = panelLeft();
        final int top = panelTop();

        drawPanel(context, left, top);
        final Text stateTitle = Text.literal(title.getString() + " • ")
                .append(Text.translatable(startFar ? "curvify.far" : "curvify.near"))
                .append(" • ")
                .append(Text.translatable(leftSide ? "curvify.left" : "curvify.right"));
        context.drawCenteredTextWithShadow(textRenderer, stateTitle, width / 2, top + 8, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("curvify.search_results"), left + 8, top + 76, 0x404040);
        context.drawTextWithShadow(textRenderer, Text.translatable("curvify.psd_pattern"), left + 8, top + 136, 0x404040);
        context.drawTextWithShadow(textRenderer, Text.translatable("curvify.platform_block"), left + 8, top + 177, 0x404040);

        renderResults(context, mouseX, mouseY, left + 20, top + 89);
        renderPattern(context, mouseX, mouseY, left + 70, top + 149);
        renderPlatform(context, mouseX, mouseY, left + 70, top + 190);

        final Text hint = waitingForConfig
                ? Text.translatable("curvify.loading")
                : Text.translatable("curvify.drag_hint");
        context.drawTextWithShadow(textRenderer, hint, left + 96, top + 196, 0x555555);

        super.render(context, mouseX, mouseY, delta);

        renderHoverTooltip(context, mouseX, mouseY);
        if (draggedItemId != null) {
            context.drawItem(itemStack(draggedItemId), mouseX - 8, mouseY - 8);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForConfig) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        final int left = panelLeft();
        final int top = panelTop();

        final int resultIndex = resultAt(mouseX, mouseY, left + 20, top + 89);
        if (resultIndex >= 0 && button == 0) {
            beginDrag(filteredCandidates.get(resultIndex).id(), -1, false);
            return true;
        }

        final int patternIndex = slotAt(mouseX, mouseY, left + 70, top + 149, PlacementConfig.MAX_PATTERN_SLOTS);
        if (patternIndex >= 0) {
            if (button == 1) {
                patternSlots.set(patternIndex, null);
            } else if (button == 0 && patternSlots.get(patternIndex) != null) {
                final Identifier itemId = patternSlots.get(patternIndex);
                patternSlots.set(patternIndex, null);
                beginDrag(itemId, patternIndex, false);
            }
            return true;
        }

        if (inside(mouseX, mouseY, left + 70, top + 190, SLOT, SLOT)) {
            if (button == 1) {
                platformItemId = null;
            } else if (button == 0 && platformItemId != null) {
                final Identifier itemId = platformItemId;
                platformItemId = null;
                beginDrag(itemId, -1, true);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggedItemId == null || button != 0) {
            return super.mouseReleased(mouseX, mouseY, button);
        }

        final int left = panelLeft();
        final int top = panelTop();
        final int patternIndex = slotAt(mouseX, mouseY, left + 70, top + 149, PlacementConfig.MAX_PATTERN_SLOTS);
        boolean accepted = false;
        if (patternIndex >= 0 && PsdType.isPsdItem(draggedItemId)) {
            final Identifier displaced = patternSlots.set(patternIndex, draggedItemId);
            restoreDisplaced(displaced);
            accepted = true;
        } else if (inside(mouseX, mouseY, left + 70, top + 190, SLOT, SLOT) && isPlatformItem(draggedItemId)) {
            final Identifier displaced = platformItemId;
            platformItemId = draggedItemId;
            restoreDisplaced(displaced);
            accepted = true;
        }

        if (!accepted) {
            restoreDragOrigin();
        }
        clearDrag();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        final int left = panelLeft();
        final int top = panelTop();
        if (inside(mouseX, mouseY, left + 20, top + 89, RESULT_COLUMNS * SLOT, RESULT_ROWS * SLOT)) {
            final int maxOffset = Math.max(0, filteredCandidates.size() - RESULTS_PER_PAGE);
            if (verticalAmount < 0) {
                resultOffset = Math.min(maxOffset, resultOffset + RESULT_COLUMNS);
            } else if (verticalAmount > 0) {
                resultOffset = Math.max(0, resultOffset - RESULT_COLUMNS);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        returnToPrevious();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void save() {
        final List<PsdType> pattern = patternSlots.stream()
                .filter(java.util.Objects::nonNull)
                .map(PsdType::fromItemId)
                .flatMap(java.util.Optional::stream)
                .toList();
        if (pattern.isEmpty() || platformItemId == null || !isPlatformItem(platformItemId)) {
            return;
        }
        CurvifyClientNetworking.save(
                railId,
                new PlacementConfig(pattern, platformItemId, startFar, leftSide)
        );
        returnToPrevious();
    }

    private void returnToPrevious() {
        MinecraftClient.getInstance().setScreen(previous);
    }

    private void applyConfig(PlacementConfig config) {
        for (int index = 0; index < patternSlots.size(); index++) {
            patternSlots.set(index, index < config.pattern().size() ? config.pattern().get(index).itemId() : null);
        }
        platformItemId = config.platformBlockItemId();
        startFar = config.startFar();
        leftSide = config.leftSide();
    }

    private void collectCandidates() {
        allCandidates.clear();
        for (PsdType type : PsdType.values()) {
            allCandidates.add(new Candidate(type.itemId(), true));
        }
        Registries.ITEM.getIds().stream()
                .filter(id -> !PsdType.isPsdItem(id))
                .filter(this::isPlatformItem)
                .map(id -> new Candidate(id, false))
                .sorted(Comparator.comparing(Candidate::displayName).thenComparing(candidate -> candidate.id().toString()))
                .forEach(allCandidates::add);
        filterCandidates();
    }

    private void filterCandidates() {
        final String query = searchField == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        filteredCandidates.clear();
        allCandidates.stream()
                .filter(candidate -> candidate.displayName().toLowerCase(Locale.ROOT).contains(query))
                .forEach(filteredCandidates::add);
        resultOffset = 0;
    }

    private void renderResults(DrawContext context, int mouseX, int mouseY, int x, int y) {
        for (int visible = 0; visible < RESULTS_PER_PAGE; visible++) {
            final int candidateIndex = resultOffset + visible;
            final int slotX = x + visible % RESULT_COLUMNS * SLOT;
            final int slotY = y + visible / RESULT_COLUMNS * SLOT;
            drawSlot(context, slotX, slotY, candidateIndex < filteredCandidates.size());
            if (candidateIndex < filteredCandidates.size()) {
                context.drawItem(itemStack(filteredCandidates.get(candidateIndex).id()), slotX + 2, slotY + 2);
            }
        }
        if (filteredCandidates.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("curvify.no_results"), x + RESULT_COLUMNS * SLOT / 2, y + 25, 0x666666);
        }
    }

    private void renderPattern(DrawContext context, int mouseX, int mouseY, int x, int y) {
        for (int index = 0; index < patternSlots.size(); index++) {
            final int slotX = x + index * SLOT;
            drawSlot(context, slotX, y, true);
            final Identifier itemId = patternSlots.get(index);
            if (itemId != null) {
                context.drawItem(itemStack(itemId), slotX + 2, y + 2);
            }
        }
    }

    private void renderPlatform(DrawContext context, int mouseX, int mouseY, int x, int y) {
        drawSlot(context, x, y, true);
        if (platformItemId != null) {
            context.drawItem(itemStack(platformItemId), x + 2, y + 2);
        }
    }

    private void renderHoverTooltip(DrawContext context, int mouseX, int mouseY) {
        final int left = panelLeft();
        final int top = panelTop();
        final int resultIndex = resultAt(mouseX, mouseY, left + 20, top + 89);
        if (resultIndex >= 0) {
            context.drawItemTooltip(textRenderer, itemStack(filteredCandidates.get(resultIndex).id()), mouseX, mouseY);
            return;
        }
        final int patternIndex = slotAt(mouseX, mouseY, left + 70, top + 149, PlacementConfig.MAX_PATTERN_SLOTS);
        if (patternIndex >= 0 && patternSlots.get(patternIndex) != null) {
            context.drawItemTooltip(textRenderer, itemStack(patternSlots.get(patternIndex)), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, left + 70, top + 190, SLOT, SLOT) && platformItemId != null) {
            context.drawItemTooltip(textRenderer, itemStack(platformItemId), mouseX, mouseY);
        }
    }

    private void drawPanel(DrawContext context, int left, int top) {
        context.fill(left - 3, top - 3, left + PANEL_WIDTH + 3, top + PANEL_HEIGHT + 3, 0xFF111111);
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFFC6C6C6);
        context.fill(left + 4, top + 4, left + PANEL_WIDTH - 4, top + 20, 0xFF555555);
    }

    private static void drawSlot(DrawContext context, int x, int y, boolean enabled) {
        context.fill(x, y, x + SLOT, y + SLOT, 0xFF373737);
        context.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, enabled ? 0xFF8B8B8B : 0xFF666666);
        context.fill(x + 2, y + 2, x + SLOT - 2, y + SLOT - 2, enabled ? 0xFFC6C6C6 : 0xFF777777);
    }

    private int resultAt(double mouseX, double mouseY, int x, int y) {
        if (!inside(mouseX, mouseY, x, y, RESULT_COLUMNS * SLOT, RESULT_ROWS * SLOT)) {
            return -1;
        }
        final int column = ((int) mouseX - x) / SLOT;
        final int row = ((int) mouseY - y) / SLOT;
        final int index = resultOffset + row * RESULT_COLUMNS + column;
        return index < filteredCandidates.size() ? index : -1;
    }

    private static int slotAt(double mouseX, double mouseY, int x, int y, int count) {
        if (!inside(mouseX, mouseY, x, y, count * SLOT, SLOT)) {
            return -1;
        }
        return ((int) mouseX - x) / SLOT;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void beginDrag(Identifier itemId, int patternIndex, boolean fromPlatform) {
        draggedItemId = itemId;
        draggedFromPattern = patternIndex;
        draggedFromPlatform = fromPlatform;
    }

    private void restoreDragOrigin() {
        if (draggedFromPattern >= 0) {
            patternSlots.set(draggedFromPattern, draggedItemId);
        } else if (draggedFromPlatform) {
            platformItemId = draggedItemId;
        }
    }

    private void restoreDisplaced(Identifier displaced) {
        if (displaced == null) {
            return;
        }
        if (draggedFromPattern >= 0) {
            patternSlots.set(draggedFromPattern, displaced);
        } else if (draggedFromPlatform) {
            platformItemId = displaced;
        }
    }

    private void clearDrag() {
        draggedItemId = null;
        draggedFromPattern = -1;
        draggedFromPlatform = false;
    }

    private boolean isPlatformItem(Identifier id) {
        final Item item = Registries.ITEM.get(id);
        if (!(item instanceof BlockItem blockItem) || PsdType.isPsdItem(id)) {
            return false;
        }
        final Block block = blockItem.getBlock();
        return !block.getDefaultState().isAir() && !block.getDefaultState().hasBlockEntity();
    }

    private static ItemStack itemStack(Identifier id) {
        return Registries.ITEM.get(id).getDefaultStack();
    }

    private Text frontBackText() {
        return Text.translatable("curvify.front_back_flip");
    }

    private Text leftRightText() {
        return Text.translatable("curvify.left_right_flip");
    }

    private int panelLeft() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return Math.max(0, (height - PANEL_HEIGHT) / 2);
    }

    private record Candidate(Identifier id, boolean psd) {
        private String displayName() {
            return itemStack(id).getName().getString();
        }
    }
}
