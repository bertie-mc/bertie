package io.github.bertie_mc.bertieprogression.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import io.github.bertie_mc.bertieprogression.recipe.CuttingRecipeAccess;
import io.github.bertie_mc.bertieprogression.recipe.CuttingRecipeRules;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecipeManager.class, priority = 800)
public abstract class CuttingRecipeManagerMixin implements CuttingRecipeAccess {
    @Shadow
    private Map<ResourceLocation, RecipeHolder<?>> byName;

    @Shadow
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Inject(
            method =
                    "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"),
            require = 1)
    private void bertie$afterRecipes(
            Map<ResourceLocation, JsonElement> data,
            ResourceManager resources,
            ProfilerFiller profiler,
            CallbackInfo ci) {
        bertie$separateWood();
    }

    @Override
    @Unique
    public void bertie$separateWood() {
        if (!ModList.get().isLoaded("sawmill")) return;
        var recipes = CuttingRecipeRules.separate(byName.values(), registries);
        ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> names = ImmutableMap.builder();
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> types = ImmutableMultimap.builder();
        for (RecipeHolder<?> recipe : recipes) {
            names.put(recipe.id(), recipe);
            types.put(recipe.value().getType(), recipe);
        }
        byName = names.build();
        byType = types.build();
    }
}
