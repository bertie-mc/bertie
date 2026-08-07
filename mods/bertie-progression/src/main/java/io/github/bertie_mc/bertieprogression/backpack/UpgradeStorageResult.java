package io.github.bertie_mc.bertieprogression.backpack;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.RitualResult;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.RitualResultType;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A Hephaestus ritual result that turns the MAIN INGREDIENT into another item instead of building a
 * new one, carrying a Sophisticated storage's contents across.
 *
 * <p>Forbidden Arcanus ships {@code create_item}, which produces a fresh stack, and
 * {@code transmute_input}, which keeps the input's components but nothing else - the upgraded pack
 * would keep the previous tier's slot counts. This is {@code transmute_input} plus
 * {@link BackpackHandover#retier}.
 *
 * <p>On anything that is not a Sophisticated storage this behaves exactly like
 * {@code transmute_input}, so it is safe to use for any ritual that should hand the input's
 * identity to its result.
 */
public record UpgradeStorageResult(Holder<Item> result) implements RitualResult {

    public static final MapCodec<UpgradeStorageResult> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemStack.ITEM_NON_AIR_CODEC.fieldOf("result_item").forGetter(UpgradeStorageResult::result))
            .apply(instance, UpgradeStorageResult::new));

    @Override
    public ItemStack getResultItem(ItemStack mainInput) {
        ItemStack upgraded = mainInput.transmuteCopy(this.result.value(), 1);
        BackpackHandover.retier(upgraded);
        return upgraded;
    }

    @Override
    public RitualResultType<?> getType() {
        return ModRitualResults.UPGRADE_STORAGE.get();
    }
}
