package io.github.bertie_mc.voidfog.client;

import io.github.bertie_mc.voidfog.VoidFogConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The grey motes drifting in the fog, and the tick that drives the sky scan.
 *
 * <p>White ash rather than a particle of our own: it is already a pale grey speck that falls slowly
 * and lights the way the fog wants, and using it means no texture, no registration and nothing to
 * keep in step with a resource pack.
 *
 * <p>Motes are placed in a shell around the camera rather than a filled sphere - scattering
 * uniformly through the volume puts most of them right on top of the player, where they read as
 * grit on the screen instead of depth.
 */
public final class VoidFogMotes {
    private static final RandomSource RANDOM = RandomSource.create();

    private VoidFogMotes() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        LocalPlayer player = client.player;
        if (level == null || player == null || client.isPaused()) {
            return;
        }

        // The sky scan reads about eighteen hundred columns, so it only runs where its answer
        // could matter: inside the depth band, before the sky factor is applied to it.
        if (VoidFogRenderer.depthStrength(player.getEyeY()) > 0.0F) {
            SkyProximity.tick(level, BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
        }

        if (!VoidFogConfig.PARTICLES.getAsBoolean()) {
            return;
        }
        float strength = VoidFogRenderer.strength(player.getEyeY());
        if (strength <= 0.0F) {
            return;
        }

        int wanted = Math.round(VoidFogConfig.PARTICLE_COUNT.getAsInt() * strength);
        int radius = VoidFogConfig.PARTICLE_RADIUS.getAsInt();
        for (int i = 0; i < wanted; i++) {
            // Cube root of a uniform draw biases the shell outward; without it the motes cluster
            // at the camera, since a sphere holds most of its volume near the rim anyway.
            double distance = radius * Math.cbrt(RANDOM.nextDouble());
            double yaw = RANDOM.nextDouble() * Math.PI * 2.0;
            double pitch = Math.acos(1.0 - 2.0 * RANDOM.nextDouble());
            double x = player.getX() + distance * Math.sin(pitch) * Math.cos(yaw);
            double y = player.getEyeY() + distance * Math.cos(pitch);
            double z = player.getZ() + distance * Math.sin(pitch) * Math.sin(yaw);
            level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
