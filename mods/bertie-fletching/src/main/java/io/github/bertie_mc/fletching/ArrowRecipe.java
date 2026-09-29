package io.github.bertie_mc.fletching;

import com.fletchery.mod.registry.ModRegistries;
import com.fletchery.mod.screen.FletchingTableScreenHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

public final class ArrowRecipe {
    public static final int BATCH = 8;

    private ArrowRecipe() {}

    public static boolean accepts(int slot, ItemStack stack) {
        return PartCatalog.find(slot, stack) != null;
    }

    public static ItemStack craft(
            ItemStack feather, ItemStack shaft, ItemStack tip, ItemStack effect, PotionTank tank) {
        if (!accepts(0, feather)
                || !accepts(1, shaft)
                || !accepts(2, tip)
                || (!effect.isEmpty() && !accepts(3, effect))) return ItemStack.EMPTY;
        CompoundTag tag = new CompoundTag();
        tag.putInt("bertieVersion", 2);
        String f = key(feather), s = key(shaft), t = key(tip);
        tag.putString("feather", f);
        tag.putString("shaft", s);
        tag.putString("tip", t);
        if (!effect.isEmpty()) tag.putString("effect", key(effect));
        int model = FletchingTableScreenHandler.computeBaseModelData(
                "minecraft:" + PartCatalog.find(0, feather).visual(),
                "minecraft:" + PartCatalog.find(1, shaft).visual(),
                "minecraft:" + PartCatalog.find(2, tip).visual());
        tag.putInt("modelData", model);
        tag.putBoolean("bertieCoating", tank.batches() > 0);
        if (tank.batches() > 0) {
            tag.putBoolean("isPotionEffect", true);
            // Upstream forwards potion visuals only behind this legacy flag. Coating duration is handled separately.
            tag.putBoolean("isLingeringPotion", true);
            tag.putString("potionName", CoatingColor.encode(tank.contents().getColor()));
            tag.putInt("bertiePotionColor", tank.contents().getColor() & 0xffffff);
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

    public static ItemStack fromData(CompoundTag tag, int count, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(ModRegistries.CUSTOM_ARROW.get(), count);
        CompoundTag copy = tag.copy();
        copy.remove("bertieFlight");
        copy.remove("bertieNoRecovery");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(copy));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(copy.getInt("modelData")));
        if (copy.contains("bertiePotionContents"))
            net.minecraft.world.item.alchemy.PotionContents.CODEC
                    .parse(
                            net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries),
                            copy.get("bertiePotionContents"))
                    .result()
                    .ifPresent(contents -> stack.set(DataComponents.POTION_CONTENTS, contents));
        return stack;
    }

    private static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
