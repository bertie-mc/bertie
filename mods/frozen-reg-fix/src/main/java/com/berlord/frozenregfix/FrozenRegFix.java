package com.berlord.frozenregfix;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Some mods register content into a built-in registry (armor_material, item, entity_type, ...)
 * inside a class's STATIC INITIALIZER, and only touch that class lazily -- sometimes not until
 * the first client tick. NeoForge FREEZES the registries after mod loading, so in a large pack
 * where nothing references that class during loading the clinit runs post-freeze and throws
 * "Registry is already frozen". The same jar works in a smaller pack where something touches the
 * class in time.
 *
 * Known target in the bertie pack:
 *   - immersive_armors.Items   (armor_material; crashes on first client tick via ItemsClient.setupPieces)
 *
 * The preferred fix is two mixins that both call {@link #forceTargets()} (idempotent, guarded so it runs once):
 *   - {@link com.berlord.frozenregfix.mixin.GameDataMixin}        @ GameData.freezeData() HEAD
 *   - {@link com.berlord.frozenregfix.mixin.MappedRegistryMixin}  @ MappedRegistry.freeze() HEAD, for the
 *     armor_material registry only.
 * The GameData hook is the clean "all RegisterEvents done, nothing frozen yet" point. But a load-order
 * disruptor (railways-untold shifting Create's coremods) can reroute the freeze so GameData.freezeData()'s
 * injected code never runs. The MappedRegistry hook is the backstop: it fires at the HEAD of the
 * armor_material registry's OWN freeze(), which happens however the freeze is orchestrated -- at that
 * moment armor_material is still open, so forcing IA's Items clinit lands its materials in the writable
 * window. NeoForge 21.1.233 performs no second low-level armor-material freeze after mod construction,
 * so {@code MappedRegistryMixin} also bypasses only the frozen-state guard for late
 * {@code immersive_armors} armor-material keys. All other registries and namespaces remain protected.
 *
 * v1.0.0 used a mod-bus RegisterEvent listener (never fired in big packs). v2.0.0 used GameData.freezeData
 * (defeated by railways). v2.1.0 added the MappedRegistry.freeze backstop. v2.2.0 gates that backstop with
 * {@link #modConstructed}: armor_material is frozen TWICE -- once during vanilla Bootstrap.bootStrap() (before
 * any mod is constructed; IA's Registration.Impl is still null so forcing Items there NPEs and permanently
 * poisons the class) and again during the mod-load re-freeze (after RegisterEvents). The flag skips the first
 * and only forces on the second. v2.4.0 adds the targeted fallback needed when that second freeze does
 * not occur.
 */
@Mod(FrozenRegFix.MOD_ID)
public class FrozenRegFix {
    public static final String MOD_ID = "frozenregfix";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Fully-qualified classes whose static initializer must run before the registries freeze. */
    public static final String[] TARGETS = {
        "immersive_armors.Items",
    };

    /** Set in the @Mod constructor: true once FML has begun constructing mods (i.e. we are past vanilla
     *  Bootstrap.bootStrap() and the targets' mods exist). Until then, force-init would run too early. */
    private static volatile boolean modConstructed = false;
    private static volatile boolean done = false;

    public FrozenRegFix() {
        modConstructed = true;
        LOGGER.info("[{}] loaded; will force lazy registry-content classes to init before the mod-load registry freeze", MOD_ID);
    }

    /** Idempotent: force-initializes every TARGET once, the first time any hook calls in AFTER mod
     *  construction. Calls during the early vanilla bootstrap freeze are ignored (mods not built yet). */
    public static synchronized void forceTargets(String source) {
        LOGGER.info("[{}] DIAG forceTargets from {} (modConstructed={}, done={})", MOD_ID, source, modConstructed, done);
        if (done || !modConstructed) {
            return;
        }
        done = true;
        for (String target : TARGETS) {
            try {
                Class.forName(target, true, FrozenRegFix.class.getClassLoader());
                LOGGER.info("[{}] force-initialized {} pre-freeze", MOD_ID, target);
            } catch (ClassNotFoundException e) {
                LOGGER.info("[{}] target {} not present, skipping", MOD_ID, target);
            } catch (Throwable t) {
                LOGGER.error("[{}] failed to force-initialize {}", MOD_ID, target, t);
            }
        }
    }
}
