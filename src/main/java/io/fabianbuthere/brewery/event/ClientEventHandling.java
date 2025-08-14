package io.fabianbuthere.brewery.event;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
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
    private static boolean alcoholShadersEnabled = false;
    private static byte alcoholInversionState = 0;

    private static double amplifierToJumbleProbability(float x) {return Math.max(0, (-1.6 / (x + 1.2)) + 0.84 );}

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

    private static void doPlayerBoatInversion(Minecraft mc, LocalPlayer player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Boat boat)) return;

        // Only modify if the player is the one controlling the boat
        if (boat.getControllingPassenger() != player) return;

        Options options = mc.options;
        boolean up = options.keyUp.isDown();
        boolean down = options.keyDown.isDown();
        boolean left = options.keyLeft.isDown();
        boolean right = options.keyRight.isDown();

        // Do nothing if not actually trying to move, or if sneaking (dismount key)
        if (!(up || down || left || right) || player.input.shiftKeyDown) return;

        if (!player.hasEffect(ModEffects.ALCOHOL.get())) {
            // Clear if effect wore off
            if (remainingAlcoholInversionTicks <= 0) {
                remainingAlcoholInversionTicks = 0L;
            }
            return;
        }

        // Trigger/continue inversion similarly to on-foot logic
        if (remainingAlcoholInversionTicks <= 0) {
            if (player.getRandom().nextFloat() < 0.015f * (player.getEffect(ModEffects.ALCOHOL.get()).getAmplifier() + 1)) {
                remainingAlcoholInversionTicks = player.getRandom().nextIntBetweenInclusive(10, 20);
                alcoholInversionState = (byte) player.getRandom().nextIntBetweenInclusive(0, 3);
            } else {
                return; // Not in an inversion window, leave default boat controls
            }
        }

        // Convert key booleans to signed axes
        int fwdAxis = (up ? 1 : 0) + (down ? -1 : 0);      // +1 forward, -1 backward
        int strAxis = (left ? 1 : 0) + (right ? -1 : 0);   // +1 left, -1 right

        // Apply the same swap/invert mapping as player input
        int mappedFwd = strAxis * ((alcoholInversionState == 1 || alcoholInversionState == 2) ? -1 : 1);
        int mappedStr = fwdAxis * ((alcoholInversionState == 3 || alcoholInversionState == 2) ? -1 : 1);

        // Convert back to key booleans
        boolean mappedUp = mappedFwd > 0;
        boolean mappedDown = mappedFwd < 0;
        boolean mappedLeft = mappedStr > 0;
        boolean mappedRight = mappedStr < 0;

        // Override boat input for this tick.
        boat.setInput(mappedLeft, mappedRight, mappedUp, mappedDown);

        remainingAlcoholInversionTicks--;
    }

    @SuppressWarnings("removal")
    public static void doPlayerAlcoholShaders(Minecraft mc, LocalPlayer player) {
        if (player.hasEffect(ModEffects.ALCOHOL.get())) {
            if (!alcoholShadersEnabled) {
                float amplifier = player.getEffect(ModEffects.ALCOHOL.get()).getAmplifier();
                if (player.getRandom().nextFloat() < 0.001f * (amplifier - 1)) {
                    mc.gameRenderer.loadEffect(new ResourceLocation("minecraft", "shaders/post/deconverge.json"));
                    alcoholShadersEnabled = true;
                } else if (player.getRandom().nextFloat() < 0.002f * (amplifier - 2)) {
                    mc.gameRenderer.loadEffect(new ResourceLocation("minecraft", "shaders/post/phosphor.json"));
                    alcoholShadersEnabled = true;
                } else if (player.getRandom().nextFloat() < 0.001f * (amplifier - 2)) {
                    mc.gameRenderer.loadEffect(new ResourceLocation("minecraft", "shaders/post/blobs2.json"));
                    alcoholShadersEnabled = true;
                }
            } else if (player.getRandom().nextFloat() < 0.001f) {
                alcoholShadersEnabled = false;
                mc.gameRenderer.shutdownEffect();
            }
        } else {
            alcoholShadersEnabled = false;
            mc.gameRenderer.shutdownEffect();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        doPlayerBoatInversion(mc, player);
        doPlayerAlcoholShaders(mc, player);
    }

    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            float forward = player.input.forwardImpulse;
            float strafe = player.input.leftImpulse;

            if ((strafe != 0 || forward != 0) && !player.input.shiftKeyDown) {
                if (player.hasEffect(ModEffects.ALCOHOL.get())) {
                    if (remainingAlcoholInversionTicks <= 0 && (player.getRandom().nextFloat() < 0.015f * (player.getEffect(ModEffects.ALCOHOL.get()).getAmplifier() + 1))) {
                        remainingAlcoholInversionTicks = player.getRandom().nextIntBetweenInclusive(10, 20);
                        alcoholInversionState = (byte) player.getRandom().nextIntBetweenInclusive(0, 3);
                    } else if (remainingAlcoholInversionTicks > 0) {
                        // Swap and optionally invert axes
                        player.input.forwardImpulse = strafe * ((alcoholInversionState == 1 || alcoholInversionState == 2) ? -1 : 1);
                        player.input.leftImpulse = forward * ((alcoholInversionState == 3 || alcoholInversionState == 2) ? -1 : 1);
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
