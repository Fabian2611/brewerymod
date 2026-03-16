package io.fabianbuthere.brewery.datagen.loot;

import io.fabianbuthere.brewery.block.ModBlocks;
import io.fabianbuthere.brewery.block.custom.CoffeeCropBlock;
import io.fabianbuthere.brewery.item.ModItems;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        this.dropSelf(ModBlocks.BREWING_CAULDRON.get());
        this.dropSelf(ModBlocks.DISTILLERY_STATION.get());
        for (var barrel : ModBlocks.FERMENTATION_BARRELS.values()) {
            this.dropSelf(barrel.get());
        }
        this.dropSelf(ModBlocks.BREW_SHELF.get());
        this.dropSelf(ModBlocks.COCKTAIL_STATION.get());
        this.dropSelf(ModBlocks.COFFEE_MAKER.get());

        LootItemCondition.Builder lootitemcondition$builder = LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(ModBlocks.COFFEE_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CoffeeCropBlock.AGE, 7));
        this.add(ModBlocks.COFFEE_CROP.get(), applyExplosionDecay(ModBlocks.COFFEE_CROP.get(),
                LootTable.lootTable()
                        .withPool(LootPool.lootPool()
                                .add(LootItem.lootTableItem(ModItems.COFFEE_BEAN.get())))
                        .withPool(LootPool.lootPool()
                                .when(lootitemcondition$builder)
                                .add(LootItem.lootTableItem(ModItems.COFFEE_BEAN.get())
                                        .apply(ApplyBonusCount.addBonusBinomialDistributionCount(Enchantments.BLOCK_FORTUNE, 0.5714286F, 2))))
        ));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
