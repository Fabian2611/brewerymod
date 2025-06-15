package io.fabianbuthere.brewery.block.custom;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum WoodType implements StringRepresentable {
    ACACIA("acacia"), BAMBOO("bamboo"), BIRCH("birch"), CHERRY("cherry"), DARK_OAK("dark_oak"), JUNGLE("jungle"), MANGROVE("mangrove"), OAK("oak"), CRIMSON("crimson"), WARPED("warped"), SPRUCE("spruce");

    private final String representation;

    WoodType(String representation) {
        this.representation = representation;
    }

    @Override
    public @NotNull String getSerializedName() {
        return representation;
    }
}
