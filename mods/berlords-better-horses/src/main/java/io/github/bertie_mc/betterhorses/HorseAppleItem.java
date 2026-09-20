package io.github.bertie_mc.betterhorses;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class HorseAppleItem extends Item {
    public enum Effect {
        ZOMBIE,
        SKELETON,
        BREED
    }

    private final Effect effect;

    public HorseAppleItem(Effect effect) {
        super(new Properties());
        this.effect = effect;
    }

    public boolean canChange(Horse horse) {
        return effect == Effect.BREED || ((HorseAppearance) horse).betterhorses$appearance() != style();
    }

    public void apply(Horse horse) {
        if (effect == Effect.BREED) {
            Variant[] variants = Variant.values();
            int offset = 1 + horse.getRandom().nextInt(variants.length - 1);
            horse.setVariant(variants[(horse.getVariant().ordinal() + offset) % variants.length]);
        } else {
            ((HorseAppearance) horse).betterhorses$setAppearance(style());
        }
    }

    private HorseAppearance.Style style() {
        return effect == Effect.ZOMBIE ? HorseAppearance.Style.ZOMBIE : HorseAppearance.Style.SKELETON;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return target instanceof Horse horse && horse.isAlive() ? horse.fedFood(player, stack) : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.betterhorses." + effect.name().toLowerCase(Locale.ROOT) + "_apple"));
        if (effect != Effect.BREED) lines.add(Component.translatable("tooltip.betterhorses.appearance_revert"));
    }
}
