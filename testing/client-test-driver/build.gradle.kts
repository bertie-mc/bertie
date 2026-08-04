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
    archivesName = "bertie-client-test-driver"
}

dependencies {
    api(project(":testing:client-test-api"))
    testRuntimeOnly(libs.asm)
}

extensions.configure<NeoForgeExtension> {
    accessTransformers.from(
        layout.projectDirectory.file("src/main/resources/META-INF/accesstransformer.cfg"),
    )
    mods.register("bertie_client_test_driver") {
        sourceSet(extensions.getByType<SourceSetContainer>().named("main").get())
    }
    unitTest {
        enable()
        testedMod = mods.getByName("bertie_client_test_driver")
    }
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "Automatic-Module-Name" to "io.github.bertie_mc.testing.client.driver",
    )
}
