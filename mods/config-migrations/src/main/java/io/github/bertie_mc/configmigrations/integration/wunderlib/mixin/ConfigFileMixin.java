package io.github.bertie_mc.configmigrations.integration.wunderlib.mixin;

import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import java.io.File;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Migrates a loaded JSON root before the concrete config constructs its values. */
@Pseudo
@Mixin(targets = "de.ambertation.wunderlib.configs.ConfigFile", remap = false)
abstract class ConfigFileMixin {
    @Shadow
    @Final
    private File path;

    @Inject(
            method = "<init>(Lde/ambertation/wunderlib/utils/Version$ModVersionProvider;"
                    + "Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("RETURN"))
    private void configmigrations$migrate(CallbackInfo callbackInfo) {
        AbstractConfigAccessor config = (AbstractConfigAccessor) this;
        MigrationRuntime.migrateWunderLib(
                config.configmigrations$location(),
                path.toPath(),
                config.configmigrations$root(),
                () -> config.configmigrations$save(true));
    }
}
