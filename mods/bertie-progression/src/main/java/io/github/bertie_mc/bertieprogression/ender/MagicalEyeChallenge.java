package io.github.bertie_mc.bertieprogression.ender;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MagicalEyeChallenge {
    public static boolean onManaPaid(ServerPlayer player, float before, float after) {
        if (!(before > 0) || after != 0 || !player.getOffhandItem().is(Items.ENDER_EYE)) return false;
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("endrem:magical_eye")));
        if (result.isEmpty()) return false;
        ItemStack input = player.getOffhandItem();
        input.shrink(1);
        if (input.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, result);
        } else {
            player.getInventory().add(result);
            if (!result.isEmpty()) player.drop(result, false);
        }
        return true;
    }

    private MagicalEyeChallenge() {}
}
