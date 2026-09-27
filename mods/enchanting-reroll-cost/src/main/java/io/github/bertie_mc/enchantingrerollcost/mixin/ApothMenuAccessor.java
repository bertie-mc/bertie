package io.github.bertie_mc.enchantingrerollcost.mixin;

import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ApothEnchantmentMenu.class)
public interface ApothMenuAccessor {
    @Accessor("stats")
    EnchantmentTableStats rerollCost$getStats();
}
