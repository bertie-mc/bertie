package io.github.bertie_mc.bertieprogression.mixin.combat;

import io.github.bertie_mc.bertieprogression.combat.DamageFamilies;
import io.github.bertie_mc.bertieprogression.combat.DamageFamily;
import io.github.bertie_mc.bertieprogression.combat.RoutedDamage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public abstract class DamageSourceFamiliesMixin implements RoutedDamage {
    @Unique
    private DamageFamily bertie$family;

    @Unique
    private boolean bertie$explosion;

    @Unique
    private boolean bertie$transferred;

    @Unique
    private ResourceLocation bertie$school;

    @Unique
    private int bertie$properties;

    @Unique
    private boolean bertie$hasProperties;

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void bertie$familyTag(TagKey<DamageType> tag, CallbackInfoReturnable<Boolean> cir) {
        Boolean value = DamageFamilies.matches((DamageSource) (Object) this, tag);
        if (value != null) {
            cir.setReturnValue(value);
        }
    }

    public DamageFamily bertie$getFamily() {
        return bertie$family;
    }

    public void bertie$setFamily(DamageFamily family) {
        bertie$family = family;
    }

    public boolean bertie$isExplosion() {
        return bertie$explosion;
    }

    public void bertie$setExplosion(boolean explosion) {
        bertie$explosion = explosion;
    }

    public boolean bertie$isTransferred() {
        return bertie$transferred;
    }

    public void bertie$setTransferred(boolean transferred) {
        bertie$transferred = transferred;
    }

    public ResourceLocation bertie$school() {
        return bertie$school;
    }

    public void bertie$school(ResourceLocation school) {
        bertie$school = school;
    }

    public int bertie$properties() {
        return bertie$properties;
    }

    public boolean bertie$hasProperties() {
        return bertie$hasProperties;
    }

    public void bertie$properties(int properties) {
        bertie$properties = properties;
        bertie$hasProperties = true;
    }
}
