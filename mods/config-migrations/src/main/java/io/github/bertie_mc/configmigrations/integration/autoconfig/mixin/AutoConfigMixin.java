package io.github.bertie_mc.configmigrations.integration.autoconfig.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Decorates serializers created by the optional AutoConfig library. */
@SuppressWarnings("unchecked")
@Pseudo
@Mixin(targets = "me.shedaniel.autoconfig.AutoConfig", remap = false)
abstract class AutoConfigMixin {
    @WrapOperation(
            method = "register(Ljava/lang/Class;Lme/shedaniel/autoconfig/serializer/"
                    + "ConfigSerializer$Factory;)Lme/shedaniel/autoconfig/ConfigHolder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/shedaniel/autoconfig/serializer/ConfigSerializer$Factory;create("
                            + "Lme/shedaniel/autoconfig/annotation/Config;Ljava/lang/Class;)"
                            + "Lme/shedaniel/autoconfig/serializer/ConfigSerializer;"))
    @SuppressWarnings("unchecked")
    private static <T extends ConfigData> ConfigSerializer<T> configmigrations$wrapSerializer(
            ConfigSerializer.Factory<T> factory,
            Config definition,
            Class<T> configClass,
            Operation<ConfigSerializer<T>> original) {
        ConfigSerializer<T> serializer = original.call(factory, definition, configClass);
        return (ConfigSerializer<T>) MigrationRuntime.wrapAutoConfigSerializer(definition.name(), serializer);
    }
}
