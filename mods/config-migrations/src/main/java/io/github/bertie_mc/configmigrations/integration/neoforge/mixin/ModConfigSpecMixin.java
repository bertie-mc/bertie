package io.github.bertie_mc.configmigrations.integration.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;

/** Connects NeoForge's standard config specification to the migration lifecycle. */
@Mixin(value = ModConfigSpec.class, remap = false)
abstract class ModConfigSpecMixin {
    @WrapMethod(method = "validateSpec(Lnet/neoforged/fml/config/ModConfig;)V")
    private void configmigrations$registerSpec(ModConfig modConfig, Operation<Void> original) {
        original.call(modConfig);
        MigrationRuntime.registerNeoForgeSpec((IConfigSpec) (Object) this, modConfig);
    }

    @WrapMethod(method = "acceptConfig(Lnet/neoforged/fml/config/IConfigSpec$ILoadedConfig;)V")
    private void configmigrations$accept(IConfigSpec.ILoadedConfig loadedConfig, Operation<Void> original) {
        MigrationRuntime.acceptNeoForge((IConfigSpec) (Object) this, loadedConfig, () -> original.call(loadedConfig));
    }
}
