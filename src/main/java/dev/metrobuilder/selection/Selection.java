package dev.metrobuilder.selection;

import net.minecraft.util.math.BlockPos;

public record Selection(BlockPos first, BlockPos second) {
    public BlockPos min() {
        return new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
    }
    public BlockPos max() {
        return new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
    }
    public int sizeX() { return max().getX() - min().getX() + 1; }
    public int sizeY() { return max().getY() - min().getY() + 1; }
    public int sizeZ() { return max().getZ() - min().getZ() + 1; }
    public long volume() { return (long) sizeX() * sizeY() * sizeZ(); }
    public Selection moved(BlockPos newMin) {
        BlockPos delta = newMin.subtract(min());
        return new Selection(first.add(delta), second.add(delta));
    }
}
