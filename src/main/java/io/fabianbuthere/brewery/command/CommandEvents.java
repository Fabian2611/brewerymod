package io.fabianbuthere.brewery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.recipe.BrewingRecipe;
import io.fabianbuthere.brewery.recipe.ModRecipes;
import io.fabianbuthere.brewery.util.BrewType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID)
public class CommandEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("brewery")
                .requires(source -> source.isPlayer() && source.hasPermission(3))
                .then(Commands.literal("give")
                    .then(Commands.argument("brew_type", StringArgumentType.word())
                        .suggests(CommandEvents::suggestBrewTypes)
                        .executes(context -> {
                            String brewType = StringArgumentType.getString(context, "brew_type");
                            BrewingRecipe recipe = getRecipeByBrewTypeId(context.getSource().getServer(), brewType);
                            if (recipe == null) {
                                context.getSource().sendFailure(Component.literal("Unknown brew_type: " + brewType));
                                return 0;
                            }
                            ItemStack result = BrewType.finalizeBrew(recipe, context.getSource().getLevel());
                            context.getSource().getPlayerOrException().getInventory().add(result);
                            context.getSource().sendSuccess(() -> Component.literal("Given brew '" + brewType + "'!"), false);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("list")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        Set<String> brewTypes = getAllBrewTypeIds(server);
                        if (brewTypes.isEmpty()) {
                            context.getSource().sendFailure(Component.literal("No brew types found."));
                        } else {
                            context.getSource().sendSuccess(() -> Component.literal("Available brew types: " + String.join(", ", brewTypes)), false);
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("drink")
                    .then(Commands.argument("brew_type", StringArgumentType.word())
                        .suggests(CommandEvents::suggestBrewTypes)
                        .executes(context -> {
                            String brewTypeArg = StringArgumentType.getString(context, "brew_type");
                            BrewingRecipe recipe = getRecipeByBrewTypeId(context.getSource().getServer(), brewTypeArg);
                            if (recipe == null) {
                                context.getSource().sendFailure(Component.literal("Unknown brew_type: " + brewTypeArg));
                                return 0;
                            }
                            BrewType brewType = BrewType.getBrewTypeFromId(recipe.getBrewTypeId());
                            if (brewType == null) {
                                context.getSource().sendFailure(Component.literal("Brew type not found: " + brewTypeArg));
                                return 0;
                            }
                            for (MobEffectInstance effect : brewType.effects()) {
                                context.getSource().getPlayerOrException().addEffect(effect);
                            }
                            return 1;
                        })
                    )
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestBrewTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        Set<String> brewTypes = getAllBrewTypeIds(server);
        for (String brewTypeId : brewTypes) {
            builder.suggest(brewTypeId);
        }
        return builder.buildFuture();
    }

    private static Set<String> getAllBrewTypeIds(MinecraftServer server) {
        Set<String> brewTypes = new HashSet<>();
        ServerLevel level = server.overworld(); // Why tf is this not in the server instance?
        for (BrewingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.BREWING_TYPE)) {
            brewTypes.add(recipe.getBrewTypeId());
        }
        return brewTypes;
    }

    private static BrewingRecipe getRecipeByBrewTypeId(MinecraftServer server, String brewTypeId) {
        ServerLevel level = server.overworld();
        for (BrewingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.BREWING_TYPE)) {
            if (recipe.getBrewTypeId().equals(brewTypeId)) {
                return recipe;
            }
        }
        return null;
    }
}
