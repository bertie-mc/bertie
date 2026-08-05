package io.github.bertie_mc.testing.client.driver.mixin.options;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Runs vanilla's option visitor so the driver can capture and restore a complete baseline. */
@Mixin(Options.class)
public interface OptionsAccessor {
    @Invoker("processOptions")
    void bertie$processOptions(Options.FieldAccess access);
}
