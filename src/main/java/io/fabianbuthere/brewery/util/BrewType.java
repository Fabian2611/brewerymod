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
import net.minecraftforge.items.ItemStackHandler;

import java.util.*;

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

    public static boolean isValid(String id) {
        return Arrays.stream(ModBrewTypes.BREW_TYPES).map(BrewType::id).anyMatch(id::equals);
    }

    public static BrewType getResultBrewType(String id) {
        return Arrays.stream(ModBrewTypes.BREW_TYPES)
                .filter(brewType -> brewType.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static ItemStack getResultItem(String id) {
        if (getResultBrewType(id) != null) {
            return itemFromBrewType(getResultBrewType(id));
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
            listTag.add(effect.save(new net.minecraft.nbt.CompoundTag()));
        }
        return listTag;
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
            for (ItemStack inputStack : inventoryList) {
                if (inputStack.getItem() == i.item()) {
                    if (inputStack.getCount() < i.minCount() || inputStack.getCount() > i.maxCount()) {
                        return INCORRECT_INGREDIENT_AMOUNT_BREW();
                    }
                }
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
}
