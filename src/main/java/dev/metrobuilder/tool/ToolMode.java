package dev.metrobuilder.tool;

public enum ToolMode {
    BUILD("Build"),
    SELECT("Select"),
    MOVE("Move"),
    ROTATE("Rotate"),
    DUPLICATE("Duplicate"),
    DELETE("Delete"),
    RAIL("Rail");

    private final String displayName;

    ToolMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public ToolMode next() {
        ToolMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public ToolMode previous() {
        ToolMode[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }
}
