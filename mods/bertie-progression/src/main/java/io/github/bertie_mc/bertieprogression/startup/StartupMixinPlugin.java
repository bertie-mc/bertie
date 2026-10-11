package io.github.bertie_mc.bertieprogression.startup;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class StartupMixinPlugin implements IMixinConfigPlugin {
    private static final String MIDNIGHT_CONFIG = "eu.midnightdust.lib.config.MidnightConfig";

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!MIDNIGHT_CONFIG.equals(targetClassName)) {
            return;
        }
        // Registration and JSON loading share static maps. A class monitor also releases on
        // exceptional returns, and leaves the library's method body and entry ordering intact.
        for (var method : targetClass.methods) {
            if (method.name.equals("init")
                    && method.desc.equals("(Ljava/lang/String;Ljava/lang/Class;)V")
                    && (method.access & Opcodes.ACC_STATIC) != 0) {
                method.access |= Opcodes.ACC_SYNCHRONIZED;
                return;
            }
        }
        throw new IllegalStateException("MidnightConfig.init(String, Class) was not found");
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
