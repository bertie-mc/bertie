package io.github.bertie_mc.bertieprogression.anvil;

import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import java.util.ArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/** Paired collisions consume their exact support block, without a surrounding-block explosion. */
public final class PairedAnvilCollision {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void collide(AnvilEvent.CollisionBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!blockId.getNamespace().equals("iceandfire") || !blockId.getPath().startsWith("dragonsteel_")) return;
        boolean pairedTarget = false;
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get())) {
            if (!holder.id().getNamespace().equals("bertieprogression")
                    || !holder.id().getPath().startsWith("anvil_collision/dragonsteel_")) continue;
            var recipe = holder.value();
            if (!recipe.hitBlock().test(level, state, level.getBlockEntity(pos))) continue;
            pairedTarget = true;
            if (event.getSpeed() < recipe.speed()
                    || event.getSpeed() < AnvilCraft.CONFIG.anvilCollisionCraftSpeed
                    || !recipe.anvil().test(level, event.getEntity().getBlockState(), null)
                    || recipe.transformBlocks().size() != 1) continue;
            var base = pos.below();
            if (!recipe.transformBlocks()
                    .getFirst()
                    .inputBlock()
                    .test(level, level.getBlockState(base), level.getBlockEntity(base))) continue;
            event.setCanceled(true);
            event.setAnvilDamage(false);
            level.removeBlock(pos, false);
            level.removeBlock(base, false);
            var results = new ArrayList<ItemStack>();
            for (ChanceItemStack result : recipe.outputItems()) {
                ItemStack stack = result.getResult(level);
                if (!stack.isEmpty()) {
                    results.add(stack.copy());
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1, 1);
            TriggerUtil.recipe(level, pos, holder.id(), results);
            return;
        }
        // The ordinary resolver cannot express the required vertical pair and would accept a lone target.
        if (pairedTarget) event.setCanceled(true);
    }

    private PairedAnvilCollision() {}
}
