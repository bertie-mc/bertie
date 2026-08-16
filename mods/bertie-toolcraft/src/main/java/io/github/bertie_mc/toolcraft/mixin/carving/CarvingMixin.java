package io.github.bertie_mc.toolcraft.mixin.carving;

import io.github.bertie_mc.carving.Carving;
import io.github.bertie_mc.carving.CarvingMaterial;
import io.github.bertie_mc.carving.ToolKind;
import io.github.bertie_mc.toolcraft.compat.MagitechParts;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Points carving at Magitech's part system.
 *
 * <p>Berlord's Carving carves a tool head and hands the player a Slag part. Bertie keeps Slag for its
 * metallurgy but builds tools out of Magitech parts, so a carve has to produce one of those instead.
 * That is a pack decision, not a carving one — the standalone mod still ships its Slag behaviour, and
 * this reroute lives here.
 *
 * <p>Only tools move. Armour keeps carving's own path, override map and all.
 */
@Mixin(Carving.class)
public abstract class CarvingMixin {

    /**
     * The carved result becomes a Magitech part.
     *
     * <p>With Magitech present the Slag path is taken away entirely, empty result and all. Slag's
     * own tool assembly is switched off in this pack, so a Slag part would be a dead end; a material
     * Magitech has no equivalent for is better carving nothing than carving something unusable. It
     * also keeps the empty result out of EMI, which drops a carving entry whose output is empty.
     *
     * <p>Without Magitech nothing is touched and carving behaves as it does standalone.
     */
    @Inject(method = "resultStack", at = @At("HEAD"), cancellable = true)
    private static void bertietoolcraft$magitechPart(
            CarvingMaterial material,
            boolean armor,
            int kindIndex,
            int flaws,
            int penalty,
            CallbackInfoReturnable<ItemStack> cir) {
        if (armor || !ModList.get().isLoaded("magitech")) {
            return;
        }
        cir.setReturnValue(MagitechParts.build(material.id, ToolKind.byIndex(kindIndex).id, flaws, penalty));
    }

    /**
     * The hoe tab carves a scythe head.
     *
     * <p>Bertie has no hoe — the scythe is the hoe. Neither Slag nor Magitech ships a scythe-head
     * item to take a silhouette from, so the shape is cut from Magitech's assembled scythe, the same
     * source and the same outline as the part texture the carve produces.
     *
     * <p>The other four tabs keep their shapes: three heads carry over as they are, and the
     * sword-blade shape is close enough to a handle to stand in for one.
     */
    @Inject(method = "shapeKey", at = @At("HEAD"), cancellable = true)
    private static void bertietoolcraft$scytheShape(
            CarvingMaterial material, boolean armor, int kindIndex, CallbackInfoReturnable<String> cir) {
        if (!armor && ModList.get().isLoaded("magitech") && ToolKind.byIndex(kindIndex) == ToolKind.HOE) {
            cir.setReturnValue("slag/scythe_head");
        }
    }
}
