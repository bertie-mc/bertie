package io.github.bertie_mc.fletching;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BertieFletching.ID)
public final class BertieFletching {
    public static final String ID = "bertiefletching";
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredHolder<MenuType<?>, MenuType<FletchingMenu>> MENU =
            MENUS.register("fletching", () -> IMenuTypeExtension.create(FletchingMenu::new));

    public BertieFletching(IEventBus bus) {
        MENUS.register(bus);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BertieFletching::interact);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BertieFletching::breakTable);
    }

    private static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE)
                || event.getEntity().isShiftKeyDown()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, ignored) -> new FletchingMenu(id, inventory, event.getPos()),
                            Component.translatable("block.minecraft.fletching_table")),
                    buffer -> buffer.writeBlockPos(event.getPos()));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    private static void breakTable(BlockEvent.BreakEvent event) {
        if (event.getState().is(Blocks.FLETCHING_TABLE) && event.getLevel() instanceof ServerLevel server)
            TankStorage.get(server).remove(event.getPos());
    }
}
