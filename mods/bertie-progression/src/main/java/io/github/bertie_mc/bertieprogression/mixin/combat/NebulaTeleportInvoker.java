package io.github.bertie_mc.bertieprogression.mixin.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "auviotre.enigmatic.legacy.contents.item.spellstones.EyeOfNebula", remap = false)
public interface NebulaTeleportInvoker {
    @Invoker("dodgeTeleport")
    static void bertie$teleport(ServerLevel level, Entity attacker, LivingEntity wearer) {
        throw new AssertionError();
    }
}
