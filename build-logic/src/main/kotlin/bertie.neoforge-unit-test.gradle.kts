import io.github.bertie_mc.gradle.BertieModMetadata
import io.github.bertie_mc.gradle.loadOnly
import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
}

val modMetadata = extensions.getByType<BertieModMetadata>()

extensions.configure<NeoForgeExtension> {
    unitTest {
        enable()
        loadOnly(mods.getByName(modMetadata.id))
    }
}
