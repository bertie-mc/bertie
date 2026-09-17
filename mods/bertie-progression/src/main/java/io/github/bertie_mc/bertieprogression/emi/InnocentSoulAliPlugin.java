package io.github.bertie_mc.bertieprogression.emi;

import com.yanny.aci.api.RangeValue;
import com.yanny.aci.tooltip.TooltipBuilder;
import com.yanny.ali.api.AliEntrypoint;
import com.yanny.ali.api.IDataNode;
import com.yanny.ali.api.ILootModifier;
import com.yanny.ali.api.IOperation;
import com.yanny.ali.api.IPlugin;
import com.yanny.ali.api.IServerRegistry;
import com.yanny.ali.plugin.common.nodes.ItemNode;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.bertieprogression.ModItems;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * Puts the Innocent Soul on the villager's drop page in Advanced Loot Info.
 *
 * <p>It is added by a global loot modifier of a type ALI cannot decode, so the villager's page listed
 * an unnamed modifier slot and nothing else: the item read as having no source. The drop page is
 * where this belongs rather than an info page of its own — it is where a player looks, and it already
 * carries every other thing a villager drops.
 *
 * <p>Discovery mirrors EMI's: {@link AliEntrypoint} is a CLASS-retention (RuntimeInvisible)
 * annotation that ALI finds by scanning, so this class is only ever loaded when Advanced Loot Info
 * is installed. Do NOT give it {@code @Retention(RUNTIME)}.
 */
@AliEntrypoint
public class InnocentSoulAliPlugin implements IPlugin {

    @Override
    public String getModId() {
        return BertieProgression.MODID;
    }

    @Override
    public void registerServer(IServerRegistry registry) {
        registry.registerLootModifiers(utils -> {
            ItemStack soul = new ItemStack(ModItems.INNOCENT_SOUL.get());
            if (soul.isEmpty()) {
                return List.of();
            }
            IDataNode node = new ItemNode(
                    1.0F,
                    new RangeValue(1, 1),
                    soul,
                    TooltipBuilder.value("Killed by a player").build(),
                    List.of(),
                    List.of());
            return List.of(new VillagerDrop(List.of(new IOperation.AddOperation(any -> true, node))));
        });
    }

    private record VillagerDrop(List<IOperation> operations) implements ILootModifier<Entity> {

        @Override
        public boolean predicate(Entity entity) {
            return entity != null && entity.getType() == EntityType.VILLAGER;
        }

        @Override
        public List<IOperation> getOperations() {
            return operations;
        }

        @Override
        public IType<Entity> getType() {
            return IType.ENTITY;
        }
    }
}
