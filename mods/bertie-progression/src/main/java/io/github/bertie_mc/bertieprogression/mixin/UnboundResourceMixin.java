package io.github.bertie_mc.bertieprogression.mixin;

import io.github.bertie_mc.bertieprogression.compat.UnboundData;
import java.io.InputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
        targets = {
            "me.moltea.unbound.block.gate.GateRules",
            "me.moltea.unbound.creature.CreatureRoster",
            "me.moltea.unbound.gear.GearCatalog",
            "me.moltea.unbound.natives.NativeRoster",
            "me.moltea.unbound.natives.quest.QuestBook",
            "me.moltea.unbound.registry.ModDecor",
            "me.moltea.unbound.registry.ModDungeonSets",
            "me.moltea.unbound.registry.ModFamilies",
            "me.moltea.unbound.registry.ModFlora",
            "me.moltea.unbound.registry.ModItems",
            "me.moltea.unbound.registry.ModOres",
            "me.moltea.unbound.registry.ModSounds",
            "me.moltea.unbound.registry.ModUses",
            "me.moltea.unbound.registry.ModVillagers",
            "me.moltea.unbound.script.Script",
            "me.moltea.unbound.space.ShipCatalog",
            "me.moltea.unbound.space.combat.RaiderKind",
            "me.moltea.unbound.space.emitter.EmitterRules",
            "me.moltea.unbound.space.research.ResearchCatalog",
            "me.moltea.unbound.space.station.StationLayout",
            "me.moltea.unbound.space.station.order.Gifts",
            "me.moltea.unbound.space.station.order.OrderCatalog",
            "me.moltea.unbound.spectral.Spectra",
            "me.moltea.unbound.spectral.Uses"
        },
        remap = false)
public abstract class UnboundResourceMixin {
    @Redirect(
            method = {"<clinit>", "*"},
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Ljava/lang/ClassLoader;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"),
            require = 0)
    private static InputStream bertie$nativeData(ClassLoader loader, String path) {
        return UnboundData.resource(loader, path);
    }
}
