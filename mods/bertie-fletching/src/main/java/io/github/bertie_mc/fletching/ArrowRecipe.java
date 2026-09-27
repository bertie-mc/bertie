package io.github.bertie_mc.fletching;

import com.fletchery.mod.registry.ModRegistries;
import com.fletchery.mod.screen.FletchingTableScreenHandler;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

public final class ArrowRecipe {
    public static final int BATCH = 4;
    public static final List<Item> FEATHERS = List.of(Items.FEATHER, Items.PHANTOM_MEMBRANE, Items.WHEAT);
    public static final List<Item> SHAFTS = List.of(
            Items.STICK, Items.BLAZE_ROD, Items.BREEZE_ROD, Items.BONE, Items.END_ROD, Items.FISHING_ROD, Items.CHAIN);
    public static final List<Item> TIPS = List.of(
            Items.FLINT,
            Items.AMETHYST_SHARD,
            Items.PRISMARINE_SHARD,
            Items.ECHO_SHARD,
            Items.QUARTZ,
            Items.GOLD_INGOT,
            Items.COPPER_INGOT,
            Items.SHULKER_SHELL,
            Items.IRON_INGOT,
            Items.DIAMOND,
            Items.NETHERITE_INGOT,
            Items.HEAVY_CORE);
    public static final List<Item> EFFECTS = List.of(
            Items.GUNPOWDER,
            Items.GLOWSTONE_DUST,
            Items.SLIME_BALL,
            Items.HONEYCOMB,
            Items.ENDER_PEARL,
            Items.BLAZE_POWDER,
            Items.TURTLE_HELMET,
            Items.DRAGON_BREATH,
            Items.HEART_OF_THE_SEA,
            Items.WIND_CHARGE,
            Items.LAPIS_LAZULI,
            Items.TORCH,
            Items.FIRE_CHARGE,
            Items.FIREWORK_STAR,
            Items.FIREWORK_ROCKET);

    private ArrowRecipe() {}

    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> FEATHERS.contains(stack.getItem());
            case 1 -> SHAFTS.contains(stack.getItem());
            case 2 -> TIPS.contains(stack.getItem());
            case 3 -> EFFECTS.contains(stack.getItem());
            default -> false;
        };
    }

    public static ItemStack craft(
            ItemStack feather, ItemStack shaft, ItemStack tip, ItemStack effect, PotionTank tank) {
        if (!accepts(0, feather)
                || !accepts(1, shaft)
                || !accepts(2, tip)
                || (!effect.isEmpty() && !accepts(3, effect))) return ItemStack.EMPTY;
        boolean basic = feather.is(Items.FEATHER) && shaft.is(Items.STICK) && tip.is(Items.FLINT);
        if (basic && tank.batches() == 0) {
            if (effect.isEmpty()) return new ItemStack(Items.ARROW, BATCH);
            if (effect.is(Items.GLOWSTONE_DUST)) return new ItemStack(Items.SPECTRAL_ARROW, BATCH);
        }
        CompoundTag tag = new CompoundTag();
        String f = key(feather), s = key(shaft), t = key(tip);
        tag.putString("feather", f);
        tag.putString("shaft", s);
        tag.putString("tip", t);
        if (!effect.isEmpty()) tag.putString("effect", key(effect));
        int model = FletchingTableScreenHandler.computeModelDataWithEffect(f, s, t, effect);
        tag.putInt("modelData", model);
        tag.putBoolean("bertieCoating", tank.batches() > 0);
        if (tank.batches() > 0) {
            tag.putBoolean("isPotionEffect", true);
            // Upstream forwards potion visuals only behind this legacy flag. Coating duration is handled separately.
            tag.putBoolean("isLingeringPotion", true);
            tag.putString(
                    "potionName", "#" + Integer.toHexString(tank.contents().getColor()));
            tag.putInt("bertiePotionColor", tank.contents().getColor());
            tag.put(
                    "bertiePotionContents",
                    net.minecraft.world.item.alchemy.PotionContents.CODEC
                            .encodeStart(
                                    net.minecraft.resources.RegistryOps.create(
                                            net.minecraft.nbt.NbtOps.INSTANCE,
                                            net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                                                    BuiltInRegistries.REGISTRY)),
                                    tank.contents())
                            .getOrThrow());
            ListTag effects = new ListTag();
            for (MobEffectInstance instance : tank.contents().getAllEffects()) {
                CompoundTag e = new CompoundTag();
                e.putString(
                        "id",
                        BuiltInRegistries.MOB_EFFECT
                                .getKey(instance.getEffect().value())
                                .toString());
                e.putInt("duration", instance.getDuration());
                e.putInt("amplifier", instance.getAmplifier());
                effects.add(e);
            }
            tag.put("potionEffects", effects);
        }
        ItemStack result = new ItemStack(ModRegistries.CUSTOM_ARROW.get(), BATCH);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        result.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(model));
        if (tank.batches() > 0) result.set(DataComponents.POTION_CONTENTS, tank.contents());
        return result;
    }

    private static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
