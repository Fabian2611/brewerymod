package io.fabianbuthere.brewery.util;

import io.fabianbuthere.brewery.recipe.BrewingRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record BrewType(
        String id,
        int maxAlcoholLevel,
        int maxPurity,
        List<MobEffectInstance> effects,
        int tintColor,
        String customLore,
        String customName
) {
    public static ItemStack DEFAULT_POTION() {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.getOrCreateTag().putString("Potion", "minecraft:water");
        return stack;
    }

    public static ItemStack GENERIC_FAILED_BREW() {
        ItemStack brew = new ItemStack(Items.POTION).setHoverName(Component.translatable("brewery.brew.failed_brew"));
        CompoundTag tag = brew.getOrCreateTag();
        CompoundTag displayTag = tag.getCompound("display");
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.translatable("brewery.brew.failed_brew_generic_lore"))));
        displayTag.put("Lore", loreList);
        tag.put("display", displayTag);
        tag.putInt("CustomPotionColor", 0xFFCD94);
        brew.setTag(tag);
        return brew;
    }

    public static ItemStack WRONG_INGREDIENTS_BREW() {
        ItemStack brew = new ItemStack(Items.POTION).setHoverName(Component.translatable("brewery.brew.failed_brew"));
        CompoundTag tag = brew.getOrCreateTag();
        CompoundTag displayTag = tag.getCompound("display");
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.translatable("brewery.brew.failed_brew_wrong_ingredients_lore"))));
        displayTag.put("Lore", loreList);
        tag.put("display", displayTag);
        tag.putInt("CustomPotionColor", 0xFFCD94);
        brew.setTag(tag);
        return brew;
    }

    public static ItemStack INCORRECT_INGREDIENT_AMOUNT_BREW(){
        ItemStack brew = new ItemStack(Items.POTION).setHoverName(Component.translatable("brewery.brew.failed_brew"));
        CompoundTag tag = brew.getOrCreateTag();
        CompoundTag displayTag = tag.getCompound("display");
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.translatable("brewery.brew.failed_brew_wrong_ingredients_amount_lore"))));
        displayTag.put("Lore", loreList);
        tag.put("display", displayTag);
        tag.putInt("CustomPotionColor", 0xFFCD94);
        brew.setTag(tag);
        return brew;
    }

    public static ItemStack INCORRECT_DISTILLERY_BREW(){
        ItemStack brew = new ItemStack(Items.POTION).setHoverName(Component.translatable("brewery.brew.failed_brew"));
        CompoundTag tag = brew.getOrCreateTag();
        CompoundTag displayTag = tag.getCompound("display");
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.translatable("brewery.brew.failed_brew_wrong_distilling_lore"))));
        displayTag.put("Lore", loreList);
        tag.put("display", displayTag);
        tag.putInt("CustomPotionColor", 0xFFCD94);
        brew.setTag(tag);
        return brew;
    }

    public static ItemStack INCORRECT_BREWING_TIME_BREW(){
        ItemStack brew = new ItemStack(Items.POTION).setHoverName(Component.translatable("brewery.brew.failed_brew"));
        CompoundTag tag = brew.getOrCreateTag();
        CompoundTag displayTag = tag.getCompound("display");
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.translatable("brewery.brew.failed_brew_wrong_brewing_time_lore"))));
        displayTag.put("Lore", loreList);
        tag.put("display", displayTag);
        tag.putInt("CustomPotionColor", 0xFFCD94);
        brew.setTag(tag);
        return brew;
    }

    public static ItemStack INCORRECT_AGING_BREW() {
        ItemStack brew = new ItemStack(Items.POTION).setHoverName(Component.translatable("brewery.brew.failed_brew"));
        CompoundTag tag = brew.getOrCreateTag();
        CompoundTag displayTag = tag.getCompound("display");
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.translatable("brewery.brew.failed_brew_wrong_aging_lore"))));
        displayTag.put("Lore", loreList);
        tag.put("display", displayTag);
        tag.putInt("CustomPotionColor", 0xFFCD94);
        brew.setTag(tag);
        return brew;
    }

    public static boolean isValid(String id) {
        return BrewTypeRegistry.contains(id);
    }

    public static BrewType getBrewTypeFromId(String id) {
        return BrewTypeRegistry.get(id);
    }

    public static ItemStack getResultItem(String id) {
        if (getBrewTypeFromId(id) != null) {
            return itemFromBrewType(getBrewTypeFromId(id));
        } else {
            return DEFAULT_POTION();
        }
    }

    public static ItemStack itemFromBrewType(BrewType brewType) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.setHoverName(Component.translatable(brewType.customName()));
        stack.getOrCreateTag().putInt("maxAlcoholLevel", brewType.maxAlcoholLevel());
        stack.getOrCreateTag().putInt("maxPurity", brewType.maxPurity());
        stack.getOrCreateTag().putInt("tintColor", brewType.tintColor());
        stack.getOrCreateTag().putInt("CustomPotionColor", brewType.tintColor());
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(Component.translatable(brewType.customLore()).getString()));
        stack.getOrCreateTag().getCompound("display").put("Lore", loreList);
        return stack;
    }

    public static Tag serializeEffects(List<MobEffectInstance> effects) {
        net.minecraft.nbt.ListTag listTag = new net.minecraft.nbt.ListTag();
        for (MobEffectInstance effect : effects) {
            CompoundTag effectTag = effect.save(new net.minecraft.nbt.CompoundTag());
            effectTag.putBoolean("ShowParticles", false);
            listTag.add(effectTag);
        }
        return listTag;
    }

    public Tag serializeEffects() {
        return serializeEffects(this.effects());
    }

    public ItemStack toItem(BrewingRecipe recipe, long brewingTime, ItemStackHandler inventory) {
        ItemStack stack = itemFromBrewType(this);

        List<ItemStack> inventoryList = new ArrayList<>(3);
        for (int j = 0; j < inventory.getSlots(); j++) {
            ItemStack inputStack = inventory.getStackInSlot(j);
            if (!inputStack.isEmpty()) {
                inventoryList.add(inputStack.copy());
            }
        }

        // Fail if the set of input item ids does not match the set of expected item ids
        Set<Item> expectedItems = new HashSet<>(recipe.getInputs().stream().map(ItemStackInput::item).toList());
        Set<Item> actualItems = new HashSet<>(inventoryList.stream().map(ItemStack::getItem).toList());
        if (!expectedItems.equals(actualItems)) {
            return WRONG_INGREDIENTS_BREW();
        }

        // Fail if any item count is off
        for (ItemStackInput i : recipe.getInputs()) {
            int totalCount = 0;
            for (ItemStack inputStack : inventoryList) {
                if (inputStack.getItem() == i.item()) {
                    totalCount += inputStack.getCount();
                }
            }
            if (totalCount < i.minCount() || totalCount > i.maxCount()) {
                return INCORRECT_INGREDIENT_AMOUNT_BREW();
            }
        }

        // Fail if brewing time is not within the allowed range
        double brewingTimeError = Math.abs((double)brewingTime - recipe.getOptimalBrewingTime()) / recipe.getOptimalBrewingTime();
        if (brewingTimeError > recipe.getMaxBrewingTimeError()) {
            return INCORRECT_BREWING_TIME_BREW();
        } else {
            // Calculate purity
            double normalizedBrewingTimeError = brewingTimeError / recipe.getMaxBrewingTimeError();
            int purity = (int)Math.round(maxPurity * (1 - normalizedBrewingTimeError));
            stack.getOrCreateTag().putInt("purity", purity);
        }

        // Set custom display
        stack.setHoverName(Component.translatable("brewery.brew.unfinished_brew"));
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.translatable("brewery.brew.unfinished_brew_lore"))));
        stack.getOrCreateTag().getCompound("display").put("Lore", loreList);
        // Make potion remember recipe
        stack.getOrCreateTag().putString("recipeId", recipe.getId().toString());
        return stack;
    }

    /**
     * Finalizes a brew, returning a finished or failed brew ItemStack.
     * Used by both Fermentation Barrel and Distillery Station.
     * @param recipe The BrewingRecipe
     * @param inputStack The input ItemStack
     * @param filterStack The filter ItemStack (can be null for barrel)
     * @param progress The aging/distilling progress (can be 0 for distillery)
     * @param barrelWoodType The wood type for barrel aging (can be null for distillery)
     * @param context Context string: "barrel" or "distillery"
     * @param level The Level
     * @return The finalized ItemStack
     */
    public static ItemStack finalizeBrew(
            BrewingRecipe recipe,
            ItemStack inputStack,
            ItemStack filterStack,
            long progress,
            String barrelWoodType,
            String context,
            Level level
    ) {
        // Get BrewType result
        BrewType brewTypeResult = BrewType.getBrewTypeFromId(recipe.getBrewTypeId());
        if (brewTypeResult == null) {
            return BrewType.GENERIC_FAILED_BREW();
        }
        int maxPurity = brewTypeResult.maxPurity();
        CompoundTag inputTag = inputStack.getOrCreateTag();
        int actualPurity = inputTag.getInt("purity");
        CompoundTag resultTag;
        ItemStack resultItem;
        // Barrel context: aging logic
        if ("barrel".equals(context)) {
            // Fail if distillery is needed but missing
            if (!(recipe.getDistillingItem() == null) && !recipe.getDistillingItem().isEmpty()) {
                if (!inputTag.getString("distillingItem").equals(recipe.getDistillingItem())) {
                    return BrewType.GENERIC_FAILED_BREW();
                }
            }
            // Fail if brew does not need aging
            if (recipe.getOptimalAgingTime() == 0L || recipe.getAllowedWoodTypes().isEmpty()) return BrewType.INCORRECT_AGING_BREW();
            // Fail if wood type does not match
            if (barrelWoodType == null || !recipe.getAllowedWoodTypes().contains(barrelWoodType)) return BrewType.INCORRECT_AGING_BREW();
            // Fail if time was off
            float maxError = recipe.getMaxAgingTimeError();
            long maxTime = Math.round(recipe.getOptimalAgingTime() * (1 + maxError));
            long minTime = Math.round(recipe.getOptimalAgingTime() * (1 - maxError));
            if (progress < minTime || progress > maxTime) {return BrewType.INCORRECT_AGING_BREW();}
            // Return the resulting brew
            float error = (float) Math.abs(progress - recipe.getOptimalAgingTime()) / recipe.getOptimalAgingTime();
            resultItem = recipe.getResultItem(level.registryAccess());
            resultTag = resultItem.getOrCreateTag();
            // Calculate purity
            float errorContribution = (float) Math.pow(1.0f - error, 2);
            int effectivePurity = Math.round(actualPurity * errorContribution);
            float purityFactor = (float) effectivePurity / (float) maxPurity;
            // Display
            String purityRepresentation = "★".repeat(Math.max(0, effectivePurity)) + "☆".repeat(Math.max(0, maxPurity - effectivePurity));
            resultTag.putString("recipeId", recipe.getId().toString());
            ListTag loreList = new ListTag();
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(purityRepresentation))));
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(brewTypeResult.customLore()))));
            CompoundTag displayTag = resultTag.getCompound("display");
            displayTag.put("Lore", loreList);
            displayTag.putString("Name", Component.Serializer.toJson(Component.translatable(brewTypeResult.customName())));
            resultTag.put("display", displayTag);
            resultItem.setTag(resultTag);
            // Effects
            java.util.List<MobEffectInstance> resultEffects = new java.util.ArrayList<>();
            for (MobEffectInstance effect : brewTypeResult.effects()) {
                net.minecraft.world.effect.MobEffect mobEffect = effect.getEffect();
                int duration = Math.round(effect.getDuration() * purityFactor);
                int amplifier = Math.max(0, Math.round(effect.getAmplifier() * purityFactor));
                if (duration > 0) {
                    resultEffects.add(new MobEffectInstance(mobEffect, duration, amplifier));
                }
            }
            // Hangover effect for bad purity
            if (effectivePurity < (double)maxPurity / 2) {
                resultEffects.add(new MobEffectInstance(io.fabianbuthere.brewery.effect.ModEffects.HANGOVER.get(), 600 * (Math.max(1, maxPurity / 2 - effectivePurity + 1)), 0, false, false, true));
            }
            resultItem.getTag().put("CustomPotionEffects", BrewType.serializeEffects(resultEffects));
            return resultItem;
        } else if ("distillery".equals(context)) {
            // Fail if recipe does not require distilling, but a filter is used or already distilled
            if (recipe.getDistillingItem() == null || recipe.getDistillingItem().isEmpty() || !inputTag.getString("distillingItem").isEmpty()) {
                return BrewType.INCORRECT_DISTILLERY_BREW();
            }
            // Fail if filter does not match
            String filterItemId = filterStack.getItem().toString();
            if (!recipe.getDistillingItem().equals(filterItemId)) {
                return BrewType.INCORRECT_DISTILLERY_BREW();
            }
            // Set distilling item
            resultItem = inputStack.copyWithCount(1);
            resultTag = resultItem.getOrCreateTag();
            resultTag.putString("distillingItem", filterItemId);
            // If the recipe does not need aging, finalize the brew here
            if (recipe.getOptimalAgingTime() == 0L) {
                int effectivePurity = actualPurity;
                float purityFactor = (float) effectivePurity / (float) maxPurity;
                String purityRepresentation = "★".repeat(Math.max(0, effectivePurity)) + "☆".repeat(Math.max(0, maxPurity - effectivePurity));
                resultTag.putString("recipeId", recipe.getId().toString());
                ListTag loreList = new ListTag();
                loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(purityRepresentation))));
                loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(brewTypeResult.customLore()))));
                CompoundTag displayTag = resultTag.getCompound("display");
                displayTag.put("Lore", loreList);
                displayTag.putString("Name", Component.Serializer.toJson(Component.translatable(brewTypeResult.customName())));
                resultTag.put("display", displayTag);
                // Effects
                java.util.List<MobEffectInstance> resultEffects = new java.util.ArrayList<>();
                for (MobEffectInstance effect : brewTypeResult.effects()) {
                    net.minecraft.world.effect.MobEffect mobEffect = effect.getEffect();
                    int duration = Math.round(effect.getDuration() * purityFactor);
                    int amplifier = Math.max(0, Math.round(effect.getAmplifier() * purityFactor));
                    if (duration > 0) {
                        resultEffects.add(new MobEffectInstance(mobEffect, duration, amplifier));
                    }
                }
                // Hangover effect for bad purity
                if (effectivePurity < (double)maxPurity / 2) {
                    resultEffects.add(new MobEffectInstance(io.fabianbuthere.brewery.effect.ModEffects.HANGOVER.get(), 600 * (Math.max(1, maxPurity / 2 - effectivePurity + 1)), 0, false, false, true));
                }
                resultTag.put("CustomPotionEffects", BrewType.serializeEffects(resultEffects));
            }
            resultItem.setTag(resultTag);
            return resultItem;
        }
        return BrewType.GENERIC_FAILED_BREW();
    }

    /**
     * Returns the perfect brew for a recipe, with best stats possible.
     * @param recipe The BrewingRecipe
     * @param level The Level
     * @return The perfect finalized ItemStack
     */
    public static ItemStack finalizeBrew(BrewingRecipe recipe, Level level) {
        BrewType brewTypeResult = BrewType.getBrewTypeFromId(recipe.getBrewTypeId());
        int maxPurity = brewTypeResult.maxPurity();
        ItemStack resultItem = recipe.getResultItem(level.registryAccess());
        CompoundTag resultTag = resultItem.getOrCreateTag();
        // Set perfect purity
        int effectivePurity = maxPurity;
        float purityFactor = 1.0f;
        String purityRepresentation = "★".repeat(maxPurity);
        resultTag.putString("recipeId", recipe.getId().toString());
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(purityRepresentation))));
        loreList.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(brewTypeResult.customLore()))));
        CompoundTag displayTag = resultTag.getCompound("display");
        displayTag.put("Lore", loreList);
        displayTag.putString("Name", Component.Serializer.toJson(Component.translatable(brewTypeResult.customName())));
        resultTag.put("display", displayTag);
        resultItem.setTag(resultTag);
        // Effects
        java.util.List<MobEffectInstance> resultEffects = new java.util.ArrayList<>();
        for (MobEffectInstance effect : brewTypeResult.effects()) {
            net.minecraft.world.effect.MobEffect mobEffect = effect.getEffect();
            int duration = effect.getDuration();
            int amplifier = effect.getAmplifier();
            if (duration > 0) {
                resultEffects.add(new MobEffectInstance(mobEffect, duration, amplifier));
            }
        }
        resultItem.getTag().put("CustomPotionEffects", BrewType.serializeEffects(resultEffects));
        return resultItem;
    }
}
