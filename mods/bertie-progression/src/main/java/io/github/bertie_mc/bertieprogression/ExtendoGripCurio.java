package io.github.bertie_mc.bertieprogression;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Create's Extendo Grip, worn rather than held.
 *
 * <p>In the hand it is Create's item unchanged: it costs durability, needs a Brass Hand and an
 * empty offhand, and reaches further for both blocks and entities. Worn in a curio slot it does one
 * thing only — it extends BLOCK reach — and it does it for free: no durability, no offhand
 * requirement, no entity reach. That split is deliberate. The held grip stays the interesting item
 * and keeps its cost; the worn one is a quality-of-life range bump that should never quietly eat a
 * tool the player was relying on.
 *
 * <p>The modifier is attached through a capability rather than a datapack attribute so it applies
 * only while the stack sits in a slot. A data-driven modifier on the item would also fire in the
 * hand, doubling Create's own reach bonus.
 */
public final class ExtendoGripCurio {

    private static final ResourceLocation GRIP =
            ResourceLocation.fromNamespaceAndPath("create", "extendo_grip");

    /** Three blocks of extra block reach - the same bump Create's held grip gives. */
    private static final double BLOCK_REACH_BONUS = 3.0D;

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(BertieProgression.MODID, "worn_extendo_grip");

    @SubscribeEvent
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        if (!ModList.get().isLoaded("create") || !ModList.get().isLoaded("curios")) {
            return;
        }
        Item grip = BuiltInRegistries.ITEM.get(GRIP);
        if (grip == Items.AIR) {
            return;
        }
        event.registerItem(CuriosCapability.ITEM, (stack, ctx) -> new WornGrip(stack), grip);
    }

    private static final class WornGrip implements ICurio {

        private final ItemStack stack;

        private WornGrip(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getStack() {
            return stack;
        }

        @Override
        public boolean canEquipFromUse(SlotContext context) {
            return true;
        }

        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
                SlotContext context, ResourceLocation id) {
            Multimap<Holder<Attribute>, AttributeModifier> out = LinkedHashMultimap.create();
            out.put(
                    Attributes.BLOCK_INTERACTION_RANGE,
                    new AttributeModifier(
                            MODIFIER_ID, BLOCK_REACH_BONUS, AttributeModifier.Operation.ADD_VALUE));
            return out;
        }
    }

    private ExtendoGripCurio() {}
}
