package io.github.bertie_mc.configmigrations.integration.resourcefulconfig.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Migrates parsed configs between Configurator's initial native load and save. */
@Pseudo
@Mixin(targets = "com.teamresourceful.resourcefulconfig.api.loader.Configurator", remap = false)
abstract class ConfiguratorMixin {
    private static final String SAVE =
            "Lcom/teamresourceful/resourcefulconfig/api/types/ResourcefulConfig;save()V";

    @Shadow
    @Final
    private String modid;

    @WrapOperation(
            method = "loadConfigClass(Ljava/lang/Class;Ljava/util/function/Consumer;)"
                    + "Lcom/teamresourceful/resourcefulconfig/api/types/ResourcefulConfig;",
            at = @At(value = "INVOKE", target = SAVE))
    private void configmigrations$migrateParsedClass(
            ResourcefulConfig config, Operation<Void> original) {
        migrate(config, original);
    }

    @WrapOperation(
            method = "register(Lcom/teamresourceful/resourcefulconfig/api/types/ResourcefulConfig;"
                    + "Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = SAVE))
    private void configmigrations$migrateRegisteredConfig(
            ResourcefulConfig config, Operation<Void> original) {
        migrate(config, original);
    }

    private void migrate(ResourcefulConfig config, Operation<Void> original) {
        MigrationRuntime.migrateResourcefulConfig(
                modid, config.id(), config, () -> original.call(config));
    }
}
