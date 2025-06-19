package io.fabianbuthere.brewery.event;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandling {
    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private static long remainingAlcoholInversionTicks = 0L;
    private static byte alcoholInversionState = 0;

    private static double amplifierToJumbleProbability(float x) {return Math.max(0, 0.5 - (2.25 / (x + 4) ));}

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
         String originalMessage = event.getMessage();
         LocalPlayer player = Minecraft.getInstance().player;

         if (player != null && player.hasEffect(ModEffects.ALCOHOL.get())) {
             int amplifier = player.getEffect(ModEffects.ALCOHOL.get()).getAmplifier(); // We know this is non null due to the previous check
             StringBuilder modifiedMessage = new StringBuilder();
             for (char c : originalMessage.toCharArray()) {
                 if (player.getRandom().nextFloat() < amplifierToJumbleProbability(amplifier) && Character.isLetter(c)) {
                     int index = Character.toLowerCase(c) - 'a';
                     if (index >= 0 && index < ALPHABET.length) {
                         int shiftedIndex = (index + player.getRandom().nextIntBetweenInclusive(1, 4) + 26) % ALPHABET.length;
                         char shiftedChar = ALPHABET[shiftedIndex];
                         modifiedMessage.append(Character.isUpperCase(c) ? Character.toUpperCase(shiftedChar) : shiftedChar);
                     } else {
                         modifiedMessage.append(c);
                     }
                 } else {
                     modifiedMessage.append(c);
                 }
             }
            event.setMessage(modifiedMessage.toString());
         }

    }

    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            float forward = player.input.forwardImpulse;
            float strafe = player.input.leftImpulse;
            if (strafe != 0 || forward != 0 && !player.input.shiftKeyDown) {
                if (player.hasEffect(ModEffects.ALCOHOL.get())) {
                    if (remainingAlcoholInversionTicks <= 0 && (player.getRandom().nextFloat() < 0.015f * (player.getEffect(ModEffects.ALCOHOL.get()).getAmplifier() + 1))) {
                        remainingAlcoholInversionTicks = player.getRandom().nextIntBetweenInclusive(10, 20);
                        alcoholInversionState = (byte) player.getRandom().nextIntBetweenInclusive(0, 3);
                    } else if (remainingAlcoholInversionTicks > 0) {
                        player.input.forwardImpulse = strafe * (alcoholInversionState == 1 || alcoholInversionState == 2 ? -1 : 1);
                        player.input.leftImpulse = forward * (alcoholInversionState == 3 || alcoholInversionState == 2 ? -1 : 1);
                        remainingAlcoholInversionTicks--;
                    }
                } else {
                    if (remainingAlcoholInversionTicks <= 0) {
                        remainingAlcoholInversionTicks = 0L;
                    }
                }
            }
        }
    }
}
