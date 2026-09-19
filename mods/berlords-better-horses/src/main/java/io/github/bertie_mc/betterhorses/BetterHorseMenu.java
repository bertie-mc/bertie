package io.github.bertie_mc.betterhorses;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class BetterHorseMenu extends AbstractContainerMenu {
    public static final int STORAGE_START = 39;
    public static final int STORAGE_END = STORAGE_START + 15;
    public final Horse horse;
    private final SimpleContainer saddle;

    public static BetterHorseMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var entity = inventory.player.level().getEntity(buffer.readVarInt());
        if (!(entity instanceof Horse horse))
            throw new IllegalStateException("Horse is not available for its inventory");
        return new BetterHorseMenu(id, inventory, horse);
    }

    public BetterHorseMenu(int id, Inventory inventory, Horse horse) {
        super(BetterHorses.HORSE_MENU.get(), id);
        this.horse = horse;
        HorseEquipment equipment = (HorseEquipment) horse;
        saddle = equipment.betterhorses$saddleInventory();
        saddle.startOpen(inventory.player);
        addSlot(new Slot(saddle, 0, 8, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return horse.isSaddleable()
                        && (stack.is(Items.SADDLE) || stack.getItem() instanceof SpecialSaddleItem)
                        && (equipment.betterhorses$storage().isEmpty() || stack.is(BetterHorses.WANDERER.get()));
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPickup(Player player) {
                return horse.getPassengers().size() < 2
                        && equipment.betterhorses$storage().isEmpty();
            }
        });
        addSlot(new Slot(horse.getBodyArmorAccess(), 0, 8, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return horse.isBodyArmorItem(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPickup(Player player) {
                return player.isCreative()
                        || !net.minecraft.world.item.enchantment.EnchantmentHelper.has(
                                getItem(),
                                net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
            }
        });
        addSlot(new Slot(equipment.betterhorses$shoes(), 0, 8, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof HorseshoeItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        // Keep slot indices stable while equipping or removing the traveller saddle.
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 5; col++)
                addSlot(new Slot(equipment.betterhorses$storage(), col + row * 5, 80 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return hasStorage();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return hasStorage();
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return hasStorage();
                    }
                });
    }

    public boolean hasStorage() {
        return saddle.getItem(0).is(BetterHorses.WANDERER.get());
    }

    @Override
    public boolean stillValid(Player player) {
        return horse.isAlive()
                && horse.isTamed()
                && !horse.hasInventoryChanged(saddle)
                && player.canInteractWithEntity(horse, 4);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saddle.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved = false;
        if (index < 3 || index >= STORAGE_START) moved = moveItemStackTo(stack, 3, STORAGE_START, true);
        else {
            if (getSlot(2).mayPlace(stack)) moved = moveItemStackTo(stack, 2, 3, false);
            else if (getSlot(1).mayPlace(stack)) moved = moveItemStackTo(stack, 1, 2, false);
            else if (getSlot(0).mayPlace(stack)) moved = moveItemStackTo(stack, 0, 1, false);
            if (!moved && hasStorage()) moved = moveItemStackTo(stack, STORAGE_START, STORAGE_END, false);
            if (!moved)
                moved = index < 30 ? moveItemStackTo(stack, 30, 39, false) : moveItemStackTo(stack, 3, 30, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
