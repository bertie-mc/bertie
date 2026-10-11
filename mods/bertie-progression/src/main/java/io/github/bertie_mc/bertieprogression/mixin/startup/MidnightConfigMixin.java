package io.github.bertie_mc.bertieprogression.mixin.startup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "eu.midnightdust.lib.config.MidnightConfig", remap = false)
public abstract class MidnightConfigMixin {}
