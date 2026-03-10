package io.fabianbuthere.brewery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.recipe.BrewingRecipe;
import io.fabianbuthere.brewery.recipe.ModRecipes;
import io.fabianbuthere.brewery.util.BrewType;
import io.fabianbuthere.brewery.util.BrewTypeRegistry;
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
                                            String brewTypeId = StringArgumentType.getString(context, "brew_type");
                                            BrewType brewType = BrewType.getBrewTypeFromId(brewTypeId);
                                            if (brewType == null) {
                                                context.getSource().sendFailure(Component.literal("Unknown brew_type: " + brewTypeId));
                                                return 0;
                                            }
                                            ItemStack result = BrewType.buildPerfectBrewFromType(brewType, 24000);
                                            context.getSource().getPlayerOrException().getInventory().add(result);
                                            context.getSource().sendSuccess(() -> Component.literal("Given brew '" + brewTypeId + "'!"), false);
                                            return 1;
                                        })
                                        .then(Commands.argument("age", LongArgumentType.longArg(0))
                                                .executes(context -> {
                                                    String brewTypeId = StringArgumentType.getString(context, "brew_type");
                                                    long age = LongArgumentType.getLong(context, "age");
                                                    BrewType brewType = BrewType.getBrewTypeFromId(brewTypeId);
                                                    if (brewType == null) {
                                                        context.getSource().sendFailure(Component.literal("Unknown brew_type: " + brewTypeId));
                                                        return 0;
                                                    }
                                                    ItemStack result = BrewType.buildPerfectBrewFromType(brewType, age * 24000);
                                                    context.getSource().getPlayerOrException().getInventory().add(result);
                                                    context.getSource().sendSuccess(() -> Component.literal("Given brew '" + brewTypeId + "' aged for " + age + " days!"), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("list_recipes")
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    Set<String> brewingRecipes = getAllBrewRecipeIds(server);
                                    if (brewingRecipes.isEmpty()) {
                                        context.getSource().sendFailure(Component.literal("No brewing recipes found."));
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.literal("Available brewing recipes: " + String.join(", ", brewingRecipes.stream().sorted().toList())), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("list_brews")
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    Set<String> brewTypes = getAllBrewTypeIds(server);
                                    if (brewTypes.isEmpty()) {
                                        context.getSource().sendFailure(Component.literal("No brew types found."));
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.literal("Available brew types: " + String.join(", ", brewTypes.stream().sorted().toList())), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("drink")
                                .then(Commands.argument("brew_type", StringArgumentType.word())
                                        .suggests(CommandEvents::suggestBrewTypes)
                                        .executes(context -> {
                                            String brewTypeId = StringArgumentType.getString(context, "brew_type");
                                            BrewType brewType = BrewType.getBrewTypeFromId(brewTypeId);
                                            if (brewType == null) {
                                                context.getSource().sendFailure(Component.literal("Brew type not found: " + brewTypeId));
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

    private static CompletableFuture<Suggestions> suggestBrewRecipes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        Set<String> brewTypes = getAllBrewRecipeIds(server);
        for (String brewTypeId : brewTypes) {
            builder.suggest(brewTypeId);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestBrewTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Set<String> brewTypes = BrewTypeRegistry.getAll().keySet();
        for (String brewTypeId : brewTypes) {
            builder.suggest(brewTypeId);
        }
        return builder.buildFuture();
    }

    private static Set<String> getAllBrewRecipeIds(MinecraftServer server) {
        Set<String> brewRecipes = new HashSet<>();
        ServerLevel level = server.overworld();
        for (BrewingRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.BREWING_RECIPE_TYPE)) {
            brewRecipes.add(recipe.getId().toString());
        }
        return brewRecipes;
    }

    private static Set<String> getAllBrewTypeIds(MinecraftServer server) {
        return new HashSet<>(BrewTypeRegistry.getAll().keySet());
    }
}
