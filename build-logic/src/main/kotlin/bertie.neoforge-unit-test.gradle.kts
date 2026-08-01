import mc.bertie.gradle.BertieModMetadata
import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
}

val modMetadata = extensions.getByType<BertieModMetadata>()

extensions.configure<NeoForgeExtension> {
    unitTest {
        enable()
        testedMod = mods.getByName(modMetadata.id)
    }
}
