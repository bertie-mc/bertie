package com.berlord.hephaestusarchitecture.mixin;

import com.stal111.forbidden_arcanus.common.block.entity.forge.ForgeDataCache;
import com.stal111.forbidden_arcanus.common.block.entity.forge.HephaestusForgeBlockEntity;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HephaestusForgeBlockEntity.class)
public interface HephaestusForgeBlockEntityAccessor {

    @Accessor(value = "dataCache", remap = false)
    ForgeDataCache hephaestusarchitecture$getDataCache();

    @Invoker(value = "onDataChanged", remap = false)
    void hephaestusarchitecture$onDataChanged(HolderLookup.Provider lookupProvider);
}
