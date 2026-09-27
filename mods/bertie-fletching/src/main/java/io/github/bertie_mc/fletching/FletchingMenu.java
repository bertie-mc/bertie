package io.github.bertie_mc.fletching;

import com.fletchery.mod.inventory.FletchingInventoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class FletchingMenu extends AbstractContainerMenu {
    public static final int RESULT = 4, PLAYER_START = 5, PLAYER_END = 41, TANK_DISPLAY = 41;
    private final Level level;
    private final BlockPos pos;
    private final SimpleContainer inputs;
    private final SimpleContainer result = new SimpleContainer(1);
    private final SimpleContainer display = new SimpleContainer(1);
    private final PotionTank tank;
    private final ContainerListener listener = ignored -> refresh();
    private boolean consuming;
    private int clientBatches;

    public FletchingMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, data.readBlockPos());
    }

    public FletchingMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(BertieFletching.MENU.get(), id);
        this.level = playerInventory.player.level();
        this.pos = pos.immutable();
        this.inputs = level instanceof ServerLevel server
                ? FletchingInventoryManager.get(server).getInventory(pos)
                : new SimpleContainer(5);
        this.tank =
                level instanceof ServerLevel server ? TankStorage.get(server).at(pos) : new PotionTank();
        for (int i = 0; i < 4; i++) {
            final int index = i;
            addSlot(new Slot(inputs, i, i == 3 ? 98 : 8 + i * 18, i == 3 ? 8 : 48) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return ArrowRecipe.accepts(index, stack);
                }
            });
        }
        addSlot(new Slot(result, 0, 98, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++) addSlot(new Slot(playerInventory, 9 + y * 9 + x, 8 + x * 18, 84 + y * 18));
        for (int x = 0; x < 9; x++) addSlot(new Slot(playerInventory, x, 8 + x * 18, 142));
        addSlot(new Slot(display, 0, -1000, -1000) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return false;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return tank.batches();
            }

            @Override
            public void set(int value) {
                clientBatches = value;
            }
        });
        inputs.addListener(listener);
        // Old versions persisted the result preview. It is not a real crafted item.
        if (!level.isClientSide) inputs.setItem(4, ItemStack.EMPTY);
        refresh();
    }

    public ItemStack preview() {
        return result.getItem(0);
    }

    public ItemStack tankDisplay() {
        return display.getItem(0);
    }

    public int tankBatches() {
        return level.isClientSide ? clientBatches : tank.batches();
    }

    private void refresh() {
        if (consuming || level.isClientSide) return;
        ItemStack crafted =
                ArrowRecipe.craft(inputs.getItem(0), inputs.getItem(1), inputs.getItem(2), inputs.getItem(3), tank);
        if (!ItemStack.matches(crafted, preview())) result.setItem(0, crafted);
        ItemStack bottle = tank.displayStack();
        if (!ItemStack.matches(bottle, tankDisplay())) display.setItem(0, bottle);
        dirty();
    }

    private void dirty() {
        if (level instanceof ServerLevel server) {
            FletchingInventoryManager.get(server).setDirty();
            TankStorage.get(server).setDirty();
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || level.isClientSide || !stillValid(player)) return false;
        ItemStack carried = getCarried();
        if (carried.getCount() != 1 || !tank.fill(carried)) return false;
        setCarried(new ItemStack(Items.GLASS_BOTTLE));
        refresh();
        broadcastChanges();
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId == TANK_DISPLAY) return;
        if (slotId != RESULT) {
            super.clicked(slotId, button, type, player);
            return;
        }
        if (level.isClientSide || !stillValid(player)) return;
        refresh();
        ItemStack crafted = preview().copy();
        if (crafted.isEmpty()) return;
        if (type == ClickType.QUICK_MOVE) {
            quickMoveStack(player, RESULT);
        } else if (type == ClickType.PICKUP && (button == 0 || button == 1)) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                setCarried(crafted);
                consumeBatch(player);
            } else if (ItemStack.isSameItemSameComponents(carried, crafted)
                    && carried.getCount() + ArrowRecipe.BATCH <= carried.getMaxStackSize()) {
                carried.grow(ArrowRecipe.BATCH);
                setCarried(carried);
                consumeBatch(player);
            }
        } else if (type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)) {
            ItemStack held = player.getInventory().getItem(button);
            if (held.isEmpty()) {
                player.getInventory().setItem(button, crafted);
                consumeBatch(player);
            } else if (ItemStack.isSameItemSameComponents(held, crafted)
                    && held.getCount() + ArrowRecipe.BATCH <= held.getMaxStackSize()) {
                held.grow(ArrowRecipe.BATCH);
                consumeBatch(player);
            }
        } else if (type == ClickType.THROW && getCarried().isEmpty()) {
            player.drop(crafted, true);
            consumeBatch(player);
        } else if (type == ClickType.CLONE
                && player.getAbilities().instabuild
                && getCarried().isEmpty()) {
            setCarried(crafted.copyWithCount(crafted.getMaxStackSize()));
        }
        broadcastChanges();
    }

    private void consumeBatch(Player player) {
        ItemStack crafted = preview().copy();
        consuming = true;
        try {
            for (int i = 0; i < 4; i++) if (!inputs.getItem(i).isEmpty()) inputs.removeItem(i, 1);
            tank.consume();
        } finally {
            consuming = false;
        }
        refresh();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level instanceof ServerLevel server) {
            var data = crafted.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (data != null) {
                var tag = data.copyTag();
                var effects = tag.getList("potionEffects", 10);
                String potion = effects.isEmpty() ? "" : effects.getCompound(0).getString("id");
                boolean fresh = com.fletchery.mod.stat.ArrowCraftTracker.get(server)
                        .registerCraft(
                                serverPlayer.getUUID(),
                                tag.getString("feather"),
                                tag.getString("shaft"),
                                tag.getString("tip"),
                                tag.getString("effect"),
                                potion);
                com.fletchery.mod.advancement.ModCriteria.ARROW_CRAFTED.get().trigger(serverPlayer);
                serverPlayer.awardStat(com.fletchery.mod.stat.ModStats.ARROWS_CRAFTED, ArrowRecipe.BATCH);
                if (fresh) serverPlayer.awardStat(com.fletchery.mod.stat.ModStats.UNIQUE_COMBOS_CRAFTED);
            }
        }
    }

    private int spaceFor(ItemStack stack) {
        int space = 0;
        for (int i = PLAYER_START; i < PLAYER_END; i++) {
            Slot slot = getSlot(i);
            ItemStack existing = slot.getItem();
            if (existing.isEmpty()) space += slot.getMaxStackSize(stack);
            else if (ItemStack.isSameItemSameComponents(existing, stack))
                space += Math.max(0, slot.getMaxStackSize(stack) - existing.getCount());
        }
        return space;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (level.isClientSide || !stillValid(player) || index < 0 || index >= PLAYER_END) return ItemStack.EMPTY;
        Slot slot = getSlot(index);
        ItemStack source = slot.getItem();
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack original = source.copy();
        if (index == RESULT) {
            // Stop at a coating change, including the tank becoming empty.
            while (ItemStack.isSameItemSameComponents(preview(), original)
                    && !preview().isEmpty()
                    && spaceFor(preview()) >= ArrowRecipe.BATCH) {
                ItemStack batch = preview().copy();
                if (!moveItemStackTo(batch, PLAYER_START, PLAYER_END, true) || !batch.isEmpty()) break;
                consumeBatch(player);
            }
            return ItemStack.EMPTY;
        }
        if (PotionTank.isPotion(source)) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (source.getCount() > 1 && spaceFor(bottle) == 0) return ItemStack.EMPTY;
            if (!tank.fill(source)) return ItemStack.EMPTY;
            if (source.getCount() == 1) slot.set(bottle);
            else {
                source.shrink(1);
                moveItemStackTo(bottle, PLAYER_START, PLAYER_END, false);
                slot.setChanged();
            }
            refresh();
            return ItemStack.EMPTY;
        }
        if (index < 4) {
            if (!moveItemStackTo(source, PLAYER_START, PLAYER_END, false)) return ItemStack.EMPTY;
        } else {
            int target = -1;
            for (int i = 0; i < 4; i++)
                if (ArrowRecipe.accepts(i, source)) {
                    target = i;
                    break;
                }
            if (target < 0 || !moveItemStackTo(source, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        slot.setChanged();
        refresh();
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != RESULT && slot.index != TANK_DISPLAY && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return level.getBlockState(pos).is(Blocks.FLETCHING_TABLE)
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inputs.removeListener(listener);
        dirty();
    }
}
