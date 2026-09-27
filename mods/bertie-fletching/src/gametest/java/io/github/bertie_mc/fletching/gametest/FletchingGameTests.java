package io.github.bertie_mc.fletching.gametest;

import com.fletchery.mod.entity.CustomArrowEntity;
import com.fletchery.mod.registry.ModRegistries;
import io.github.bertie_mc.fletching.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BertieFletching.ID)
@PrefixGameTestTemplate(false)
public final class FletchingGameTests {
    private FletchingGameTests() {}

    private static ItemStack potion(Item item, Holder<Potion> type) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(type));
        return stack;
    }

    private record Table(Player player, FletchingMenu menu, BlockPos pos) {}

    @GameTest(template = "empty")
    public static void destroyingTableClearsStoredLiquidAndInvalidatesOpenMenu(GameTestHelper helper) {
        Table t = table(helper);
        t.menu().setCarried(potion(Items.POTION, Potions.POISON));
        t.menu().clickMenuButton(t.player(), 0);
        helper.getLevel().destroyBlock(t.pos(), true);
        helper.assertTrue(!t.menu().stillValid(t.player()), "destroyed table cannot be crafted through");
        helper.getLevel().setBlockAndUpdate(t.pos(), Blocks.FLETCHING_TABLE.defaultBlockState());
        var replacement = new FletchingMenu(2, t.player().getInventory(), t.pos());
        helper.assertTrue(
                replacement.tankBatches() == 0 && replacement.preview().isEmpty(),
                "new table cannot inherit removed tank or ingredients");
        replacement.removed(t.player());
        t.menu().removed(t.player());
        helper.succeed();
    }

    private static Table table(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.FLETCHING_TABLE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        player.getInventory().clearContent();
        FletchingMenu menu = new FletchingMenu(1, player.getInventory(), pos);
        menu.getSlot(0).set(new ItemStack(Items.FEATHER, 20));
        menu.getSlot(1).set(new ItemStack(Items.STICK, 20));
        menu.getSlot(2).set(new ItemStack(Items.FLINT, 20));
        menu.getSlot(3).set(new ItemStack(Items.GUNPOWDER, 20));
        return new Table(player, menu, pos);
    }

    @GameTest(template = "empty")
    public static void fillsReturnBottlesAndCraftCompleteBatches(GameTestHelper helper) {
        Table t = table(helper);
        var m = t.menu();
        m.setCarried(potion(Items.SPLASH_POTION, Potions.POISON));
        helper.assertTrue(m.clickMenuButton(t.player(), 0), "carried potion accepted");
        helper.assertTrue(m.getCarried().is(Items.GLASS_BOTTLE), "empty bottle remains on cursor");
        m.setCarried(ItemStack.EMPTY);
        m.clicked(4, 1, ClickType.PICKUP, t.player());
        helper.assertTrue(
                m.getCarried().getCount() == 4 && m.tankBatches() == 1, "right-click crafts four and spends one batch");
        helper.assertTrue(
                m.getSlot(0).getItem().getCount() == 19
                        && m.getSlot(3).getItem().getCount() == 19,
                "one of each material consumed");
        t.player().getInventory().setItem(9, potion(Items.LINGERING_POTION, Potions.POISON));
        m.quickMoveStack(t.player(), 5);
        helper.assertTrue(
                t.player().getInventory().getItem(9).is(Items.GLASS_BOTTLE),
                "shift-fill returns bottle to source slot");
        helper.assertTrue(m.tankBatches() == 3, "bottle supplies two batches");
        var saved = TankStorage.get(helper.getLevel())
                .at(t.pos())
                .save(helper.getLevel().registryAccess());
        var restored = PotionTank.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.batches() == 3 && restored.contents().equals(new PotionContents(Potions.POISON)),
                "tank round-trips through disk format");
        m.removed(t.player());
        var reopened = new FletchingMenu(2, t.player().getInventory(), t.pos());
        helper.assertTrue(reopened.tankBatches() == 3, "reopening preserves tank");
        reopened.removed(t.player());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryAndPartialSpaceNeverWasteIngredients(GameTestHelper helper) {
        Table t = table(helper);
        var m = t.menu();
        m.setCarried(potion(Items.POTION, Potions.POISON));
        m.clickMenuButton(t.player(), 0);
        m.setCarried(ItemStack.EMPTY);
        for (int i = 0; i < 36; i++) t.player().getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        m.clicked(4, 0, ClickType.QUICK_MOVE, t.player());
        helper.assertTrue(
                m.tankBatches() == 2 && m.getSlot(0).getItem().getCount() == 20, "full inventory consumes nothing");
        t.player().getInventory().setItem(0, m.preview().copyWithCount(61));
        m.clicked(4, 0, ClickType.QUICK_MOVE, t.player());
        helper.assertTrue(
                m.tankBatches() == 2 && t.player().getInventory().getItem(0).getCount() == 61,
                "three free spaces cannot consume a batch");
        t.player().getInventory().setItem(0, m.preview().copyWithCount(60));
        m.clicked(4, 0, ClickType.QUICK_MOVE, t.player());
        helper.assertTrue(
                m.tankBatches() == 1 && t.player().getInventory().getItem(0).getCount() == 64,
                "four free spaces receive exactly four arrows");
        m.removed(t.player());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftCraftStopsWhenCoatingRunsOutAndDoesNotDuplicateAcrossViewers(GameTestHelper helper) {
        Table t = table(helper);
        var m = t.menu();
        m.setCarried(potion(Items.POTION, Potions.POISON));
        m.clickMenuButton(t.player(), 0);
        m.setCarried(ItemStack.EMPTY);
        var other = new FletchingMenu(2, t.player().getInventory(), t.pos());
        m.clicked(4, 0, ClickType.QUICK_MOVE, t.player());
        int arrows = t.player().getInventory().countItem(ModRegistries.CUSTOM_ARROW.get());
        helper.assertTrue(arrows == 8 && m.tankBatches() == 0, "one potion coats exactly eight arrows");
        helper.assertTrue(m.getSlot(0).getItem().getCount() == 18, "shift-craft stops before uncoated batch");
        other.broadcastChanges();
        helper.assertTrue(
                !other.preview()
                        .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag()
                        .getBoolean("bertieCoating"),
                "second viewer sees emptied tank");
        m.removed(t.player());
        other.removed(t.player());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileAppliesItemAndPotionEffectsAndPersists(GameTestHelper helper) {
        PotionTank tank = new PotionTank();
        tank.fill(potion(Items.POTION, Potions.POISON));
        ItemStack crafted = ArrowRecipe.craft(
                new ItemStack(Items.FEATHER),
                new ItemStack(Items.STICK),
                new ItemStack(Items.FLINT),
                new ItemStack(Items.GLOWSTONE_DUST),
                tank);
        var arrow = new ImpactArrow(helper);
        arrow.setCustomProperties(crafted.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag());
        arrow.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 1));
        arrow.hit(pig);
        helper.assertTrue(
                pig.hasEffect(MobEffects.POISON) && pig.hasEffect(MobEffects.GLOWING),
                "item and potion effects both apply");
        helper.assertTrue(pig.getEffect(MobEffects.POISON).getDuration() == 112, "vanilla tipped-arrow duration");
        CompoundTag saved = new CompoundTag();
        arrow.addAdditionalSaveData(saved);
        var restored = new ImpactArrow(helper);
        restored.readAdditionalSaveData(saved);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(crafted, restored.pickup()),
                "retrieved arrow preserves potion components and stacks with its source");
        helper.assertTrue(
                restored.getCustomProperties().getBoolean("bertieCoating") && restored.getProps().glowTarget,
                "both effects survive entity save/load");
        helper.succeed();
    }

    private static final class ImpactArrow extends CustomArrowEntity {
        ImpactArrow(GameTestHelper helper) {
            super(ModRegistries.CUSTOM_ARROW_ENTITY.get(), helper.getLevel());
        }

        void hit(Pig pig) {
            onHitEntity(new EntityHitResult(pig));
        }

        ItemStack pickup() {
            return getPickupItem();
        }
    }
}
