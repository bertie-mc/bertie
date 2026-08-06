package io.github.bertie_mc.configmigrations.integration.minecraft.options.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.configmigrations.integration.minecraft.options.MinecraftOptionsIntegration;
import java.io.File;
import net.minecraft.client.Options;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies options migrations after vanilla data fixing and before values are consumed. */
@Mixin(Options.class)
abstract class OptionsMixin {
    @Shadow
    @Final
    private File optionsFile;

    @Unique
    private MinecraftOptionsIntegration configmigrations$integration;

    @Unique
    private MinecraftOptionsIntegration.PendingMigration configmigrations$pending;

    @Unique
    private boolean configmigrations$loaded;

    @WrapMethod(method = "load(Z)V")
    private void configmigrations$migrate(boolean limited, Operation<Void> original) {
        if (limited) {
            original.call(limited);
            return;
        }

        if (configmigrations$integration == null) {
            configmigrations$integration =
                    MinecraftOptionsIntegration.load(optionsFile.toPath().getParent());
        }
        MinecraftOptionsIntegration.PendingMigration pending =
                configmigrations$integration.prepare(optionsFile.toPath());
        if (pending == null) {
            original.call(limited);
            return;
        }

        configmigrations$pending = pending;
        configmigrations$loaded = false;
        try {
            original.call(limited);
            if (pending.failure() != null) {
                throw pending.failure();
            }
            if (configmigrations$loaded) {
                pending.commit();
            }
        } finally {
            configmigrations$pending = null;
        }
    }

    @ModifyReturnValue(
            method = "dataFix(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"))
    private CompoundTag configmigrations$merge(CompoundTag options) {
        MinecraftOptionsIntegration.PendingMigration pending = configmigrations$pending;
        if (pending != null) {
            pending.apply(options);
        }
        return options;
    }

    @Inject(
            method = "load(Z)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/KeyMapping;resetMapping()V",
                            shift = At.Shift.AFTER))
    private void configmigrations$markLoaded(boolean limited, CallbackInfo callbackInfo) {
        if (!limited && configmigrations$pending != null) {
            configmigrations$loaded = true;
        }
    }
}
