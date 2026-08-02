package io.github.bertie_mc.configmigrations.integration.wunderlib.mixin;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "de.ambertation.wunderlib.configs.AbstractConfig", remap = false)
interface AbstractConfigAccessor {
    @Accessor("root")
    JsonObject configmigrations$root();

    @Accessor("location")
    ResourceLocation configmigrations$location();

    @Invoker("save")
    void configmigrations$save(boolean force);
}
