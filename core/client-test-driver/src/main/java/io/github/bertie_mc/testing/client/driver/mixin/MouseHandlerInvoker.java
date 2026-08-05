package io.github.bertie_mc.testing.client.driver.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    @Invoker("onPress")
    void bertie$onPress(long window, int button, int action, int modifiers);

    @Invoker("onScroll")
    void bertie$onScroll(long window, double horizontal, double vertical);

    @Invoker("onMove")
    void bertie$onMove(long window, double x, double y);
}
