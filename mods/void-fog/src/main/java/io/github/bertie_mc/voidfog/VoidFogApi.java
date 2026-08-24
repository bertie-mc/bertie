package io.github.bertie_mc.voidfog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * How other content holds the fog off one player - an armour set, a trinket, an enchanted tool.
 *
 * <p>Two ways in. {@link #SUPPRESSES} needs no code at all: put an item in the tag and wearing or
 * holding it clears the fog. {@link #addSuppressor} is for anything the tag cannot express, and
 * answers with a degree rather than a yes, so a partial effect is possible.
 *
 * <p>The tag is item data, and this mod is installed on clients only. In single player that is the
 * same game and the tag works; against a dedicated server without this mod the tag will be empty
 * and only registered suppressors run.
 */
public final class VoidFogApi {
    /** Worn or held, any item in this tag clears the fog. */
    public static final TagKey<Item> SUPPRESSES =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath(VoidFog.MOD_ID, "suppresses_void_fog"));

    private static final List<ToDoubleFunction<Player>> SUPPRESSORS = new CopyOnWriteArrayList<>();

    private VoidFogApi() {}

    /**
     * Registers a source of suppression. It is asked for a number from 0 (no effect) to 1 (no fog)
     * once per frame while the player is deep enough to see fog at all, so keep it cheap. The
     * strongest answer wins; sources do not stack.
     */
    public static void addSuppressor(ToDoubleFunction<Player> suppressor) {
        SUPPRESSORS.add(suppressor);
    }

    /** The strongest suppression claimed for this player, 0 to 1. */
    public static float suppression(Player player) {
        for (ItemStack stack : player.getAllSlots()) {
            if (stack.is(SUPPRESSES)) {
                return 1.0F;
            }
        }
        float strongest = 0.0F;
        for (ToDoubleFunction<Player> suppressor : SUPPRESSORS) {
            strongest = Math.max(strongest, (float) suppressor.applyAsDouble(player));
            if (strongest >= 1.0F) {
                return 1.0F;
            }
        }
        return strongest;
    }
}
