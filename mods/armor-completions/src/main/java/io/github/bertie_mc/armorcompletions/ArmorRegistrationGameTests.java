package io.github.bertie_mc.armorcompletions;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArmorCompletions.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArmorRegistrationGameTests {
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void allPiecesEquipWithTheirNativeMaterial(GameTestHelper helper) {
        helper.assertTrue(ArmorCompletions.PIECES.size() == 10, "Expected ten missing pieces");
        var stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
        ArmorCompletions.PIECES.forEach((piece, holder) -> {
            var armor = (ArmorItem)holder.get();
            helper.assertTrue(armor.getType() == piece.type(), "Incorrect slot for " + holder.getId());
            var original = ArmorCompletions.sourceArmor(piece.family());
            helper.assertTrue(armor.getMaterial().equals(original.getMaterial()), "Incorrect source material for " + holder.getId());
            ItemStack stack = new ItemStack(armor);
            stand.setItemSlot(piece.type().getSlot(), stack);
            helper.assertTrue(stand.getItemBySlot(piece.type().getSlot()).is(armor), "Could not equip " + holder.getId());
            helper.assertTrue(stack.getMaxStackSize() == 1, "Armor must not stack");
            helper.assertTrue(stack.getMaxDamage() > 0, "Armor durability missing");
            armor.getDefaultAttributeModifiers();
        });
        helper.succeed();
    }
}
