package dev.metrobuilder.client;

import dev.metrobuilder.tool.ToolMode;
import net.minecraft.block.Blocks;

public final class ClientState {
    private static ToolMode toolMode = ToolMode.BUILD;
    private static String blockId = "minecraft:white_concrete";
    private static float rotationDegrees;
    private static double sidewaysOffset;
    private static double heightOffset;

    private ClientState() {}

    public static ToolMode toolMode() { return toolMode; }
    public static void setToolMode(ToolMode mode) { toolMode = mode; }
    public static String blockId() { return blockId; }
    public static float rotationDegrees() { return rotationDegrees; }
    public static double sidewaysOffset() { return sidewaysOffset; }
    public static double heightOffset() { return heightOffset; }
}
