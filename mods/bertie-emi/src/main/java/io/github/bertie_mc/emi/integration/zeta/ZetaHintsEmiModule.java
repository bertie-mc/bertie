package io.github.bertie_mc.emi.integration.zeta;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.violetmoon.zeta.Zeta;
import org.violetmoon.zeta.config.ConfigFlagManager;
import org.violetmoon.zeta.config.ConfigObjectMapper;
import org.violetmoon.zeta.event.load.ZGatherHints;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.util.Hint;
import org.violetmoon.zeta.util.RegistryUtil;
import org.violetmoon.zeta.util.zetalist.ZetaList;

/**
 * Quark and every other Zeta mod annotate their items with {@code @Hint}, a sentence explaining what
 * the item does or where it comes from — the answer for the many items whose whole behaviour is a
 * right-click handler with no recipe anywhere. Zeta ships a bridge that turns those into JEI item
 * info, and only that one; with EMI as the pack's viewer, all of it was unreachable, so items like
 * Cloud in a Bottle looked like they did nothing at all.
 *
 * <p>This is the same bridge against EMI. It fires Zeta's own gather event per registered mod, so the
 * hints come from the mods themselves and follow their config: a hint attached to a disabled module,
 * or gated on a config flag that is off, is not collected, exactly as Zeta intends.
 */
public final class ZetaHintsEmiModule {

    private ZetaHintsEmiModule() {}

    public static void register(EmiRegistry reg) {
        RegistryAccess access = Minecraft.getInstance().level.registryAccess();
        Map<Item, List<Component>> hints = new LinkedHashMap<>();
        for (Zeta zeta : ZetaList.INSTANCE.getZetas()) {
            try {
                zeta.loadBus.fire(new Collector(zeta.modid, access, hints), ZGatherHints.class);
            } catch (Throwable ignored) {
                // one mod's hints failing must not cost the others
            }
        }
        hints.forEach((item, lines) -> {
            EmiStack stack = EmiStack.of(item);
            if (stack.isEmpty()) {
                return;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            reg.addRecipe(new EmiInfoRecipe(
                    List.<EmiIngredient>of(stack),
                    lines,
                    ResourceLocation.fromNamespaceAndPath(
                            "bertieemi", "zeta/hint/" + itemId.getNamespace() + "/" + itemId.getPath())));
        });
    }

    /**
     * Zeta's gather contract. The walk over {@code @Hint} fields mirrors its own JEI implementation,
     * because the annotation's shape — a config flag that can invert, an optional key, and extra
     * fields interpolated into the sentence — is the contract, not an implementation detail. A field
     * may hold one item, a tag, or an iterable of items, and each form fans out to the same sentence.
     */
    private static final class Collector implements ZGatherHints {

        private final String modId;
        private final RegistryAccess access;
        private final Map<Item, List<Component>> out;

        private Collector(String modId, RegistryAccess access, Map<Item, List<Component>> out) {
            this.modId = modId;
            this.access = access;
            this.out = out;
        }

        @Override
        public void accept(ItemLike itemLike, Component hint) {
            out.computeIfAbsent(itemLike.asItem(), item -> new ArrayList<>()).add(hint);
        }

        @Override
        public void hintItem(ItemLike itemLike, Object... extra) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemLike.asItem());
            String prefix = id.getNamespace().equals(modId) ? "" : id.getNamespace() + ".";
            hintItem(itemLike, prefix + id.getPath(), extra);
        }

        @Override
        public void hintItem(ItemLike itemLike, String key, Object... extra) {
            accept(itemLike, Component.translatable(modId + ".jei.hint." + key, extra));
        }

        @Override
        public RegistryAccess getRegistryAccess() {
            return access;
        }

        @Override
        public void gatherHintsFromModule(ZetaModule module, ConfigFlagManager flags) {
            if (!module.isEnabled()) {
                return;
            }
            List<Field> fields = ConfigObjectMapper.walkModuleFields(module.getClass());
            Map<String, Field> byName = new HashMap<>();
            for (Field field : fields) {
                byName.put(field.getName(), field);
            }
            for (Field field : fields) {
                Hint hint = field.getAnnotation(Hint.class);
                if (hint == null) {
                    continue;
                }
                try {
                    apply(module, flags, byName, field, hint);
                } catch (Throwable ignored) {
                    // a single unreadable field is not worth losing the module's other hints
                }
            }
        }

        private void apply(
                ZetaModule module, ConfigFlagManager flags, Map<String, Field> byName, Field field, Hint hint)
                throws Exception {
            field.setAccessible(true);
            Object target = ConfigObjectMapper.getField(module, field);
            if (target == null) {
                return;
            }
            String flag = hint.value();
            if (!flag.isEmpty() && flags.getFlag(flag) == hint.negate()) {
                return;
            }
            List<Object> extra = new ArrayList<>(hint.content().length);
            for (String name : hint.content()) {
                if (name.isEmpty()) {
                    continue;
                }
                Field source = byName.get(name);
                if (source == null) {
                    return;
                }
                extra.add(ConfigObjectMapper.getField(module, source));
            }
            fanOut(target, hint.key(), extra.toArray());
        }

        private void fanOut(Object target, String key, Object[] extra) {
            if (target instanceof TagKey<?> tag) {
                String tagKey = key.isEmpty() ? tag.location().getPath() : key;
                for (Object value : RegistryUtil.getTagValues(access, tag)) {
                    single(value, tagKey, extra);
                }
            } else if (target instanceof Iterable<?> many) {
                if (key.isEmpty()) {
                    return;
                }
                for (Object value : many) {
                    single(value, key, extra);
                }
            } else {
                single(target, key, extra);
            }
        }

        private void single(Object value, String key, Object[] extra) {
            if (!(value instanceof ItemLike itemLike)) {
                return;
            }
            if (key.isEmpty()) {
                hintItem(itemLike, extra);
            } else {
                hintItem(itemLike, key, extra);
            }
        }
    }
}
