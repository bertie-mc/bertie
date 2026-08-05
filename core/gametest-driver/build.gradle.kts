import org.gradle.jvm.tasks.Jar

plugins {
    id("bertie.mod")
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "Automatic-Module-Name" to "io.github.bertie_mc.testing.gametest.driver",
    )
}
