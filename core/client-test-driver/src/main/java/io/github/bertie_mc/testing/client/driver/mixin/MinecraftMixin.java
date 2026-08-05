package io.github.bertie_mc.testing.client.driver.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.testing.client.driver.ClientTestScreenshots;
import io.github.bertie_mc.testing.client.driver.TestScheduler;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Unique
    private Runnable bertie$deferredTestTask;

    @WrapMethod(method = "run")
    private void bertie$trackClientLifecycle(Operation<Void> original) throws Throwable {
        Throwable failure = null;
        try {
            original.call();
        } catch (Throwable clientFailure) {
            failure = clientFailure;
            TestScheduler.clientFailed(clientFailure);
        } finally {
            try {
                TestScheduler.clientStopped();
            } catch (Throwable stopFailure) {
                failure = bertie$append(failure, stopFailure);
            }
            TestScheduler.awaitTestThreadTermination();
            failure = bertie$append(failure, TestScheduler.takeTerminalFailure());
        }
        if (failure != null) {
            throw failure;
        }
    }

    @ModifyExpressionValue(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceTime(JZ)I"))
    private int bertie$limitTicksPerFrame(int ticksPerFrame) {
        return TestScheduler.capClientTicksPerFrame(ticksPerFrame);
    }

    @Inject(method = "emergencySaveAndCrash", at = @At("HEAD"))
    private void bertie$releaseSchedulerBeforeCrash(CrashReport report, CallbackInfo callback) {
        TestScheduler.clientFailed(report.getException());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void bertie$enterClientTick(CallbackInfo callback) {
        bertie$runClientPhase();
    }

    @Inject(
            method = "runTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V",
                    shift = At.Shift.AFTER))
    private void bertie$captureScreenshotAfterRender(
            boolean renderLevel, CallbackInfo callback) {
        ClientTestScreenshots.afterRender((Minecraft) (Object) this);
    }

    @Inject(method = "doWorldLoad", at = @At("HEAD"), cancellable = true)
    private void bertie$deferIntegratedServerStart(
            LevelStorageSource.LevelStorageAccess storage,
            PackRepository packs,
            WorldStem worldStem,
            boolean newWorld,
            CallbackInfo callback) {
        if (TestScheduler.isClientTaskRunning()) {
            Minecraft self = (Minecraft) (Object) this;
            bertie$deferredTestTask = () -> self.doWorldLoad(storage, packs, worldStem, newWorld);
            callback.cancel();
        }
    }

    @Inject(
            method = "doWorldLoad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"))
    private void bertie$coordinateIntegratedServerStart(
            LevelStorageSource.LevelStorageAccess storage,
            PackRepository packs,
            WorldStem worldStem,
            boolean newWorld,
            CallbackInfo callback) {
        bertie$runClientPhase();
    }

    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void bertie$deferIntegratedServerStop(
            Screen nextScreen, boolean keepResourcePacks, CallbackInfo callback) {
        Minecraft self = (Minecraft) (Object) this;
        if (self.getSingleplayerServer() != null && TestScheduler.isClientTaskRunning()) {
            bertie$deferredTestTask = () -> self.disconnect(nextScreen, keepResourcePacks);
            callback.cancel();
        }
    }

    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;runTick(Z)V"))
    private void bertie$coordinateIntegratedServerStop(
            Screen nextScreen, boolean keepResourcePacks, CallbackInfo callback) {
        bertie$runClientPhase();
    }

    @Unique
    private void bertie$runClientPhase() {
        TestScheduler.clientTick();
        Runnable deferred = bertie$deferredTestTask;
        bertie$deferredTestTask = null;
        if (deferred != null) {
            deferred.run();
        }
    }

    @Unique
    private static Throwable bertie$append(Throwable existing, Throwable additional) {
        if (additional == null) {
            return existing;
        }
        if (existing == null) {
            return additional;
        }
        if (existing != additional) {
            existing.addSuppressed(additional);
        }
        return existing;
    }
}
