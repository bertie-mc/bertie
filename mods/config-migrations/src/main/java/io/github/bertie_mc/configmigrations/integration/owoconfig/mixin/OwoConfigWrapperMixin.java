package io.github.bertie_mc.configmigrations.integration.owoconfig.mixin;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import io.github.bertie_mc.configmigrations.integration.MigrationRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Merges the native Jankson tree during ConfigWrapper's central load lifecycle. */
@Pseudo
@Mixin(targets = "io.wispforest.owo.config.ConfigWrapper", remap = false)
abstract class OwoConfigWrapperMixin {
    @Unique
    private boolean configmigrations$loadSucceeded;

    @Shadow
    public abstract String name();

    @Shadow
    public abstract Path fileLocation();

    @Shadow
    public abstract void save();

    @Inject(method = "load()V", at = @At("HEAD"))
    private void configmigrations$prepare(CallbackInfo callbackInfo) {
        configmigrations$loadSucceeded = false;
        Path path = fileLocation();
        if (MigrationRuntime.prepareOwoConfigLoad(name(), path) && !Files.exists(path)) {
            save();
        }
    }

    @Redirect(
            method = "load()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lblue/endless/jankson/Jankson;load(Ljava/lang/String;)Lblue/endless/jankson/JsonObject;"))
    private JsonObject configmigrations$merge(Jankson parser, String source) throws SyntaxError {
        JsonObject document = parser.load(source);
        return (JsonObject) MigrationRuntime.mergeOwoConfigDocument(name(), document);
    }

    @Inject(
            method = "load()V",
            at = @At(
                    value = "FIELD",
                    target = "Lio/wispforest/owo/config/ConfigWrapper;loading:Z",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 1))
    private void configmigrations$markSuccessfulLoad(CallbackInfo callbackInfo) {
        configmigrations$loadSucceeded = true;
    }

    @Inject(method = "load()V", at = @At("RETURN"))
    private void configmigrations$finish(CallbackInfo callbackInfo) {
        if (configmigrations$loadSucceeded) {
            MigrationRuntime.finishOwoConfigLoad(name(), this::save);
        } else {
            MigrationRuntime.cancelOwoConfigLoad(name());
        }
    }
}
