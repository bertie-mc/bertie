package io.github.bertie_mc.bertieprogression.hooks;

import dev.thecodewarrior.hooked.hooks.HookBehaviors;
import dev.thecodewarrior.hooked.item.ChainAppearance;
import dev.thecodewarrior.hooked.item.HookModelInfo;
import dev.thecodewarrior.hooked.item.ItemComponents;
import dev.thecodewarrior.hooked.neoforge.HookItemNeoForge;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import io.github.bertie_mc.bertieprogression.ModItems;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import org.joml.Vector3f;

/**
 * Bertie Progression's hook progression on top of {@code hooked} (thecodewarrior).
 * Only wired when the mod is loaded; the containing class is otherwise never referenced.
 * <p>
 * Registers four new hook items in this mod's namespace, and rebalances the four built-in
 * hooks (wood, iron, diamond, ender) via {@link ModifyDefaultComponentsEvent}. The rename
 * from iron/diamond/ender → deep/cognitive/crystal is display-only via the lang overlay
 * under {@code assets/hooked/lang/}.
 */
public final class HookIntegration {
    private HookIntegration() {}

    // Registrations are kept as Suppliers so the actual HookItemNeoForge instance is only
    // resolved during item-registry population, which happens after all mod constructors run.
    public static Supplier<Item> SOUL_HOOK;
    public static Supplier<Item> BLAZING_HOOK;
    public static Supplier<Item> MOON_HOOK;
    public static Supplier<Item> ECHO_HOOK;

    public static void init(IEventBus modBus) {
        SOUL_HOOK    = registerBasic("soul_hook",    3, 16.0, 0.8, 13, 0.8);
        BLAZING_HOOK = registerBasic("blazing_hook", 3, 24.0, 1.2,  5, 1.2);
        MOON_HOOK    = registerBasic("moon_hook",    3, 28.0, 1.4,  3, 1.4);
        ECHO_HOOK    = registerBasic("echo_hook",    4, 32.0, 1.6,  2, 1.6);
        modBus.addListener(HookIntegration::onModifyDefaults);
    }

    private static DeferredItem<Item> registerBasic(
            String id, int count, double range, double speed, int cooldownTicks, double pullStrength) {
        return ModItems.ITEMS.register(id, () -> {
            HookModelInfo def = HookModelInfo.Companion.getDEFAULT();
            HookModelInfo model = new HookModelInfo(
                    def.getModel(),
                    ResourceLocation.fromNamespaceAndPath(
                            BertieProgression.MODID, "textures/hook/" + id + "/hook.png"),
                    def.getHookLength());
            ChainAppearance chain = new ChainAppearance(
                    ResourceLocation.fromNamespaceAndPath(
                            BertieProgression.MODID, "textures/hook/" + id + "/chain1.png"),
                    ResourceLocation.fromNamespaceAndPath(
                            BertieProgression.MODID, "textures/hook/" + id + "/chain2.png"),
                    /* playerGap */ 0.0,
                    Optional.empty(),
                    Optional.empty());
            Item.Properties props = new Item.Properties()
                    .stacksTo(1)
                    .component(ItemComponents.INSTANCE.getHOOK_COUNT(), count)
                    .component(ItemComponents.INSTANCE.getHOOK_RANGE(), range)
                    .component(ItemComponents.INSTANCE.getHOOK_SPEED(), speed)
                    .component(ItemComponents.INSTANCE.getFIRE_COOLDOWN(), cooldownTicks)
                    .component(
                            ItemComponents.INSTANCE.getHOOK_BEHAVIOR(),
                            HookBehaviors.INSTANCE.getBASIC())
                    .component(ItemComponents.INSTANCE.getPULL_STRENGTH(), pullStrength)
                    .component(ItemComponents.INSTANCE.getHOOK_MODEL(), model)
                    .component(ItemComponents.INSTANCE.getCHAIN_APPEARANCE(), chain);
            return new HookItemNeoForge(props);
        });
    }

    private static void onModifyDefaults(ModifyDefaultComponentsEvent event) {
        // Wood: max & range unchanged (1 / 8), speed unchanged (0.4); slower re-fire and stronger reel.
        event.modify(item("wood_hook"), b -> {
            b.set(ItemComponents.INSTANCE.getFIRE_COOLDOWN(), 32);
            b.set(ItemComponents.INSTANCE.getPULL_STRENGTH(), 0.4);
        });
        // Deep (was iron): shorter range, slower everything, still a beginner hook.
        event.modify(item("iron_hook"), b -> {
            b.set(ItemComponents.INSTANCE.getHOOK_COUNT(), 1);
            b.set(ItemComponents.INSTANCE.getHOOK_RANGE(), 12.0);
            b.set(ItemComponents.INSTANCE.getHOOK_SPEED(), 0.6);
            b.set(ItemComponents.INSTANCE.getFIRE_COOLDOWN(), 20);
            b.set(ItemComponents.INSTANCE.getPULL_STRENGTH(), 0.6);
        });
        // Cognitive (was diamond): fewer hooks, tighter range, same cooldown as vanilla diamond.
        event.modify(item("diamond_hook"), b -> {
            b.set(ItemComponents.INSTANCE.getHOOK_COUNT(), 2);
            b.set(ItemComponents.INSTANCE.getHOOK_RANGE(), 20.0);
            b.set(ItemComponents.INSTANCE.getHOOK_SPEED(), 1.0);
            b.set(ItemComponents.INSTANCE.getFIRE_COOLDOWN(), 8);
            b.set(ItemComponents.INSTANCE.getPULL_STRENGTH(), 1.0);
        });
        // Crystal (was ender): identical stats, only the shatter-particle colour re-tinted from
        // ender's purple to a dark→bright aqua that matches the crystal-matrix palette overlay.
        // Chain textures and playerGap match hooked's default for ender_hook - the event's
        // patch builder cannot read the previous value, so the full ChainAppearance is rebuilt.
        event.modify(item("ender_hook"), b -> b.set(
                ItemComponents.INSTANCE.getCHAIN_APPEARANCE(),
                new ChainAppearance(
                        ResourceLocation.fromNamespaceAndPath(
                                "hooked", "textures/hook/ender_hook/chain1.png"),
                        ResourceLocation.fromNamespaceAndPath(
                                "hooked", "textures/hook/ender_hook/chain2.png"),
                        /* playerGap */ 0.0,
                        Optional.of(new Vector3f(0.08f, 0.32f, 0.30f)),
                        Optional.of(new Vector3f(0.60f, 0.95f, 0.93f)))));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hooked", path));
    }
}
