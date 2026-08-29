package io.github.bertie_mc.bertieprogression;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;

/**
 * Writes the resolved trinket slot list to {@code logs/bertie-slots.txt} when a player joins.
 *
 * <p>The pack runs two slot registries bridged by a compat layer that renames ids, merges files
 * across namespaces and drops some of them, so what a slot file says and what the player ends up
 * with are different questions. Guessing at the second one from the first has been wrong often
 * enough that this reads the answer out of the running game instead: every slot the player actually
 * has, in the order the screen puts them, with the size it settled on, plus whether each head
 * trinket made it into the tag its slot validates against.
 */
public final class SlotAudit {

    private static final String HEAD_SLOT = "hat";
    private static final String[] HEAD_ITEMS = {
        "armageddon_mod:fisher_hat", "armageddon_mod:vagabonds_hood", "artifacts:anglers_hat",
        "artifacts:cowboy_hat", "artifacts:night_vision_goggles", "artifacts:novelty_drinking_hat",
        "artifacts:plastic_drinking_hat", "artifacts:snorkel", "artifacts:superstitious_hat",
        "artifacts:villager_hat", "cataclysm:aptrgangr_head", "cataclysm:draugr_head",
        "cataclysm:kobolediator_skull", "create:goggles", "l2hostility:detector_glasses",
        "l2hostility:oddeyes_glasses", "nameless_trinkets:cracked_crown",
        "nameless_trinkets:gods_crown", "pastel:ashen_circlet", "pastel:circlet_of_arrogance",
        "pastel:puff_circlet", "pastel:weeping_circlet", "pastel:whispy_circlet",
        "terra_curio:arctic_diving_gear", "terra_curio:jellyfish_diving_gear",
    };

    private SlotAudit() {
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        List<String> out = new ArrayList<>();
        Map<String, ISlotType> slots = CuriosApi.getEntitySlots(player);
        List<ISlotType> ordered = new ArrayList<>(slots.values());
        // The screen runs from the highest order to the lowest; mirror that here rather than trust
        // whatever iteration order the map arrived in.
        ordered.sort(Comparator.comparingInt(ISlotType::getOrder).reversed()
                .thenComparing(Comparator.comparing(ISlotType::getIdentifier).reversed()));
        out.add("slots the player has, in menu order (" + ordered.size() + ")");
        out.add(String.format("%-4s %-36s %8s %6s", "#", "slot", "order", "size"));
        int i = 1;
        for (ISlotType s : ordered) {
            out.add(String.format("%-4d %-36s %8d %6d", i++, s.getIdentifier(), s.getOrder(), s.getSize()));
        }
        out.add("");
        out.add("head trinkets vs the tags the " + HEAD_SLOT + " slot validates against");
        TagKey<Item> curiosTag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("curios", HEAD_SLOT));
        TagKey<Item> accTag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("accessories", HEAD_SLOT));
        out.add(String.format("%-44s %-14s %s", "item", "#curios:" + HEAD_SLOT, "#accessories:" + HEAD_SLOT));
        for (String id : HEAD_ITEMS) {
            ResourceLocation key = ResourceLocation.parse(id);
            Item item = BuiltInRegistries.ITEM.getOptional(key).orElse(null);
            if (item == null) {
                out.add(String.format("%-44s %-14s %s", id, "NO SUCH ITEM", ""));
                continue;
            }
            var holder = item.builtInRegistryHolder();
            out.add(String.format("%-44s %-14s %s", id,
                    holder.is(curiosTag) ? "yes" : "NO", holder.is(accTag) ? "yes" : "NO"));
        }
        Path path = FMLPaths.GAMEDIR.get().resolve("logs").resolve("bertie-slots.txt");
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, String.join(System.lineSeparator(), out).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // A diagnostic that cannot write is not worth interrupting a login for.
        }
    }
}
