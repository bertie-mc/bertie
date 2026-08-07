package io.github.bertie_mc.bertieprogression.backpack;

import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.RitualResultType;
import com.stal111.forbidden_arcanus.core.registry.FARegistries;
import io.github.bertie_mc.bertieprogression.BertieProgression;
import java.util.function.Supplier;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Ritual result types this mod adds to Forbidden Arcanus.
 *
 * <p>{@code FARegistries.RITUAL_RESULT_TYPE} is a synced NeoForge registry, so registering into it
 * from here needs nothing beyond a DeferredRegister on its key.
 *
 * <p>Only touched when Forbidden Arcanus is present - see the guard in {@link BertieProgression}.
 */
public final class ModRitualResults {

    public static final DeferredRegister<RitualResultType<?>> TYPES =
            DeferredRegister.create(FARegistries.RITUAL_RESULT_TYPE, BertieProgression.MODID);

    public static final Supplier<RitualResultType<UpgradeStorageResult>> UPGRADE_STORAGE =
            TYPES.register("upgrade_storage", () -> new RitualResultType<>(UpgradeStorageResult.CODEC));

    private ModRitualResults() {}
}
