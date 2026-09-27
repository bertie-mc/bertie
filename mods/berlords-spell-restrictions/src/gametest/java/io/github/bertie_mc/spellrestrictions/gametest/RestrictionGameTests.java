package io.github.bertie_mc.spellrestrictions.gametest;

import com.mojang.authlib.GameProfile;
import io.github.bertie_mc.spellrestrictions.ProgressData;
import io.github.bertie_mc.spellrestrictions.Restrictions;
import io.github.bertie_mc.spellrestrictions.SpellRestrictions;
import io.github.bertie_mc.spellrestrictions.UnlockState;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SpellRestrictions.ID)
@PrefixGameTestTemplate(false)
public final class RestrictionGameTests {
    private static boolean spellConfigReady;

    private static ServerPlayer player(GameTestHelper helper) {
        if (!spellConfigReady) {
            // GameTest fake players skip the normal player-login datapack synchronization.
            io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(
                    new net.neoforged.neoforge.event.OnDatapackSyncEvent(
                            helper.getLevel().getServer().getPlayerList(), null));
            spellConfigReady = true;
        }
        var id = UUID.randomUUID();
        var player = FakePlayerFactory.get(
                helper.getLevel(), new GameProfile(id, "r" + id.toString().substring(0, 12)));
        player.setGameMode(GameType.SURVIVAL);
        player.setData(SpellRestrictions.PROGRESS, new ProgressData(new UnlockState(Set.of(), 0)));
        return player;
    }

    private static ItemStack scroll(AbstractSpell spell, int level) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, level, stack);
        return stack;
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static ScrollForgeMenu forge(GameTestHelper helper, ServerPlayer player, int x) {
        var pos = helper.absolutePos(new BlockPos(x, 1, 1));
        helper.getLevel()
                .setBlockAndUpdate(pos, BlockRegistry.SCROLL_FORGE_BLOCK.get().defaultBlockState());
        var tile = (ScrollForgeTile) helper.getLevel().getBlockEntity(pos);
        var menu = (ScrollForgeMenu) tile.createMenu(1, player.getInventory(), player);
        menu.getBlankScrollSlot().set(new ItemStack(Items.PAPER, 2));
        menu.getFocusSlot().set(new ItemStack(Items.BLAZE_ROD, 2));
        menu.getInkSlot().set(item("irons_spellbooks:common_ink"));
        return menu;
    }

    @GameTest(template = "empty")
    public static void discoveryIsEventDrivenAndPermanent(GameTestHelper h) {
        var p = player(h);
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        h.assertTrue(!Restrictions.knows(p, spell), "Starts unknown");
        p.getInventory().setItem(12, scroll(spell, 1));
        h.assertTrue(Restrictions.knows(p, spell), "Inventory set discovers immediately");
        p.getInventory().clearContent();
        h.assertTrue(Restrictions.knows(p, spell), "Removing scroll preserves knowledge");
        var other = SpellRegistry.ICICLE_SPELL.get();
        p.getInventory().add(scroll(other, 1));
        h.assertTrue(Restrictions.knows(p, other), "Inventory insertion discovers immediately");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void forgeRequiresBothGates(GameTestHelper h) {
        var p = player(h);
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        var menu = forge(h, p, 1);
        menu.setRecipeSpell(spell);
        h.assertTrue(menu.getResultSlot().getItem().isEmpty(), "Unknown recipe produces no output");
        p.getInventory().setItem(0, scroll(spell, 1));
        menu.setRecipeSpell(spell);
        h.assertTrue(!menu.getResultSlot().getItem().isEmpty(), "Discovery unlocks common crafting");
        menu.getInkSlot().set(item("irons_spellbooks:legendary_ink"));
        menu.setRecipeSpell(spell);
        h.assertTrue(menu.getResultSlot().getItem().isEmpty(), "Legendary output stays locked");
        p.setItemInHand(
                InteractionHand.MAIN_HAND, SpellRestrictions.LEGENDARY_ORB.get().getDefaultInstance());
        SpellRestrictions.LEGENDARY_ORB.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        menu.setRecipeSpell(spell);
        h.assertTrue(!menu.getResultSlot().getItem().isEmpty(), "Orb unlocks legendary output");
        h.assertTrue(menu.getResultSlot().mayPickup(p), "Unlocked owner can take result");
        var other = player(h);
        h.assertTrue(!menu.getResultSlot().mayPickup(other), "Other player cannot take unknown result");
        h.assertTrue(menu.quickMoveStack(other, 39).isEmpty(), "Shift click enforces taker's own unlocks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilChecksResultRarity(GameTestHelper h) {
        var p = player(h);
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        int crossing = spell.getMinLevelForRarity(SpellRarity.UNCOMMON) - 1;
        h.assertTrue(crossing >= 1, "Fixture must cross a rarity boundary");
        p.getInventory().setItem(0, scroll(spell, crossing));
        var menu = new ArcaneAnvilMenu(2, p.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(scroll(spell, crossing));
        menu.getSlot(1).set(item("irons_spellbooks:uncommon_ink"));
        menu.createResult();
        h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Upgrading into locked tier is blocked");
        Restrictions.state(p).unlockTier(1);
        menu.createResult();
        h.assertTrue(!menu.getSlot(2).getItem().isEmpty(), "Same upgrade works after orb tier");
        h.assertTrue(
                ISpellContainer.get(menu.getSlot(2).getItem())
                                .getSpellAtIndex(0)
                                .getRarity()
                        == SpellRarity.UNCOMMON,
                "Actual output tier checked");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rarityDoesNotRestrictFoundSpellCasting(GameTestHelper h) {
        var p = player(h);
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        int level = spell.getMinLevelForRarity(SpellRarity.LEGENDARY);
        p.getInventory().setItem(0, scroll(spell, level));
        h.assertTrue(!Restrictions.canCraft(p, spell, level), "Cannot manufacture legendary at common tier");
        var result = spell.canBeCastedBy(level, CastSource.SCROLL, MagicData.getPlayerMagicData(p), p);
        h.assertTrue(result.isSuccess(), "Found legendary scroll remains castable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void manuscriptsAreRandomWithoutDuplicates(GameTestHelper h) {
        for (var school : new ResourceLocation[] {Restrictions.ELDRITCH, Restrictions.OCCULT, Restrictions.ABYSSAL}) {
            var p = player(h);
            var pool = SpellRegistry.getEnabledSpells().stream()
                    .filter(s -> s.getSchoolType().getId().equals(school))
                    .toList();
            h.assertTrue(!pool.isEmpty(), "Installed school has spells: " + school);
            var manuscript = school.equals(Restrictions.ELDRITCH)
                    ? new ItemStack(ItemRegistry.ELDRITCH_PAGE.get(), pool.size() + 1)
                    : new ItemStack(
                            school.equals(Restrictions.OCCULT)
                                    ? SpellRestrictions.OCCULT_MANUSCRIPT.get()
                                    : SpellRestrictions.ABYSSAL_MANUSCRIPT.get(),
                            pool.size() + 1);
            p.setItemInHand(InteractionHand.MAIN_HAND, manuscript);
            var learned = new HashSet<String>();
            for (int i = 0; i < pool.size(); i++) {
                int before = manuscript.getCount();
                manuscript.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
                h.assertTrue(manuscript.getCount() == before - 1, "Research consumes exactly one item");
                h.assertTrue(Restrictions.state(p).spells().size() == i + 1, "Research always adds one unknown spell");
                learned.addAll(Restrictions.state(p).spells());
            }
            h.assertTrue(learned.size() == pool.size(), "All spells can be learned once");
            int before = manuscript.getCount();
            manuscript.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(manuscript.getCount() == before, "Complete school does not consume manuscript");
            h.assertTrue(Restrictions.state(p).tier() == 0, "Research does not change crafting rarity");
            var legendary = pool.stream().filter(s -> s.getMinRarity() > 0).findFirst();
            if (legendary.isPresent())
                h.assertTrue(
                        !Restrictions.canCraft(
                                p, legendary.get(), legendary.get().getMinLevel()),
                        "Special school still observes rarity gate");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void orbsSkipTiersAndDoNotConsumeRedundantly(GameTestHelper h) {
        var p = player(h);
        var stack = new ItemStack(SpellRestrictions.EPIC_ORB.get(), 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(stack.getCount() == 1 && Restrictions.state(p).tier() == 3, "Epic orb unlocks through epic");
        stack.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(stack.getCount() == 1, "Duplicate orb is not consumed");
        p.setItemInHand(
                InteractionHand.MAIN_HAND, SpellRestrictions.COMMON_ORB.get().getDefaultInstance());
        SpellRestrictions.COMMON_ORB.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(
                !p.getMainHandItem().isEmpty() && Restrictions.state(p).tier() == 3,
                "Lower orb does not downgrade or consume");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void specialKnowledgeRestoresAndRequiresProperInk(GameTestHelper h) {
        var p = player(h);
        var spell = SpellRegistry.SONIC_BOOM_SPELL.get();
        Restrictions.state(p).discover(spell.getSpellId());
        Restrictions.state(p).unlockTier(4);
        h.assertTrue(!spell.isLearned(p), "Native research begins unknown");
        Restrictions.initialize(p);
        h.assertTrue(spell.isLearned(p), "Permanent discovery restores native research");
        var menu = forge(h, p, 1);
        menu.getFocusSlot().set(new ItemStack(Items.ECHO_SHARD));
        menu.setRecipeSpell(spell);
        h.assertTrue(
                menu.getResultSlot().getItem().isEmpty(),
                "Common ink cannot craft Legendary-only spell even with unlocked tier");
        menu.getInkSlot().set(item("irons_spellbooks:legendary_ink"));
        menu.setRecipeSpell(spell);
        h.assertTrue(!menu.getResultSlot().getItem().isEmpty(), "Proper ink and both gates permit the special spell");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesMatchRequestedIngredients(GameTestHelper h) {
        player(h);
        String[] tiers = {"common", "uncommon", "rare", "epic", "legendary"};
        for (int tier = 0; tier < tiers.length; tier++) {
            String name = tiers[tier];
            var inputs = new java.util.ArrayList<ItemStack>();
            for (int i = 0; i < 9; i++) inputs.add(item("irons_spellbooks:" + name + "_ink"));
            inputs.set(
                    4,
                    item(
                            tier == 0
                                    ? "irons_spellbooks:arcane_essence"
                                    : SpellRestrictions.ID + ":" + tiers[tier - 1] + "_crafting_orb"));
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs);
            var recipe = h.getLevel()
                    .getRecipeManager()
                    .byKey(SpellRestrictions.id(name + "_crafting_orb"))
                    .orElseThrow();
            @SuppressWarnings("unchecked")
            var crafting = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>)
                    recipe.value();
            h.assertTrue(crafting.matches(input, h.getLevel()), "Recursive orb recipe matches: " + name);
            h.assertTrue(
                    crafting.assemble(input, h.getLevel().registryAccess())
                            .is(item(SpellRestrictions.ID + ":" + name + "_crafting_orb")
                                    .getItem()),
                    "Correct orb output");
        }
        for (String name : new String[] {"occult", "abyssal"}) {
            String fragment = name.equals("occult") ? "forbidden" : "depth";
            String focus =
                    name.equals("occult") ? "discerning_the_eldritch:shard_of_malice" : "cataclysm:crystallized_coral";
            var inputs = new java.util.ArrayList<ItemStack>();
            for (int i = 0; i < 9; i++) inputs.add(item(SpellRestrictions.ID + ":" + fragment + "_knowledge_fragment"));
            inputs.set(4, item(focus));
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs);
            var recipe = h.getLevel()
                    .getRecipeManager()
                    .byKey(SpellRestrictions.id(name + "_manuscript"))
                    .orElseThrow();
            @SuppressWarnings("unchecked")
            var crafting = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>)
                    recipe.value();
            h.assertTrue(crafting.matches(input, h.getLevel()), "Manuscript recipe matches: " + name);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cataclysmKnowledgeTeachesOneSpellPerUse(GameTestHelper h) {
        for (String kind : new String[] {"burning", "frozen"}) {
            var p = player(h);
            var fragment = item("cataclysm_spellbooks:" + kind + "_knowledge_fragment");
            p.setItemInHand(InteractionHand.MAIN_HAND, fragment);
            fragment.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(
                    fragment.getCount() == 1 && Restrictions.state(p).spells().isEmpty(),
                    "Fragment is material and teaches nothing: " + kind);

            var book =
                    item("cataclysm_spellbooks:" + (kind.equals("burning") ? "burning_manuscript" : "frozen_tablet"));
            book.setCount(16);
            p.setItemInHand(InteractionHand.MAIN_HAND, book);
            int learned = 0;
            while (book.getCount() > 0) {
                int before = book.getCount();
                book.getItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
                if (book.getCount() == before) break;
                learned++;
                h.assertTrue(book.getCount() == before - 1, "One use consumes exactly one item");
                h.assertTrue(Restrictions.state(p).spells().size() == learned, "One use teaches exactly one spell");
            }
            h.assertTrue(learned > 0 && book.getCount() > 0, "List exhausts before the stack: " + kind);
            for (var id : Restrictions.state(p).spells()) {
                var spell = SpellRegistry.getSpell(id);
                h.assertTrue(spell.requiresLearning() && spell.isLearned(p), "Taught spell is learned natively: " + id);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void frozenTabletTakesCursium(GameTestHelper h) {
        var inputs = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) inputs.add(item("cataclysm_spellbooks:frozen_knowledge_fragment"));
        inputs.set(4, item("cataclysm:cursium_ingot"));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, inputs);
        var recipe = h.getLevel()
                .getRecipeManager()
                .byKey(ResourceLocation.parse("cataclysm_spellbooks:frozen_tablet"))
                .orElseThrow();
        @SuppressWarnings("unchecked")
        var crafting = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>)
                recipe.value();
        h.assertTrue(crafting.matches(input, h.getLevel()), "Cursium Ingot sits in the middle of the tablet");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fragmentsDropFromTheirSources(GameTestHelper h) {
        var server = h.getLevel().getServer();
        long ours = server.reloadableRegistries().getKeys(Registries.LOOT_TABLE).stream()
                .filter(id -> id.getNamespace().equals(SpellRestrictions.ID)
                        && id.getPath().startsWith("fragments/"))
                .count();
        h.assertTrue(ours == 26, "Every fragment source table loads: " + ours);
        String[][] cases = {
            {"minecraft:chests/nether_bridge", SpellRestrictions.ID + ":forbidden_knowledge_fragment"},
            {"cataclysm:chests/acropolis_treasure", SpellRestrictions.ID + ":depth_knowledge_fragment"},
            {"irons_spellbooks:chests/citadel/citadel_vault", "cataclysm_spellbooks:burning_knowledge_fragment"},
            {"cataclysm:chests/frosted_prison_treasure", "cataclysm_spellbooks:frozen_knowledge_fragment"},
        };
        var origin = Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO));
        for (var c : cases) {
            var table = server.reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(c[0])));
            var fragment = item(c[1]).getItem();
            boolean dropped = false;
            for (int i = 0; i < 64 && !dropped; i++) {
                var params = new LootParams.Builder(h.getLevel())
                        .withParameter(LootContextParams.ORIGIN, origin)
                        .create(LootContextParamSets.CHEST);
                dropped = table.getRandomItems(params).stream().anyMatch(s -> s.is(fragment));
            }
            h.assertTrue(dropped, c[0] + " rolls " + c[1]);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void manuscriptNamesTakeTheirSchoolColour(GameTestHelper h) {
        var cases = java.util.Map.of(
                SpellRestrictions.OCCULT_MANUSCRIPT.get(), Restrictions.OCCULT,
                SpellRestrictions.ABYSSAL_MANUSCRIPT.get(), Restrictions.ABYSSAL);
        cases.forEach((manuscript, school) -> {
            var color = new ItemStack(manuscript).getHoverName().getStyle().getColor();
            var expected =
                    SchoolRegistry.getSchool(school).getDisplayName().getStyle().getColor();
            h.assertTrue(color != null && color.equals(expected), "Manuscript name in school colour: " + school);
        });
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void persistenceAndDeathCopy(GameTestHelper h) {
        var p = player(h);
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        p.getInventory().setItem(0, scroll(spell, 1));
        Restrictions.state(p).unlockTier(4);
        var tag = ProgressData.CODEC
                .encodeStart(NbtOps.INSTANCE, p.getData(SpellRestrictions.PROGRESS))
                .getOrThrow();
        var restored = ProgressData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        h.assertTrue(restored.state.knows(spell.getSpellId()) && restored.state.tier() == 4, "Save data round trip");
        var clone = player(h);
        clone.restoreFrom(p, false);
        h.assertTrue(
                Restrictions.state(clone).knows(spell.getSpellId())
                        && Restrictions.state(clone).tier() == 4,
                "Unlocks survive death clone");
        h.succeed();
    }
}
