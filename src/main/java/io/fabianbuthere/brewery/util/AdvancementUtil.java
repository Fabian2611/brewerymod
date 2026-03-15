package io.fabianbuthere.brewery.util;

import io.fabianbuthere.brewery.BreweryMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementUtil {
    public static void grantAdvancement(ServerPlayer player, String advancementPath) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BreweryMod.MOD_ID, advancementPath);
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(id);

        if (advancement != null) {
            player.getAdvancements().award(advancement, "manual_trigger");
        }
    }
}
