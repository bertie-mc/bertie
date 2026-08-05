package io.github.bertie_mc.testing.client.driver.mixin.context;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes a cycle button's stable label for translated screen-button lookup. */
@Mixin(CycleButton.class)
public interface CycleButtonAccessor {
    @Accessor("name")
    Component bertie$getName();
}
