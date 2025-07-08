package io.fabianbuthere.brewery.event;

import io.fabianbuthere.brewery.BreweryMod;
import io.fabianbuthere.brewery.effect.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BreweryMod.MOD_ID)
public class ServerEventHandling {
    private static final Map<UUID, Long> vomiting = new HashMap<>();

    @SubscribeEvent
    public static void onServerPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!event.player.level().isClientSide && event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) {
            if (!vomiting.containsKey(player.getUUID())) {
                vomiting.put(player.getUUID(), 0L);
            }

            MobEffectInstance alcohol = player.getEffect(ModEffects.ALCOHOL.get());
            if (alcohol != null) {
                if (vomiting.get(player.getUUID()) > 0) {
                    spawnVomitParticles(player);
                    vomiting.put(player.getUUID(), vomiting.get(player.getUUID()) - 1);
                } else {
                    int amplifier = alcohol.getAmplifier();
                    if (amplifier >= 3 && player.getRandom().nextFloat() < 0.0005f * (amplifier - 2)) {
                        vomiting.put(player.getUUID(), 60L);
                    }
                }
            } else {
                vomiting.put(player.getUUID(), 0L);
            }
        }
    }

    private static void spawnVomitParticles(ServerPlayer player) {
        double spread = 0.6;
        var dustOptions = new DustParticleOptions(
            new Vector3f(0.7F, 0.85F, 0.15F),
            1.0F
        );
        for (int i = 0; i < 30; i++) {
            double speed = 0.4 + player.getRandom().nextDouble() * 0.2;
            double yaw = Math.toRadians(player.getYRot());
            double pitch = Math.toRadians(player.getXRot());
            double xDir = -Math.sin(yaw) * Math.cos(pitch);
            double yDir = -Math.sin(pitch) + (player.getRandom().nextDouble() - 0.5) * 0.2;
            double zDir = Math.cos(yaw) * Math.cos(pitch);
            double px = player.getX() + xDir * 0.5 + (player.getRandom().nextDouble() - 0.5) * spread;
            double py = player.getY() + player.getEyeHeight() - 0.2 + (player.getRandom().nextDouble() - 0.5) * 0.25;
            double pz = player.getZ() + zDir * 0.5 + (player.getRandom().nextDouble() - 0.5) * spread;
            double vx = xDir * speed + (player.getRandom().nextDouble() - 0.5) * 0.1;
            double vy = yDir * speed + (player.getRandom().nextDouble() - 0.5) * 0.1;
            double vz = zDir * speed + (player.getRandom().nextDouble() - 0.5) * 0.1;
            ((ServerLevel)player.level()).sendParticles(dustOptions, px, py, pz, 1, vx, vy, vz, 0.0);
        }
    }
}