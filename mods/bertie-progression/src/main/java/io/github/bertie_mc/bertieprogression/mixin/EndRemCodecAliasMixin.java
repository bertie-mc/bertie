package io.github.bertie_mc.bertieprogression.mixin;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Newer registries reject End Remastered's second key for the same codec object. */
@Pseudo
@Mixin(targets = "com.teamremastered.endrem.registry.RegisterHandler", remap = false)
public abstract class EndRemCodecAliasMixin {
    @Redirect(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/neoforged/neoforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/neoforged/neoforge/registries/DeferredHolder;"),
            require = 1)
    private static DeferredHolder<?, ?> bertieprogression$distinctAlias(
            DeferredRegister<MapCodec<? extends IGlobalLootModifier>> registry,
            String name,
            Supplier<MapCodec<? extends IGlobalLootModifier>> supplier) {
        return registry.register(name, () -> distinct(supplier.get()));
    }

    private static <T> MapCodec<T> distinct(MapCodec<T> codec) {
        return codec.xmap(value -> value, value -> value);
    }
}
