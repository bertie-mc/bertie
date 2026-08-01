package com.berlord.foodsystem.test;

import com.berlord.foodsystem.mixin.UpgradeWrapperBaseAccessor;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(value = FoodSystemClientTestMod.MOD_ID, dist = Dist.CLIENT)
public final class FoodSystemClientTestMod {
    static final String MOD_ID = "berlordsfoodsystemtest";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FoodSystemClientTestMod(IEventBus modBus) {
        modBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            try {
                assertMethods(Class.forName("net.minecraft.world.item.ItemStack"), "bfs$saveDurability");
                assertMethods(Class.forName("net.minecraft.world.entity.LivingEntity"), "bfs$climbWalls");
                assertMethods(Class.forName("net.minecraft.client.gui.Gui"), "bfs$hideFoodBar");
                assertMethods(Class.forName(
                                "net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeWrapper"),
                        "bfs$gateFeeding", "bfs$endFeeding", "bfs$onTickHead", "bfs$clearTickPlayer",
                        "bfs$scanCadence");
                Class<?> wrapper = Class.forName("net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase");
                if (!UpgradeWrapperBaseAccessor.class.isAssignableFrom(wrapper)) {
                    throw new IllegalStateException("Sophisticated Core wrapper is missing the accessor mixin");
                }
                LOGGER.info("BERLORDS_FOOD_SYSTEM_MIXINS_OK");
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Food-system integration classes are unavailable", failure);
            }
        });
    }

    private static void assertMethods(Class<?> target, String... fragments) {
        Set<String> methods = Arrays.stream(target.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        for (String fragment : fragments) {
            if (methods.stream().noneMatch(name -> name.contains(fragment))) {
                throw new IllegalStateException(target.getName() + " is missing " + fragment);
            }
        }
    }
}
