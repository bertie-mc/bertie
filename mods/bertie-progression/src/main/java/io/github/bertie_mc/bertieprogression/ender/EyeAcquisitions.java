package io.github.bertie_mc.bertieprogression.ender;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class EyeAcquisitions {
    private static final Map<UUID, Map<EquipmentSlot, ItemStack>> CONVERTING = new HashMap<>();
    public static final Map<String, Double> SCULK_CHANCES = Map.of(
            "deeperdarker:sculk_leech",
            0.001,
            "deeperdarker:sculk_snapper",
            0.005,
            "deeperdarker:sculk_centipede",
            0.01,
            "deeperdarker:shattered",
            0.04,
            "deeperdarker:stalker",
            0.08,
            "deeperdarker:shriek_worm",
            0.15);
    public static final Set<String> NETHER_BOSSES = Set.of(
            "minecraft:wither",
            "armageddon_mod:zoranththe_forgotten_one",
            "cataclysm:ignis",
            "cataclysm:netherite_monstrosity",
            "block_factorys_bosses:underworld_knight",
            "irons_spellbooks:fire_boss");

    public static ItemStack eye(String name) {
        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath("endrem", name))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        double chance = SCULK_CHANCES.getOrDefault(type, 0.0);
        String result = "sculk_eye";
        if (NETHER_BOSSES.contains(type)) {
            chance = 0.07;
            result = "nether_eye";
        }
        if (chance > 0 && entity.getRandom().nextDouble() < chance) {
            ItemStack stack = eye(result);
            if (!stack.isEmpty())
                event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack));
        }
    }

    public static void beginConversion(Piglin piglin) {
        if (eye("cursed_eye").isEmpty()) return;
        Map<EquipmentSlot, ItemStack> held = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack stack = piglin.getItemBySlot(slot);
            if (stack.is(Items.ENDER_EYE)) {
                held.put(slot, stack.copy());
                piglin.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        if (!held.isEmpty()) CONVERTING.put(piglin.getUUID(), held);
    }

    public static void finishConversion(Piglin piglin) {
        var remaining = CONVERTING.remove(piglin.getUUID());
        if (remaining == null) return;
        remaining.forEach((slot, stack) -> {
            if (!piglin.isRemoved() && piglin.getItemBySlot(slot).isEmpty()) piglin.setItemSlot(slot, stack);
            else piglin.spawnAtLocation(stack);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void zombified(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof Piglin)
                || !(event.getOutcome() instanceof ZombifiedPiglin piglin)
                || piglin.level().isClientSide()) return;
        var captured = CONVERTING.remove(event.getEntity().getUUID());
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack held = piglin.getItemBySlot(slot);
            ItemStack original = captured == null ? ItemStack.EMPTY : captured.getOrDefault(slot, ItemStack.EMPTY);
            if (!original.isEmpty() || held.is(Items.ENDER_EYE)) {
                ItemStack result = eye("cursed_eye");
                if (result.isEmpty()) continue;
                result.setCount(original.isEmpty() ? held.getCount() : original.getCount());
                piglin.setItemSlot(slot, result);
                piglin.setDropChance(slot, 2.0F);
            }
        }
    }

    public static void enchanted(Player player, int cost) {
        if (cost >= 80
                && player instanceof ServerPlayer serverPlayer
                && player.getRandom().nextDouble() < 0.03) give(serverPlayer, "cryptic_eye");
    }

    public static void dolphinFed(Dolphin dolphin) {
        if (!dolphin.level().isClientSide() && dolphin.getRandom().nextDouble() < 0.20) {
            ItemStack result = eye("exotic_eye");
            if (!result.isEmpty()) dolphin.spawnAtLocation(result);
        }
    }

    public static void raidWon(ServerLevel level, BlockPos center) {
        ItemStack result = eye("corrupted_eye");
        if (!result.isEmpty())
            level.addFreshEntity(
                    new ItemEntity(level, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5, result));
    }

    private static void give(ServerPlayer player, String name) {
        ItemStack stack = eye(name);
        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
    }

    public static void wanderingTrades(WanderingTrader trader) {
        if (trader.level().isClientSide()) return;
        ItemStack result = eye("evil_eye");
        if (!result.isEmpty() && trader.getRandom().nextDouble() < 0.40) {
            trader.getOffers()
                    .add(new MerchantOffer(
                            new ItemCost(Items.EMERALD, 16 + trader.getRandom().nextInt(49)), result, 1, 1, 0));
        }
    }

    public static void clericTrades(Villager villager) {
        if (villager.level().isClientSide()) return;
        if (villager.getVillagerData().getProfession() != VillagerProfession.CLERIC
                || villager.getVillagerData().getLevel() != 4) return;
        var coin = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath("bertiecoins", "emerald_coin"))
                .filter(item -> item != Items.AIR);
        ItemStack result = eye("evil_eye");
        if (coin.isPresent() && !result.isEmpty() && villager.getRandom().nextDouble() < 0.30) {
            villager.getOffers().add(new MerchantOffer(new ItemCost(coin.get(), 64), result, 1, 1, 0));
        }
    }

    private EyeAcquisitions() {}
}
