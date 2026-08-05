package io.github.bertie_mc.testing.client.driver.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.bertie_mc.testing.client.driver.InProcessDedicatedServer;
import io.github.bertie_mc.testing.client.driver.TestScheduler;
import java.util.function.BooleanSupplier;
import net.minecraft.CrashReport;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
    @WrapMethod(method = "runServer")
    private void bertie$trackServerThreadLifecycle(Operation<Void> original) {
        MinecraftServer self = (MinecraftServer) (Object) this;
        TestScheduler.serverThreadStarted(self);
        try {
            original.call();
        } finally {
            try {
                InProcessDedicatedServer.onServerThreadTerminated(self);
            } finally {
                TestScheduler.serverThreadStopped(self);
            }
        }
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void bertie$enterServerTick(BooleanSupplier hasTime, CallbackInfo callback) {
        TestScheduler.serverTick((MinecraftServer) (Object) this);
    }

    @Inject(method = "onServerCrash", at = @At("HEAD"))
    private void bertie$recordServerCrash(CrashReport report, CallbackInfo callback) {
        TestScheduler.recordFailure(report.getException());
        InProcessDedicatedServer.onServerCrash((MinecraftServer) (Object) this, report);
    }
}
