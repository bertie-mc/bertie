package io.github.bertie_mc.enchantingrerollcost;

import com.kasch_x.easyapothcompat.EasyApothCompat;
import com.kasch_x.easyapothcompat.EasyApothConfig;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.util.MiscUtil;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import io.github.bertie_mc.enchantingrerollcost.mixin.ApothMenuAccessor;
import io.github.bertie_mc.enchantingrerollcost.mixin.EnchantmentMenuAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public final class RerollPrice {
    private RerollPrice() {}

    public static int minimumOfferLevel(float power) {
        // Match Apothic's rounding of effective Eterna before selecting the first offer.
        return Math.max(1, Math.round(Math.round(Math.max(1.5F, power)) * 0.2F));
    }

    public static int experienceCost(float power) {
        return MiscUtil.getExpCostForSlot(minimumOfferLevel(power), 0);
    }

    public static int experienceCost(ApothEnchantmentMenu menu, Player player) {
        return experienceCost(((ApothMenuAccessor) menu).rerollCost$getStats().eterna(player));
    }

    public static boolean canReroll(ApothEnchantmentMenu menu, Player player) {
        if (player == null
                || !EasyApothCompat.enabled
                || !EasyApothConfig.ENABLE_REROLL.get()
                || menu.getSlot(0).getItem().isEmpty()) return false;
        if (player.getAbilities().instabuild) return true;
        var lapis = menu.getSlot(1).getItem();
        return lapis.is(Items.LAPIS_LAZULI)
                && lapis.getCount() >= 1
                && EnchantmentUtils.getExperience(player) >= experienceCost(menu, player);
    }

    public static boolean reroll(ApothEnchantmentMenu menu, Player player) {
        if (player.level().isClientSide() || !menu.stillValid(player)) return false;
        menu.gatherStats();
        if (!canReroll(menu, player)) return false;
        if (!player.getAbilities().instabuild) {
            if (!EnchantmentUtils.chargeExperience(player, experienceCost(menu, player))) return false;
            menu.getSlot(1).getItem().shrink(1);
            menu.getSlot(1).setChanged();
        }
        player.onEnchantmentPerformed(menu.getSlot(0).getItem(), 0);
        ((EnchantmentMenuAccessor) menu).rerollCost$getSeed().set(player.getEnchantmentSeed());
        menu.slotsChanged(menu.getSlot(0).container);
        menu.broadcastChanges();
        return true;
    }
}
