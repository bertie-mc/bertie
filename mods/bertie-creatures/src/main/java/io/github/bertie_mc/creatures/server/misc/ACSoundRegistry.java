package io.github.bertie_mc.creatures.server.misc;

import io.github.bertie_mc.creatures.BertieCreatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ACSoundRegistry {
    public static final DeferredRegister<SoundEvent> DEF_REG =
            DeferredRegister.create(Registries.SOUND_EVENT, BertieCreatures.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBER_MONOLITH_SUMMON =
            createSoundEvent("amber_monolith_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOOD_BASALT_STEP =
            createSoundEvent("flood_basalt_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOOD_BASALT_PLACE =
            createSoundEvent("flood_basalt_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOOD_BASALT_BREAK =
            createSoundEvent("flood_basalt_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOOD_BASALT_BREAKING =
            createSoundEvent("flood_basalt_breaking");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRIMAL_MAGMA_FISSURE_CLOSE =
            createSoundEvent("primal_magma_fissure_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> THORNWOOD_BRANCH_BREAK =
            createSoundEvent("thornwood_branch_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROTTOCERATOPS_IDLE =
            createSoundEvent("grottoceratops_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROTTOCERATOPS_HURT =
            createSoundEvent("grottoceratops_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROTTOCERATOPS_DEATH =
            createSoundEvent("grottoceratops_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROTTOCERATOPS_CALL =
            createSoundEvent("grottoceratops_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROTTOCERATOPS_ATTACK =
            createSoundEvent("grottoceratops_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROTTOCERATOPS_GRAZE =
            createSoundEvent("grottoceratops_graze");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROTTOCERATOPS_STEP =
            createSoundEvent("grottoceratops_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREMORSAURUS_IDLE =
            createSoundEvent("tremorsaurus_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREMORSAURUS_HURT =
            createSoundEvent("tremorsaurus_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREMORSAURUS_DEATH =
            createSoundEvent("tremorsaurus_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREMORSAURUS_BITE =
            createSoundEvent("tremorsaurus_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREMORSAURUS_ROAR =
            createSoundEvent("tremorsaurus_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREMORSAURUS_THROW =
            createSoundEvent("tremorsaurus_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREMORSAURUS_STOMP =
            createSoundEvent("tremorsaurus_stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_IDLE =
            createSoundEvent("luxtructosaurus_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_HURT =
            createSoundEvent("luxtructosaurus_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_DEATH =
            createSoundEvent("luxtructosaurus_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_SNORT =
            createSoundEvent("luxtructosaurus_snort");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_STEP =
            createSoundEvent("luxtructosaurus_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_STOMP =
            createSoundEvent("luxtructosaurus_stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_ROAR =
            createSoundEvent("luxtructosaurus_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_ATTACK_STOMP =
            createSoundEvent("luxtructosaurus_attack_stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_KICK =
            createSoundEvent("luxtructosaurus_kick");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_TAIL =
            createSoundEvent("luxtructosaurus_tail");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_BREATH =
            createSoundEvent("luxtructosaurus_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_SUMMON =
            createSoundEvent("luxtructosaurus_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUXTRUCTOSAURUS_JUMP =
            createSoundEvent("luxtructosaurus_jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATLATITAN_IDLE = createSoundEvent("atlatitan_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATLATITAN_HURT = createSoundEvent("atlatitan_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATLATITAN_DEATH = createSoundEvent("atlatitan_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATLATITAN_STEP = createSoundEvent("atlatitan_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATLATITAN_STOMP = createSoundEvent("atlatitan_stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATLATITAN_KICK = createSoundEvent("atlatitan_kick");
    public static final DeferredHolder<SoundEvent, SoundEvent> ATLATITAN_TAIL = createSoundEvent("atlatitan_tail");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEEPER_IDLE = createSoundEvent("nucleeper_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEEPER_HURT = createSoundEvent("nucleeper_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEEPER_DEATH = createSoundEvent("nucleeper_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEEPER_STEP = createSoundEvent("nucleeper_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEEPER_CHARGE = createSoundEvent("nucleeper_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> HULLBREAKER_IDLE = createSoundEvent("hullbreaker_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HULLBREAKER_HURT = createSoundEvent("hullbreaker_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HULLBREAKER_DEATH =
            createSoundEvent("hullbreaker_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HULLBREAKER_LAND_IDLE =
            createSoundEvent("hullbreaker_land_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HULLBREAKER_LAND_HURT =
            createSoundEvent("hullbreaker_land_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HULLBREAKER_LAND_DEATH =
            createSoundEvent("hullbreaker_land_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HULLBREAKER_ATTACK =
            createSoundEvent("hullbreaker_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_ONE_MAGE_IDLE =
            createSoundEvent("deep_one_mage_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_ONE_MAGE_HURT =
            createSoundEvent("deep_one_mage_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_ONE_MAGE_DEATH =
            createSoundEvent("deep_one_mage_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_ONE_MAGE_ADMIRE =
            createSoundEvent("deep_one_mage_admire");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_ONE_MAGE_HOSTILE =
            createSoundEvent("deep_one_mage_hostile");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEEP_ONE_MAGE_ATTACK =
            createSoundEvent("deep_one_mage_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> VESPER_IDLE = createSoundEvent("vesper_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> VESPER_HURT = createSoundEvent("vesper_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> VESPER_DEATH = createSoundEvent("vesper_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> VESPER_QUIET_IDLE =
            createSoundEvent("vesper_quiet_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> VESPER_FLAP = createSoundEvent("vesper_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> VESPER_SCREAM = createSoundEvent("vesper_scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> TECTONIC_SHARD_TRANSFORM =
            createSoundEvent("tectonic_shard_transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_CONCH_SUMMON =
            createSoundEvent("magic_conch_summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_STAFF_WOOSH = createSoundEvent("sea_staff_woosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_STAFF_HIT = createSoundEvent("sea_staff_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEA_STAFF_BUBBLE = createSoundEvent("sea_staff_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEPHRA_WHISTLE = createSoundEvent("tephra_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEPHRA_HIT = createSoundEvent("tephra_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION =
            createSoundEvent("nuclear_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_NUCLEAR_EXPLOSION =
            createSoundEvent("large_nuclear_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION_RUMBLE =
            createSoundEvent("nuclear_explosion_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION_RINGING =
            createSoundEvent("nuclear_explosion_ringing");

    private static DeferredHolder<SoundEvent, SoundEvent> createSoundEvent(final String soundName) {
        return DEF_REG.register(
                soundName,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(BertieCreatures.MODID, soundName)));
    }
}
