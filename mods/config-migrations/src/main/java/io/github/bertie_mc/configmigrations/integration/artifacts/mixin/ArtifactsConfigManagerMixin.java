package io.github.bertie_mc.configmigrations.integration.artifacts.mixin;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Artifacts migrations after its native correction and before values are read. */
@Pseudo
@Mixin(targets = "artifacts.config.ConfigManager", remap = false)
abstract class ArtifactsConfigManagerMixin {
    @Shadow
    protected CommentedFileConfig config;

    @Shadow
    @Final
    protected ConfigSpec spec;

    @Shadow
    public abstract String getName();

    @Inject(
            method = "setup()V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/electronwill/nightconfig/core/file/FileWatcher;defaultInstance()"
                                    + "Lcom/electronwill/nightconfig/core/file/FileWatcher;"))
    private void configmigrations$migrate(CallbackInfo callbackInfo) {
        MigrationRuntime.migrateArtifacts(getName(), config, spec);
    }
}
