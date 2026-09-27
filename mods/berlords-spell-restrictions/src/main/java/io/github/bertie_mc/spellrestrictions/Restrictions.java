package io.github.bertie_mc.spellrestrictions;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Restrictions {
    public static final ResourceLocation ELDRITCH = ResourceLocation.parse("irons_spellbooks:eldritch");
    public static final ResourceLocation OCCULT = ResourceLocation.parse("discerning_the_eldritch:ritual");
    public static final ResourceLocation ABYSSAL = ResourceLocation.parse("cataclysm_spellbooks:abyssal");

    public static boolean isSpecial(AbstractSpell spell) {
        var school = spell.getSchoolType().getId();
        return school.equals(ELDRITCH) || school.equals(OCCULT) || school.equals(ABYSSAL);
    }

    public static UnlockState state(Player player) {
        return player.getData(SpellRestrictions.PROGRESS).state;
    }

    public static boolean knows(Player player, AbstractSpell spell) {
        return state(player).knows(spell.getSpellId()) || (spell.requiresLearning() && spell.isLearned(player));
    }

    public static boolean canCraft(Player player, AbstractSpell spell, int level) {
        return (!RestrictionConfig.REQUIRE_DISCOVERY.get() || knows(player, spell))
                && (!RestrictionConfig.REQUIRE_RARITY.get()
                        || spell.getRarity(level).getValue() <= state(player).tier())
                && (!spell.requiresLearning() || spell.isLearned(player));
    }

    public static boolean canTakeScroll(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof Scroll)) return true;
        var spell = ISpellContainer.get(stack).getSpellAtIndex(0);
        return spell.getSpell() != SpellRegistry.none() && canCraft(player, spell.getSpell(), spell.getLevel());
    }

    public static void observe(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server) || stack.isEmpty() || !(stack.getItem() instanceof Scroll)) return;
        var spell = ISpellContainer.get(stack).getSpellAtIndex(0).getSpell();
        if (spell == SpellRegistry.none()) return;
        discover(server, spell, true);
    }

    public static boolean discover(ServerPlayer player, AbstractSpell spell, boolean announce) {
        boolean fresh = state(player).discover(spell.getSpellId());
        if (spell.requiresLearning() && !spell.isLearned(player)) {
            MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spell);
        }
        if (fresh) {
            if (announce) announce(player, spell);
            ProgressPacket.send(player);
        }
        return fresh;
    }

    public static void announce(ServerPlayer player, AbstractSpell spell) {
        var color = spell.getSchoolType().getDisplayName().getStyle().getColor();
        var name = Component.translatable(spell.getComponentId()).withStyle(Style.EMPTY.withColor(color));
        player.sendSystemMessage(Component.translatable("message.berlordsspellrestrictions.learned", name));
    }

    public static boolean research(ServerPlayer player, ItemStack manuscript, ResourceLocation school) {
        var choices = new ArrayList<AbstractSpell>();
        for (var spell : SpellRegistry.getEnabledSpells()) {
            if (spell.getSchoolType().getId().equals(school) && !knows(player, spell)) choices.add(spell);
        }
        return learnRandom(player, manuscript, choices, "message.berlordsspellrestrictions.no_unknown_spells");
    }

    /**
     * Teaches one unknown spell from an item's own list. Only spells that need learning count: a
     * list may name spells anyone can already cast, and teaching one of those would waste the item.
     */
    public static boolean learnOneOf(
            ServerPlayer player, ItemStack item, List<? extends Supplier<? extends AbstractSpell>> spells) {
        var choices = new ArrayList<AbstractSpell>();
        for (var entry : spells) {
            var spell = entry.get();
            if (spell.isEnabled() && spell.requiresLearning() && !knows(player, spell)) choices.add(spell);
        }
        return learnRandom(player, item, choices, "message.berlordsspellrestrictions.nothing_to_learn");
    }

    private static boolean learnRandom(ServerPlayer player, ItemStack item, List<AbstractSpell> choices, String none) {
        if (choices.isEmpty()) {
            player.sendSystemMessage(Component.translatable(none));
            return false;
        }
        discover(player, choices.get(player.getRandom().nextInt(choices.size())), true);
        if (!player.getAbilities().instabuild) item.shrink(1);
        return true;
    }

    public static void initialize(ServerPlayer player) {
        // One inventory pass on login/respawn also migrates existing scrolls.
        for (var stack : player.getInventory().items) observe(player, stack);
        for (var stack : player.getInventory().offhand) observe(player, stack);
        for (var spell : SpellRegistry.getEnabledSpells()) {
            if (!spell.requiresLearning()) continue;
            if (spell.isLearned(player)) state(player).discover(spell.getSpellId());
            else if (state(player).knows(spell.getSpellId())) {
                MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spell);
            }
        }
        ProgressPacket.send(player);
    }

    private Restrictions() {}
}
