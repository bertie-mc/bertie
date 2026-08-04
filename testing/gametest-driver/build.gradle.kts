import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("bertie.neoforge-platform-metadata")
    id("bertie.jvm-test")
}

group = "io.github.bertie_mc.testing"
version = "1"

base {
    archivesName = "bertie-gametest-driver"
}

extensions.configure<NeoForgeExtension> {
    mods.register("bertie_gametest_driver") {
        sourceSet(extensions.getByType<SourceSetContainer>().named("main").get())
    }
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "Automatic-Module-Name" to "io.github.bertie_mc.testing.gametest.driver",
    )
}
