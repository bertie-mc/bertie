import org.gradle.jvm.tasks.Jar

plugins {
    id("bertie.minecraft-library")
}

group = "io.github.bertie_mc.testing"
version = "1"

base {
    archivesName = "bertie-client-test-api"
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "FMLModType" to "LIBRARY",
            "Automatic-Module-Name" to "io.github.bertie_mc.testing.client.api",
        )
    }
}
