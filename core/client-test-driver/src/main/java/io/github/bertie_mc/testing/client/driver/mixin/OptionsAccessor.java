package io.github.bertie_mc.testing.client.driver.mixin;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Options.class)
public interface OptionsAccessor {
    @Invoker("processOptions")
    void bertie$processOptions(Options.FieldAccess access);
}
