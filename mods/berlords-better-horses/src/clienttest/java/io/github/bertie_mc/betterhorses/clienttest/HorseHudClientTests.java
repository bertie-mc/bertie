package io.github.bertie_mc.betterhorses.clienttest;

import io.github.bertie_mc.betterhorses.*;
import io.github.bertie_mc.betterhorses.client.HorseFade;
import io.github.bertie_mc.testing.client.ClientTest;
import io.github.bertie_mc.testing.client.context.ClientTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.neoforged.fml.ModList;

public final class HorseHudClientTests {
    private HorseHudClientTests() {}

    @ClientTest
    public static void mountedHudAndFade(ClientTestContext context) throws ReflectiveOperationException {
        try (var world = context.worldBuilder().create()) {
            context.waitFor("player ready", client -> client.player != null && client.level != null);
            world.server().runCommand("gamemode survival @a");
            world.server().runCommand("experience add @a 8 levels");
            int horseId = world.server().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                Horse horse = EntityType.HORSE.create(player.serverLevel());
                horse.moveTo(player.getX(), player.getY(), player.getZ(), 0, 0);
                horse.setTamed(true);
                horse.setAge(0);
                horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);
                horse.setHealth(30);
                ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, BetterHorses.WARRIOR.toStack());
                horse.setBodyArmorItem(BetterHorses.ARMOR.toStack());
                player.serverLevel().addFreshEntity(horse);
                player.startRiding(horse);
                return horse.getId();
            });
            context.waitFor(
                    "mounted horse",
                    client -> client.player.getVehicle() instanceof Horse horse
                            && horse.getId() == horseId
                            && horse.onGround());
            context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            assertExperienceVisible(context, true);
            context.waitTicks(2);
            context.takeScreenshot("horse-hud-xp");
            context.runOnClient(client -> {
                int expectedRightHeight = ModList.get().isLoaded("berlordsfoodsystem") ? 59 : 69;
                if (client.gui.rightHeight != expectedRightHeight)
                    throw new AssertionError("Wrong hunger/mount stacking: " + client.gui.rightHeight);
            });
            context.input().holdKey(options -> options.keyJump);
            context.waitTicks(3);
            assertExperienceVisible(context, false);
            context.takeScreenshot("horse-hud-jump");
            context.input().releaseKey(options -> options.keyJump);
            context.waitTicks(2);
            assertExperienceVisible(context, true);
            context.waitFor("landed", client -> client.player.getVehicle().onGround());

            float[] pitches = {-30, 0, 15, 37.5F, 60, 90};
            float[] opacities = {1, 1, 1, 0.55F, 0.1F, 0.1F};
            for (int i = 0; i < pitches.length; i++) {
                float pitch = pitches[i], opacity = opacities[i];
                context.runOnClient(client -> {
                    client.player.setXRot(pitch);
                    client.player.xRotO = pitch;
                    float actual = HorseFade.alphaFor((Horse) client.player.getVehicle(), 1);
                    if (Math.abs(actual - opacity) > 0.001F)
                        throw new AssertionError("Wrong opacity at " + pitch + ": " + actual);
                });
                context.waitTicks(2);
                if (pitch >= 15) context.takeScreenshot("horse-fade-" + pitch);
            }
            context.runOnClient(client -> {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                if (HorseFade.alphaFor((Horse) client.player.getVehicle(), 1) != 1)
                    throw new AssertionError("Third-person horse should remain opaque");
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.player.setXRot(0);
                client.player.xRotO = 0;
            });
            world.server().runOnServer(server -> {
                Horse horse =
                        (Horse) server.getPlayerList().getPlayers().getFirst().getVehicle();
                ((HorseEquipment) horse).betterhorses$saddleInventory().setItem(0, BetterHorses.WANDERER.toStack());
            });
            context.waitFor(
                    "traveller equipped",
                    client -> ((HorseEquipment) client.player.getVehicle()).betterhorses$traveller());
            context.input().holdKey(options -> options.keyJump);
            context.waitTicks(2);
            assertExperienceVisible(context, true);
            context.takeScreenshot("traveller-hud-jump-xp");
        } finally {
            context.input().releaseKey(options -> options.keyJump);
        }
    }

    private static void assertExperienceVisible(ClientTestContext context, boolean expected)
            throws ReflectiveOperationException {
        context.runOnClient(client -> {
            var method = Gui.class.getDeclaredMethod("isExperienceBarVisible");
            method.setAccessible(true);
            if ((boolean) method.invoke(client.gui) != expected)
                throw new AssertionError("Wrong XP bar visibility while mounted, expected " + expected);
        });
    }
}
