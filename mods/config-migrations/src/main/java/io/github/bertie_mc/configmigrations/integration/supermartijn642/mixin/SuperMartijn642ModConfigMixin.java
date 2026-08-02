package io.github.bertie_mc.configmigrations.integration.supermartijn642.mixin;

import com.supermartijn642.configlib.ConfigFile;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Migrates Config Lib's parsed document before it becomes live. */
@Pseudo
@Mixin(targets = "com.supermartijn642.configlib.ModConfig", remap = false)
abstract class SuperMartijn642ModConfigMixin {
    @Shadow
    @Final
    private ConfigFile<?> configFile;

    @Shadow
    public abstract String getModid();

    @Shadow
    public abstract String getIdentifier();

    @Inject(
            method = "initialize()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/supermartijn642/configlib/ConfigFile;readFile()V",
                    shift = At.Shift.AFTER))
    private void configmigrations$apply(CallbackInfo callbackInfo) {
        MigrationRuntime.applySuperMartijn642Config(getModid(), getIdentifier(), configFile);
    }

    @Inject(
            method = "initialize()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/supermartijn642/configlib/ConfigFile;writeFile()V",
                    shift = At.Shift.AFTER))
    private void configmigrations$commit(CallbackInfo callbackInfo) {
        MigrationRuntime.commitSuperMartijn642Config(getModid(), getIdentifier());
    }
}
