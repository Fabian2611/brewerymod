package io.fabianbuthere.brewery.util;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.recipe.BrewingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

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
        String customName,
        String customTexture,
        boolean isOverageable
) {

    // ── NBT key for brew type identification ──
    public static final String BREW_TYPE_ID_TAG = "brewTypeId";

    public enum BrewFailure {
        GENERIC("brewery.brew.failed_brew_generic_lore"),
        WRONG_INGREDIENTS("brewery.brew.failed_brew_wrong_ingredients_lore"),
        INCORRECT_INGREDIENT_AMOUNT("brewery.brew.failed_brew_wrong_ingredients_amount_lore"),
        INCORRECT_DISTILLERY("brewery.brew.failed_brew_wrong_distilling_lore"),
        INCORRECT_BREWING_TIME("brewery.brew.failed_brew_wrong_brewing_time_lore"),
        INCORRECT_AGING("brewery.brew.failed_brew_wrong_aging_lore"),
        SPOILED("brewery.brew.failed_brew_spoiled_lore");

        private final String loreKey;

        BrewFailure(String loreKey) {
            this.loreKey = loreKey;
        }

        public String getLoreKey() {
            return loreKey;
        }
    }

    private static final int FAILED_BREW_COLOR = 0xFFCD94;

    private static @NotNull ItemStack createFailedBrew(String loreTranslationKey) {
        ItemStack brew = new ItemStack(Items.POTION);
        brew.setHoverName(Component.translatable("brewery.brew.failed_brew")
                .withStyle(style -> style.withItalic(false)));
        CompoundTag tag = brew.getOrCreateTag();
        CompoundTag displayTag = tag.getCompound("display");
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(
                Component.Serializer.toJson(Component.translatable(loreTranslationKey))));
        displayTag.put("Lore", loreList);
        tag.put("display", displayTag);
        tag.putInt("CustomPotionColor", FAILED_BREW_COLOR);
        brew.setTag(tag);
        return brew;
    }

    public static @NotNull ItemStack failedBrew(@NotNull BrewFailure reason) {
        return createFailedBrew(reason.getLoreKey());
    }

    public static @NotNull ItemStack GENERIC_FAILED_BREW() { return failedBrew(BrewFailure.GENERIC); }
    public static @NotNull ItemStack WRONG_INGREDIENTS_BREW() { return failedBrew(BrewFailure.WRONG_INGREDIENTS); }
    public static @NotNull ItemStack INCORRECT_INGREDIENT_AMOUNT_BREW() { return failedBrew(BrewFailure.INCORRECT_INGREDIENT_AMOUNT); }
    public static @NotNull ItemStack INCORRECT_DISTILLERY_BREW() { return failedBrew(BrewFailure.INCORRECT_DISTILLERY); }
    public static @NotNull ItemStack INCORRECT_BREWING_TIME_BREW() { return failedBrew(BrewFailure.INCORRECT_BREWING_TIME); }
    public static @NotNull ItemStack INCORRECT_AGING_BREW() { return failedBrew(BrewFailure.INCORRECT_AGING); }
    public static @NotNull ItemStack SPOILED_BREW() { return failedBrew(BrewFailure.SPOILED); }

    public static @NotNull ItemStack DEFAULT_POTION() {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.getOrCreateTag().putString("Potion", "minecraft:water");
        return stack;
    }

    public static boolean isValid(String id) {
        return BrewTypeRegistry.contains(id);
    }

    public static BrewType getBrewTypeFromId(String id) {
        return BrewTypeRegistry.get(id);
    }

    public static ItemStack getResultItem(String id) {
        BrewType brewType = getBrewTypeFromId(id);
        return brewType != null ? itemFromBrewType(brewType) : DEFAULT_POTION();
    }

    private static boolean hasCustomTexture(@NotNull BrewType brewType) {
        return brewType.customTexture() != null && !brewType.customTexture().isEmpty();
    }

    public static @NotNull ItemStack itemFromBrewType(@NotNull BrewType brewType) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.setHoverName(Component.translatable(brewType.customName())
                .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW)));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("maxAlcoholLevel", brewType.maxAlcoholLevel());
        tag.putInt("maxPurity", brewType.maxPurity());
        tag.putInt("tintColor", brewType.tintColor());
        tag.putInt("CustomPotionColor", brewType.tintColor());
        if (hasCustomTexture(brewType)) {
            tag.putString("customTexture", brewType.customTexture());
        }
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(
                Component.Serializer.toJson(Component.translatable(brewType.customLore()))));
        tag.getCompound("display").put("Lore", loreList);
        return stack;
    }

    public static @NotNull Tag serializeEffects(@NotNull List<MobEffectInstance> effects) {
        ListTag listTag = new ListTag();
        for (MobEffectInstance effect : effects) {
            CompoundTag effectTag = effect.save(new CompoundTag());
            effectTag.putBoolean("ShowParticles", false);
            listTag.add(effectTag);
        }
        return listTag;
    }

    public @NotNull Tag serializeEffects() {
        return serializeEffects(this.effects());
    }

    private static @NotNull List<MobEffectInstance> buildScaledEffects(@NotNull BrewType brewType, int clampedPurity, float purityFactor) {
        List<MobEffectInstance> resultEffects = new ArrayList<>();
        for (MobEffectInstance effect : brewType.effects()) {
            MobEffect mobEffect = effect.getEffect();
            int duration = Math.round(effect.getDuration() * purityFactor);
            int amplifier = Math.max(0, Math.round(effect.getAmplifier() * purityFactor));
            if (duration > 0) {
                resultEffects.add(new MobEffectInstance(mobEffect, duration, amplifier));
            }
        }
        int maxPurity = brewType.maxPurity();
        if (clampedPurity < (double) maxPurity / 2) {
            int hangoverDuration = 600 * Math.max(1, maxPurity / 2 - clampedPurity + 1);
            resultEffects.add(new MobEffectInstance(
                    io.fabianbuthere.brewery.effect.ModEffects.HANGOVER.get(),
                    hangoverDuration, 0, false, false, true));
        }
        return resultEffects;
    }

    private static void applyFinalDisplay(@NotNull CompoundTag resultTag, @NotNull BrewType brewType, @NotNull BrewingRecipe recipe,
                                          String purityRepresentation) {
        resultTag.putString("recipeId", recipe.getId().toString());
        resultTag.putString(BREW_TYPE_ID_TAG, brewType.id()); // ← store brew type ID directly
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(
                Component.Serializer.toJson(Component.literal(purityRepresentation))));
        loreList.add(StringTag.valueOf(
                Component.Serializer.toJson(Component.translatable(brewType.customLore()))));
        CompoundTag displayTag = resultTag.getCompound("display");
        displayTag.put("Lore", loreList);
        displayTag.putString("Name",
                Component.Serializer.toJson(Component.translatable(brewType.customName())
                        .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW))));
        resultTag.put("display", displayTag);
        if (hasCustomTexture(brewType)) {
            resultTag.putString("customTexture", brewType.customTexture());
        }
    }

    /// Produces an "unfinished brew" or a failed brew from the cauldron state.
    public ItemStack toItem(BrewingRecipe recipe, long brewingTime, ItemStackHandler inventory) {
        ItemStack stack = itemFromBrewType(this);

        List<ItemStack> inventoryList = new ArrayList<>(inventory.getSlots());
        for (int j = 0; j < inventory.getSlots(); j++) {
            ItemStack inputStack = inventory.getStackInSlot(j);
            if (!inputStack.isEmpty()) {
                inventoryList.add(inputStack.copy());
            }
        }

        // Fail if the set of input item types does not match expected
        Set<Item> expectedItems = new HashSet<>(recipe.getInputs().stream().map(ItemStackInput::item).toList());
        Set<Item> actualItems = new HashSet<>(inventoryList.stream().map(ItemStack::getItem).toList());
        if (!expectedItems.equals(actualItems)) {
            return WRONG_INGREDIENTS_BREW();
        }

        // Fail if any item count is off
        for (ItemStackInput input : recipe.getInputs()) {
            int totalCount = 0;
            for (ItemStack is : inventoryList) {
                if (is.getItem() == input.item()) {
                    totalCount += is.getCount();
                }
            }
            if (totalCount < input.minCount() || totalCount > input.maxCount()) {
                return INCORRECT_INGREDIENT_AMOUNT_BREW();
            }
        }

        // Fail if brewing time is not within the allowed range
        double brewingTimeError = Math.abs((double) brewingTime - recipe.getOptimalBrewingTime())
                / recipe.getOptimalBrewingTime();
        if (brewingTimeError > recipe.getMaxBrewingTimeError()) {
            return INCORRECT_BREWING_TIME_BREW();
        }

        // Calculate purity
        double normalizedBrewingTimeError = brewingTimeError / recipe.getMaxBrewingTimeError();
        int purity = (int) Math.round(maxPurity * (1 - normalizedBrewingTimeError));
        stack.getOrCreateTag().putInt("purity", purity);

        stack.setHoverName(Component.translatable("brewery.brew.unfinished_brew")
                .withStyle(style -> style.withItalic(false)));
        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(
                Component.Serializer.toJson(Component.translatable("brewery.brew.unfinished_brew_lore"))));
        stack.getOrCreateTag().getCompound("display").put("Lore", loreList);
        stack.getOrCreateTag().putString("recipeId", recipe.getId().toString());
        stack.getOrCreateTag().putString(BREW_TYPE_ID_TAG, this.id()); // ← store on unfinished too
        return stack;
    }

    /// Build a finalized brew ItemStack with display and effects, reused by all workstations.
    public static ItemStack buildFinalBrew(BrewingRecipe recipe, int effectivePurity, Level level) {
        BrewType brewType = getBrewTypeFromId(recipe.getBrewTypeId());
        if (brewType == null) return GENERIC_FAILED_BREW();

        int maxPurity = brewType.maxPurity();
        int clampedPurity = Math.max(0, Math.min(effectivePurity, maxPurity));
        float purityFactor = maxPurity <= 0 ? 0f : ((float) clampedPurity / (float) maxPurity);

        ItemStack resultItem = recipe.getResultItem(level.registryAccess());
        CompoundTag resultTag = resultItem.getOrCreateTag();

        String purityRepresentation = "★".repeat(Math.max(0, clampedPurity))
                + "☆".repeat(Math.max(0, maxPurity - clampedPurity));

        applyFinalDisplay(resultTag, brewType, recipe, purityRepresentation);

        List<MobEffectInstance> resultEffects = buildScaledEffects(brewType, clampedPurity, purityFactor);
        resultTag.put("CustomPotionEffects", serializeEffects(resultEffects));

        resultItem.setTag(resultTag);
        return resultItem;
    }

    /// Build an overaged brew with aging time included in the tag.
    public static ItemStack buildFinalBrew(BrewingRecipe recipe, int effectivePurity, Level level, long agingTime) {
        ItemStack brew = buildFinalBrew(recipe, effectivePurity, level);
        CompoundTag tag = brew.getOrCreateTag();
        tag.putLong("agingTime", agingTime);
        tag.getCompound("display").getList("Lore", Tag.TAG_STRING).add(
                StringTag.valueOf(Component.Serializer.toJson(
                        Component.translatable(agingTime / 24000 == 1
                                ? "brewery.brew.overaged_singular_lore"
                                : "brewery.brew.overaged_plural_lore", agingTime / 24000))));
        brew.setTag(tag);
        return brew;
    }

    /// Finalizes a brew during barrel aging.
    public static ItemStack finalizeBarrelBrew(BrewingRecipe recipe, ItemStack inputStack,
                                               long progress, String barrelWoodType, Level level) {
        BrewType brewType = getBrewTypeFromId(recipe.getBrewTypeId());
        if (brewType == null) return GENERIC_FAILED_BREW();

        CompoundTag inputTag = inputStack.getOrCreateTag();
        int actualPurity = inputTag.getInt("purity");

        if (recipe.requiresDistilling()) {
            if (!inputTag.getString("distillingItem").equals(recipe.getDistillingItem())) {
                return GENERIC_FAILED_BREW();
            }
        }
        if (!recipe.requiresAging()) return INCORRECT_AGING_BREW();
        if (barrelWoodType == null || !recipe.getAllowedWoodTypes().contains(barrelWoodType)) {
            return INCORRECT_AGING_BREW();
        }
        if (inputTag.contains("CustomPotionEffects") && !inputTag.getList("CustomPotionEffects", Tag.TAG_COMPOUND).isEmpty()) {
            return SPOILED_BREW();
        }
        float maxError = recipe.getMaxAgingTimeError();
        long maxTime = Math.round(recipe.getOptimalAgingTime() * (1 + maxError));
        long minTime = Math.round(recipe.getOptimalAgingTime() * (1 - maxError));
        if (progress < minTime) {
            return INCORRECT_AGING_BREW();
        } else if (progress > maxTime && !brewType.isOverageable()) {
            return SPOILED_BREW();
        }
        float error = (brewType.isOverageable() && progress >= recipe.getOptimalAgingTime())
                ? 0.0f
                : (float) Math.abs(progress - recipe.getOptimalAgingTime()) / recipe.getOptimalAgingTime();
        float errorContribution = (float) Math.pow(1.0f - error, 2);
        int effectivePurity = Math.round(actualPurity * errorContribution);

        if (brewType.isOverageable()) {
            return buildFinalBrew(recipe, effectivePurity, level, progress);
        } else {
            return buildFinalBrew(recipe, effectivePurity, level);
        }
    }

    /// Finalizes a brew during distillery processing.
    public static ItemStack finalizeDistilleryBrew(BrewingRecipe recipe, ItemStack inputStack,
                                                   ItemStack filterStack, Level level) {
        BrewType brewType = getBrewTypeFromId(recipe.getBrewTypeId());
        if (brewType == null) return GENERIC_FAILED_BREW();

        CompoundTag inputTag = inputStack.getOrCreateTag();
        int actualPurity = inputTag.getInt("purity");

        if (!recipe.requiresDistilling() || !inputTag.getString("distillingItem").isEmpty()) {
            return INCORRECT_DISTILLERY_BREW();
        }
        String filterItemId = filterStack.getItem().toString();
        if (!recipe.getDistillingItem().equals(filterItemId)) {
            return INCORRECT_DISTILLERY_BREW();
        }

        ItemStack resultItem = inputStack.copyWithCount(1);
        CompoundTag resultTag = resultItem.getOrCreateTag();
        resultTag.putString("distillingItem", filterItemId);

        if (!recipe.requiresAging()) {
            ItemStack finalized = buildFinalBrew(recipe, actualPurity, level);
            finalized.getOrCreateTag().putString("distillingItem", filterItemId);
            return finalized;
        }

        resultItem.setTag(resultTag);
        return resultItem;
    }

    /// Returns the perfect brew for a recipe, with best stats possible.
    public static ItemStack finalizePerfectBrew(BrewingRecipe recipe, Level level, long agingTime) {
        BrewType brewType = getBrewTypeFromId(recipe.getBrewTypeId());
        if (brewType == null) return GENERIC_FAILED_BREW();

        int maxPurity = brewType.maxPurity();

        ItemStack resultItem = buildFinalBrew(recipe, maxPurity, level);
        CompoundTag resultTag = resultItem.getOrCreateTag();

        if (brewType.isOverageable()) {
            resultTag.getCompound("display").getList("Lore", Tag.TAG_STRING).add(
                    StringTag.valueOf(Component.Serializer.toJson(
                            Component.translatable(agingTime / 24000 == 1
                                    ? "brewery.brew.overaged_singular_lore"
                                    : "brewery.brew.overaged_plural_lore", agingTime / 24000))));
        }

        resultItem.setTag(resultTag);
        resultItem.setHoverName(Component.translatable(brewType.customName())
                .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW)));
        return resultItem;
    }

    // ── Brew identification ──

    /**
     * Checks if a finished brew ItemStack matches the given BrewType ID.
     * Uses the dedicated "brewTypeId" NBT tag for reliable identification,
     * with a fallback to recipeId-based lookup for brews created before this tag existed.
     */
    public static boolean isBrewType(ItemStack stack, String brewTypeId) {
        if (stack.getItem() != Items.POTION) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("CustomPotionEffects")) {
            return false;
        }

        // Primary: check the dedicated brewTypeId tag
        if (tag.contains(BREW_TYPE_ID_TAG)) {
            String storedId = tag.getString(BREW_TYPE_ID_TAG);
            return storedId.equals(brewTypeId);
        }

        // Fallback: try to resolve via recipeId (for legacy brews without the tag)
        if (tag.contains("recipeId")) {
            String recipeId = tag.getString("recipeId");
            String path = recipeId.contains(":") ? recipeId.substring(recipeId.indexOf(':') + 1) : recipeId;
            BrewType brewType = getBrewTypeFromId(path);
            BreweryMod.LOGGER.debug("isBrewType fallback — recipeId: {}, path: {}, resolved: {}, target: {}",
                    recipeId, path, brewType, brewTypeId);
            return brewType != null && brewType.id().equals(brewTypeId);
        }

        return false;
    }

    /**
     * Builds a perfect brew ItemStack directly from a BrewType, without requiring a recipe.
     * Used by commands and anywhere a BrewType exists without a corresponding BrewingRecipe.
     *
     * @param brewType  The BrewType to build from
     * @param agingTime The aging time in ticks (used for overaged lore display)
     * @return A finished brew with max purity and full effects
     */
    public static @NotNull ItemStack buildPerfectBrewFromType(@NotNull BrewType brewType, long agingTime) {
        int maxPurity = brewType.maxPurity();
        float purityFactor = 1.0f;

        ItemStack resultItem = new ItemStack(Items.POTION);
        CompoundTag resultTag = resultItem.getOrCreateTag();

        // Purity stars (all filled)
        String purityRepresentation = "★".repeat(maxPurity);
        resultTag.putString(BREW_TYPE_ID_TAG, brewType.id());
        resultTag.putInt("CustomPotionColor", brewType.tintColor());

        ListTag loreList = new ListTag();
        loreList.add(StringTag.valueOf(
                Component.Serializer.toJson(Component.literal(purityRepresentation))));
        loreList.add(StringTag.valueOf(
                Component.Serializer.toJson(Component.translatable(brewType.customLore()))));
        if (brewType.isOverageable()) {
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.translatable(agingTime / 24000 == 1
                            ? "brewery.brew.overaged_singular_lore"
                            : "brewery.brew.overaged_plural_lore", agingTime / 24000))));
        }

        CompoundTag displayTag = resultTag.getCompound("display");
        displayTag.put("Lore", loreList);
        displayTag.putString("Name",
                Component.Serializer.toJson(Component.translatable(brewType.customName())
                        .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW))));
        resultTag.put("display", displayTag);

        if (hasCustomTexture(brewType)) {
            resultTag.putString("customTexture", brewType.customTexture());
        }

        // Full effects (purityFactor = 1.0, no hangover)
        List<MobEffectInstance> resultEffects = new ArrayList<>();
        for (MobEffectInstance effect : brewType.effects()) {
            if (effect.getDuration() > 0) {
                resultEffects.add(new MobEffectInstance(
                        effect.getEffect(), effect.getDuration(), effect.getAmplifier()));
            }
        }
        resultTag.put("CustomPotionEffects", serializeEffects(resultEffects));

        resultItem.setTag(resultTag);
        return resultItem;
    }
}
