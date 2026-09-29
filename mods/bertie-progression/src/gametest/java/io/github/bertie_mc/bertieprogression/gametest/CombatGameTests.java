package io.github.bertie_mc.bertieprogression.gametest;

import auviotre.enigmatic.legacy.registries.EnigmaticAttachments;
import com.mojang.authlib.GameProfile;
import earth.terrarium.pastel.attachments.data.azure_dike.AzureDikeData;
import io.github.bertie_mc.bertieprogression.combat.ArmorDefenses;
import io.github.bertie_mc.bertieprogression.combat.CombatRegistry;
import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import io.github.bertie_mc.bertieprogression.combat.MagicOrigin;
import io.github.bertie_mc.bertieprogression.combat.MagicOrigins;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.confluence.terra_curio.common.init.TCEffects;
import org.confluence.terra_curio.common.item.curio.combat.PaladinsShield;

@GameTestHolder("bertieprogression")
@PrefixGameTestTemplate(false)
public final class CombatGameTests {
    private CombatGameTests() {}

    @GameTest(template = "empty", batch = "combat")
    public static void armorAndProtectionUseChosenCurves(GameTestHelper helper) {
        Zombie target = target(helper);
        equipProtection(helper, target);
        target.hurt(helper.getLevel().damageSources().generic(), 20);
        near(helper, 200 - target.getHealth(), 20 * 8.0 / 28.0 * 0.1, "armor then Protection X on four pieces");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void energyIgnoresArmorButUsesProtection(GameTestHelper helper) {
        Zombie target = target(helper);
        equipProtection(helper, target);
        DamageSource source = helper.getLevel().damageSources().onFire();
        helper.assertTrue(DamageFamilies.of(source) == DamageFamily.ENERGY, "burning must be Energy");
        target.hurt(source, 10);
        near(helper, 200 - target.getHealth(), 1, "Energy protection");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void pureBypassesResistanceButSpendsAbsorption(GameTestHelper helper) {
        Zombie target = target(helper);
        equipProtection(helper, target);
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4));
        target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);
        target.setAbsorptionAmount(5);
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 10);
        near(helper, target.getAbsorptionAmount(), 0, "Pure absorption");
        near(helper, 200 - target.getHealth(), 5, "Pure health remainder");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void toughnessCountersEffectArmorLoss(GameTestHelper helper) {
        Zombie target = target(helper);
        target.getAttribute(Attributes.ARMOR).setBaseValue(25);
        target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(10);
        ResourceLocation penalty = ResourceLocation.fromNamespaceAndPath("bertieprogression", "test_armor_penalty");
        ArmorDefenses.registerPenalty(penalty);
        target.getAttribute(Attributes.ARMOR)
                .addTransientModifier(
                        new AttributeModifier(penalty, -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        helper.assertTrue(
                target.getArmorValue() == 21,
                "10 toughness must counter 20% of the armor penalty; armor=" + target.getArmorValue());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void toughnessContinuesPastSixtyPercentAgainstShredAndPierce(GameTestHelper helper) {
        Zombie target = target(helper);
        Zombie attacker = target(helper);
        CombatRegistry.attribute(attacker, "apothic_attributes:armor_shred").setBaseValue(1);
        CombatRegistry.attribute(attacker, "apothic_attributes:armor_pierce").setBaseValue(10);
        DamageSource source = helper.getLevel().damageSources().mobAttack(attacker);
        // Pass the effective toughness directly: the compact test pack omits AttributeFix,
        // which raises the vanilla attribute limit in the full pack.
        near(
                helper,
                dev.shadowsoffire.apothic_attributes.api.ALCombatRules.getDamageAfterArmor(target, source, 20, 25, 30),
                160.0 / 19,
                "30 toughness still counters 60% of penetration");
        near(
                helper,
                dev.shadowsoffire.apothic_attributes.api.ALCombatRules.getDamageAfterArmor(target, source, 20, 25, 40),
                160.0 / 26,
                "40 toughness counters 80% of penetration");
        near(
                helper,
                dev.shadowsoffire.apothic_attributes.api.ALCombatRules.getDamageAfterArmor(target, source, 20, 25, 50),
                160.0 / 33,
                "50 toughness prevents all penetration");
        near(
                helper,
                dev.shadowsoffire.apothic_attributes.api.ALCombatRules.getDamageAfterArmor(target, source, 20, 25, 100),
                160.0 / 33,
                "excess toughness cannot turn penetration into bonus armor");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void magicDoesNotReceiveOrdinaryFireProtection(GameTestHelper helper) {
        Zombie target = target(helper);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        boots.enchant(
                helper.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.FIRE_PROTECTION),
                10);
        target.setItemSlot(EquipmentSlot.FEET, boots);
        target.tick();
        target.setHealth(200);
        var type = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ResourceKey.create(
                        Registries.DAMAGE_TYPE,
                        ResourceLocation.fromNamespaceAndPath("bertieprogression", "test_magic")));
        DamageSource source = new DamageSource(type);
        target.hurt(source, 10);
        near(
                helper,
                200 - target.getHealth(),
                10 * 8.0 / 9.0,
                "Magic uses its one armor point but not Fire Protection");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void bleedingAndHealingBonusOffsetEachOther(GameTestHelper helper) {
        Zombie target = target(helper);
        target.setHealth(100);
        CombatRegistry.attribute(target, "apothic_attributes:healing_received").setBaseValue(1.5);
        target.addEffect(
                new MobEffectInstance(CombatRegistry.effect("simplymore:bleed").orElseThrow(), 200));
        target.heal(10);
        near(helper, target.getHealth(), 110, "+50 healing and -50 Bleeding");
        helper.assertTrue(
                CombatRegistry.effectLevel(target, "simplymore:bleed") == 1, "Bleeding must remain installed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void bleedingCannotBypassHealingCancellation(GameTestHelper helper) {
        Zombie target = target(helper);
        target.setHealth(100);
        target.addEffect(
                new MobEffectInstance(CombatRegistry.effect("simplymore:bleed").orElseThrow(), 200));
        HealBlocker blocker = new HealBlocker(target.getUUID());
        NeoForge.EVENT_BUS.register(blocker);
        try {
            target.heal(10);
            near(helper, target.getHealth(), 100, "healing cancellation with Bleeding");
        } finally {
            NeoForge.EVENT_BUS.unregister(blocker);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void energyWeakeningSubtractsFromCombinedResistance(GameTestHelper helper) {
        Zombie target = target(helper);
        var resistance = CombatRegistry.attribute(target, "lodestone:magic_resistance");
        helper.assertTrue(resistance != null, "Lodestone resistance must be installed");
        resistance.addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("bertieprogression", "energy_a"),
                0.5,
                AttributeModifier.Operation.ADD_VALUE));
        resistance.addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("bertieprogression", "energy_b"),
                0.5,
                AttributeModifier.Operation.ADD_VALUE));
        resistance.addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("bertieprogression", "energy_weakness"),
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        target.hurt(helper.getLevel().damageSources().onFire(), 20);
        near(helper, 200 - target.getHealth(), 15, "75% combined resistance minus 50 percentage points");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void malignantStrongholdSuppliesEnergyImmunity(GameTestHelper helper) {
        Zombie target = target(helper);
        String[] names = {"boots", "leggings", "chestplate", "helmet"};
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < names.length; i++) {
            var item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("malum", "malignant_stronghold_" + names[i]));
            helper.assertTrue(item != Items.AIR, "Stronghold armor must be installed");
            target.setItemSlot(slots[i], new ItemStack(item));
        }
        target.tick();
        target.setHealth(200);
        target.hurt(helper.getLevel().damageSources().onFire(), 40);
        near(helper, target.getHealth(), 200, "four additive 25% Stronghold pieces");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void heartPoolsHaveTheAgreedOrderAgainstPure(GameTestHelper helper) {
        Zombie target = target(helper);
        target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);
        target.setAbsorptionAmount(5);
        target.getData(EnigmaticAttachments.ENIGMATIC_DATA).setEtherealShield(3);
        target.setData(AzureDikeData.ATTACHMENT, new AzureDikeData(7, 7, 20, 200, 0));
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 6);
        near(helper, target.getData(EnigmaticAttachments.ENIGMATIC_DATA).getEtherealShield(), 0, "Lantern is first");
        near(helper, target.getAbsorptionAmount(), 2, "absorption is second");
        near(helper, target.getData(AzureDikeData.ATTACHMENT).getCurrentProtection(), 7, "Azure Dike is last");
        near(helper, target.getHealth(), 200, "no health damage while early pools suffice");
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 13);
        near(helper, target.getAbsorptionAmount(), 0, "remaining absorption spent");
        near(helper, target.getData(AzureDikeData.ATTACHMENT).getCurrentProtection(), 0, "remaining Dike spent");
        near(helper, target.getHealth(), 196, "Pure remainder reaches health");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void azureDikeSpendsOnlyPostMitigationDamage(GameTestHelper helper) {
        Zombie target = target(helper);
        equipProtection(helper, target);
        target.setData(AzureDikeData.ATTACHMENT, new AzureDikeData(10, 10, 20, 200, 0));
        target.hurt(helper.getLevel().damageSources().generic(), 20);
        near(helper, target.getHealth(), 200, "Dike prevents the remaining hit");
        near(
                helper,
                target.getData(AzureDikeData.ATTACHMENT).getCurrentProtection(),
                10 - 20 * 8.0 / 28.0 * 0.1,
                "Dike must not spend raw damage or pay twice");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void pureResistanceRemainsAnExplicitException(GameTestHelper helper) {
        Zombie target = target(helper);
        target.addEffect(new MobEffectInstance(
                CombatRegistry.effect("enigmaticlegacyplus:pure_resistance").orElseThrow(), 200, 4));
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 10);
        near(helper, target.getHealth(), 200, "Pure Resistance V prevents Pure");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void lifeStealMergesRatesAndExcludesOverkill(GameTestHelper helper) {
        Zombie attacker = target(helper);
        Zombie victim = target(helper);
        attacker.setHealth(50);
        victim.setHealth(3);
        CombatRegistry.attribute(attacker, "apothic_attributes:life_steal").setBaseValue(0.1);
        CombatRegistry.attribute(attacker, "enigmaticlegacyplus:lifesteal").setBaseValue(0.2);
        CombatRegistry.attribute(attacker, "apothic_attributes:overheal").setBaseValue(0.25);
        victim.hurt(helper.getLevel().damageSources().mobAttack(attacker), 100);
        near(helper, attacker.getHealth(), 50.9, "30% lifesteal on three lost HP");
        near(helper, attacker.getAbsorptionAmount(), 0.75, "native Overheal remains separate");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void passiveDodgeOnlyStopsPhysicalDamage(GameTestHelper helper) {
        Zombie target = target(helper);
        CombatRegistry.attribute(target, "apothic_attributes:dodge_chance").setBaseValue(1);
        target.hurt(helper.getLevel().damageSources().generic(), 10);
        near(helper, target.getHealth(), 200, "guaranteed Physical dodge");
        target.hurt(helper.getLevel().damageSources().onFire(), 10);
        near(helper, target.getHealth(), 190, "Energy cannot trigger passive Dodge");
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 10);
        near(helper, target.getHealth(), 180, "Pure cannot trigger passive Dodge");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void l2FlatSubtractionFollowsOtherReductions(GameTestHelper helper) {
        Zombie target = target(helper);
        equipProtection(helper, target);
        CombatRegistry.attribute(target, "l2damagetracker:damage_absorption").setBaseValue(1);
        target.hurt(helper.getLevel().damageSources().onFire(), 20);
        near(helper, target.getHealth(), 199, "20 -> 2 after Protection -> 1 after flat subtraction");
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 10);
        near(helper, target.getHealth(), 189, "Pure skips ordinary flat subtraction");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void borrowedSpellProjectilesRemainMagic(GameTestHelper helper) {
        Zombie caster = target(helper);
        Zombie victim = target(helper);
        CombatRegistry.attribute(victim, "irons_spellbooks:spell_resist").setBaseValue(1.5);
        CombatRegistry.attribute(victim, "irons_spellbooks:fire_magic_resist").setBaseValue(1);
        var arrow = EntityType.ARROW.create(helper.getLevel());
        arrow.setOwner(caster);
        arrow.setPos(caster.position());
        MagicOrigins.begin(caster, SchoolRegistry.FIRE.get().getId());
        try {
            helper.getLevel().addFreshEntity(arrow);
        } finally {
            MagicOrigins.end();
        }
        helper.assertTrue(
                ((MagicOrigin) arrow).bertie$isSpellEntity(), "spell origin must follow the spawned projectile");
        var type = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.FIREBALL);
        var source = new DamageSource(type, arrow, caster);
        helper.assertTrue(DamageFamilies.of(source) == DamageFamily.MAGIC, "borrowed fireball must retain Magic");
        victim.hurt(source, 20);
        near(helper, victim.getHealth(), 190, "borrowed damage uses Spell Resistance once");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void nativeSpellResistanceAndLifeStealAreNotDuplicated(GameTestHelper helper) {
        Zombie caster = target(helper);
        Zombie victim = target(helper);
        caster.setHealth(50);
        CombatRegistry.attribute(caster, "apothic_attributes:life_steal").setBaseValue(1);
        CombatRegistry.attribute(caster, "enigmaticlegacyplus:lifesteal").setBaseValue(1);
        CombatRegistry.attribute(victim, "irons_spellbooks:spell_resist").setBaseValue(1.5);
        CombatRegistry.attribute(victim, "irons_spellbooks:blood_magic_resist").setBaseValue(1);
        DamageSources.applyDamage(
                victim, 20, SpellRegistry.BLOOD_SLASH_SPELL.get().getDamageSource(caster));
        near(helper, victim.getHealth(), 190, "native spell resistance must not run twice");
        near(helper, caster.getHealth(), 51.5, "only the spell's native 15% lifesteal applies to Magic");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void heartstopStoresPostDefenseDebtAndPaysPure(GameTestHelper helper) {
        FakePlayer target = player(helper, "Heartstop");
        target.tickCount = 100;
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        target.addEffect(new MobEffectInstance(MobEffectRegistry.HEARTSTOP, 200));
        target.hurt(helper.getLevel().damageSources().generic(), 20);
        near(helper, target.getHealth(), 200, "Heartstop postpones the hit");
        near(
                helper,
                MagicData.getPlayerMagicData(target).getSyncedData().getHeartstopAccumulatedDamage(),
                8,
                "half of the 16 post-Resistance damage");
        target.removeEffect(MobEffectRegistry.HEARTSTOP);
        near(helper, target.getHealth(), 192, "debt is not reduced again or lost to hit cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void evasionDoesNotSpendChargesOnExcludedDamage(GameTestHelper helper) {
        FakePlayer target = player(helper, "Evasion");
        target.addEffect(new MobEffectInstance(MobEffectRegistry.EVASION, 200, 2));
        int before = MagicData.getPlayerMagicData(target).getSyncedData().getEvasionHitsRemaining();
        for (DamageSource source : new DamageSource[] {
            helper.getLevel().damageSources().onFire(),
            helper.getLevel().damageSources().lava(),
            helper.getLevel().damageSources().fellOutOfWorld()
        }) {
            target.invulnerableTime = 0;
            target.hurt(source, 2);
        }
        helper.assertTrue(
                MagicData.getPlayerMagicData(target).getSyncedData().getEvasionHitsRemaining() == before,
                "excluded hits must not consume Evasion");
        near(helper, target.getHealth(), 194, "excluded hits are not canceled by Evasion");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void detonationAndBleedingUseSeparateFamilies(GameTestHelper helper) {
        Zombie burn = target(helper);
        burn.getAttribute(Attributes.ARMOR).setBaseValue(20);
        burn.setRemainingFireTicks(140);
        var detonation = new MobEffectInstance(
                CombatRegistry.effect("apothic_attributes:detonation").orElseThrow(), 1);
        burn.addEffect(detonation);
        detonation.tick(burn, () -> {});
        near(helper, burn.getHealth(), 190, "Detonation ignores armor as Energy");
        Zombie bleed = target(helper);
        bleed.getAttribute(Attributes.ARMOR).setBaseValue(20);
        var effect =
                new MobEffectInstance(CombatRegistry.effect("simplymore:bleed").orElseThrow(), 35);
        bleed.addEffect(effect);
        effect.tick(bleed, () -> {});
        near(helper, 200 - bleed.getHealth(), 8.0 / 28, "Bleeding uses Physical armor");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void ribbonwrathWorksWithAndWithoutProtection(GameTestHelper helper) {
        Zombie target = target(helper);
        target.addEffect(new MobEffectInstance(
                CombatRegistry.effect("simplyswords:ribbonwrath").orElseThrow(), 200));
        target.hurt(helper.getLevel().damageSources().generic(), 20);
        near(helper, target.getHealth(), 183, "15% reduction without Protection");
        equipProtection(helper, target);
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().generic(), 20);
        near(helper, 200 - target.getHealth(), 20 * 8.0 / 28 * 0.1 * 0.85, "Ribbonwrath must not apply twice");
        target.invulnerableTime = 0;
        float before = target.getHealth();
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 10);
        near(helper, before - target.getHealth(), 10, "Pure ignores Ribbonwrath");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void paladinTransferRetainsEnergyAndDoesNotTransferPure(GameTestHelper helper) {
        FakePlayer victim = player(helper, "PalVictim");
        FakePlayer protector = player(helper, "PalGuard");
        protector.getAttribute(Attributes.ARMOR).setBaseValue(20);
        protector.addEffect(new MobEffectInstance(TCEffects.PALADINS_SHIELD, 600, 1));
        var board = helper.getLevel().getScoreboard();
        var team = board.addPlayerTeam("ct" + UUID.randomUUID().toString().substring(0, 8));
        board.addPlayerToTeam(victim.getScoreboardName(), team);
        board.addPlayerToTeam(protector.getScoreboardName(), team);
        helper.getLevel().addNewPlayer(victim);
        helper.getLevel().addNewPlayer(protector);
        try {
            float remainder = PaladinsShield.apply(
                    victim, helper.getLevel().damageSources().onFire(), 20);
            near(helper, remainder, 15, "Paladin transfers one quarter");
            near(helper, protector.getHealth(), 195, "transferred Energy must not become Physical");
            near(
                    helper,
                    PaladinsShield.apply(
                            victim, helper.getLevel().damageSources().fellOutOfWorld(), 20),
                    20,
                    "Pure cannot be shared by Paladin");
        } finally {
            victim.discard();
            protector.discard();
            board.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void optionalCompatibilityTargetsTransform(GameTestHelper helper) throws Exception {
        var loader = CombatGameTests.class.getClassLoader();
        int checked = 0;
        try (var reader = new java.io.InputStreamReader(
                loader.getResourceAsStream("combat-mixin-targets.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            for (var name : com.google.gson.JsonParser.parseReader(reader).getAsJsonArray()) {
                String target = name.getAsString();
                if (loader.getResource(target.replace('.', '/') + ".class") != null) {
                    Class.forName(target, false, loader);
                    checked++;
                }
            }
        }
        helper.assertTrue(checked >= 65, "optional compatibility coverage unexpectedly absent: " + checked);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void waverEnergyResistanceChangesAfterAssimilation(GameTestHelper helper) throws Exception {
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("witheringwaver:withering_waver"));
        var target = (net.minecraft.world.entity.Mob) type.create(helper.getLevel());
        helper.assertTrue(
                target != null && target instanceof io.github.bertie_mc.bertieprogression.combat.InnateEnergyResistance,
                "Waver integration must be installed");
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        target.hurt(helper.getLevel().damageSources().magic(), 100);
        near(helper, target.getHealth(), 930, "Waver base Energy resistance is 30%");
        var assimilate = target.getClass().getDeclaredMethod("setAssimilated", boolean.class);
        assimilate.setAccessible(true);
        assimilate.invoke(target, true);
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().magic(), 100);
        near(helper, target.getHealth(), 890, "Waver assimilated Energy resistance is 60%");
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 100);
        near(helper, target.getHealth(), 790, "Waver resistance does not reduce Pure");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void aegisStopsOrdinaryHitsButNotPure(GameTestHelper helper) {
        Zombie target = target(helper);
        CombatRegistry.attribute(target, "malum:malignant_aegis_capacity").setBaseValue(3);
        var data = target.getData(com.sammy.malum.registry.common.MalumAttachmentTypes.MALIGNANT_INFLUENCE);
        data.setAegis(3);
        target.hurt(helper.getLevel().damageSources().generic(), 20);
        near(helper, target.getHealth(), 200, "one Aegis charge cancels an ordinary hit");
        helper.assertTrue(data.getMalignantAegis() == 2, "one charge spent");
        target.invulnerableTime = 0;
        target.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 20);
        near(helper, target.getHealth(), 180, "Pure bypasses Aegis");
        helper.assertTrue(data.getMalignantAegis() == 2, "Pure does not consume Aegis charges");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "combat")
    public static void mageslayerConvertsOnlyRemainingEnergy(GameTestHelper helper) {
        Zombie target = target(helper);
        equipProtection(helper, target);
        target.setHealth(100);
        CombatRegistry.attribute(target, "lodestone:magic_resistance").setBaseValue(1.5);
        var handler = new MageSlayerFixture(target.getUUID());
        NeoForge.EVENT_BUS.register(handler);
        try {
            target.hurt(helper.getLevel().damageSources().magic(), 100);
            near(helper, target.getHealth(), 97.5, "100 -> 10 Protection -> 5 Energy; heal 1.25 and take 3.75");
        } finally {
            NeoForge.EVENT_BUS.unregister(handler);
        }
        helper.succeed();
    }

    public record MageSlayerFixture(UUID target) {
        @SubscribeEvent
        public void capture(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
            if (!event.getEntity().getUUID().equals(target)) return;
            var gem = dev.shadowsoffire.apotheosis.socket.gem.GemInstance.EMPTY;
            var bonus = new dev.shadowsoffire.apotheosis.socket.gem.bonus.special.MageSlayerBonus(
                    null, java.util.Map.of(gem.purity(), 0.25F));
            event.setNewDamage(bonus.onHurt(gem, event.getSource(), event.getEntity(), event.getNewDamage()));
        }
    }

    @GameTest(template = "empty", batch = "combat")
    public static void spellProjectilesSkipOrdinaryDeflection(GameTestHelper helper) {
        FakePlayer target = player(helper, "Deflect");
        target.getAttribute(auviotre.enigmatic.legacy.registries.EnigmaticAttributes.PROJECTILE_DEFLECT)
                .setBaseValue(1);
        var arrow = EntityType.ARROW.create(helper.getLevel());
        var hit = new net.minecraft.world.phys.EntityHitResult(target);
        var normal = new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(arrow, hit);
        auviotre.enigmatic.legacy.contents.attribute.ProjectileDeflectAttribute.Events.onProjectileImpact(normal);
        helper.assertTrue(normal.isCanceled(), "ordinary projectile can be deflected");
        ((MagicOrigin) arrow)
                .bertie$spellOrigin(target.getUUID(), SchoolRegistry.FIRE.get().getId());
        var spell = new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(arrow, hit);
        auviotre.enigmatic.legacy.contents.attribute.ProjectileDeflectAttribute.Events.onProjectileImpact(spell);
        helper.assertTrue(!spell.isCanceled(), "spell projectile must not be deflected by ordinary Projectile defense");
        MagicOrigins.beginEntity(arrow);
        try {
            helper.assertTrue(
                    arrow.getType().is(earth.terrarium.pastel.registries.PastelEntityTypeTags.UNDEFLECTABLE),
                    "Pastel rebound excludes borrowed spell projectiles");
        } finally {
            MagicOrigins.end();
        }
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper, String prefix) {
        FakePlayer player =
                new FakePlayer(
                        helper.getLevel(),
                        new GameProfile(
                                UUID.randomUUID(),
                                prefix + UUID.randomUUID().toString().substring(0, 4))) {
                    @Override
                    public boolean isInvulnerableTo(DamageSource source) {
                        return false;
                    }
                };
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            var grace = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            grace.setAccessible(true);
            grace.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        player.setHealth(200);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        return player;
    }

    public record HealBlocker(UUID target) {
        @SubscribeEvent
        public void block(LivingHealEvent event) {
            if (event.getEntity().getUUID().equals(target)) {
                event.setCanceled(true);
            }
        }
    }

    private static Zombie target(GameTestHelper helper) {
        Zombie target = helper.spawn(EntityType.ZOMBIE, 1, 2, 1);
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(200);
        return target;
    }

    private static void equipProtection(GameTestHelper helper, LivingEntity target) {
        var protection = helper.getLevel()
                .registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.PROTECTION);
        ItemStack[] stacks = {
            new ItemStack(Items.NETHERITE_BOOTS),
            new ItemStack(Items.NETHERITE_LEGGINGS),
            new ItemStack(Items.NETHERITE_CHESTPLATE),
            new ItemStack(Items.NETHERITE_HELMET)
        };
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < stacks.length; i++) {
            stacks[i].enchant(protection, 10);
            target.setItemSlot(slots[i], stacks[i]);
        }
        // Equipment attribute changes are normally observed during the next entity tick.
        target.tick();
        target.setHealth(200);
    }

    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.002, message + ": expected " + expected + ", got " + actual);
    }
}
