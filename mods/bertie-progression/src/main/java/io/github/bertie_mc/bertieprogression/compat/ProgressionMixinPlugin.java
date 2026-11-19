package io.github.bertie_mc.bertieprogression.compat;

import java.util.List;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class ProgressionMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("AugmentationCenterMixin")) {
            return loaded("oritech") && loaded("create");
        }
        if (mixinClassName.endsWith("UnboundMixingCapacityMixin")) return loaded("unbound") && loaded("create");
        if (mixinClassName.endsWith("UnboundResourceMixin")
                || mixinClassName.endsWith("UnboundJournalMixin")
                || mixinClassName.endsWith("UnboundRepairMixin")) return loaded("unbound");
        if (mixinClassName.endsWith("UnboundVacuumMixin")) {
            return loaded("oritech") && loaded("unbound");
        }
        return !mixinClassName.endsWith("WardenModelMixin") || FMLEnvironment.dist == Dist.CLIENT;
    }

    private static boolean loaded(String id) {
        return FMLLoader.getLoadingModList().getModFileById(id) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
