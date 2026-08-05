package io.github.bertie_mc.testing.client.driver.mixin.input;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes Minecraft's character-input handler for simulated Unicode input. */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerInvoker {
    @Invoker("charTyped")
    void bertie$charTyped(long window, int codePoint, int modifiers);
}
