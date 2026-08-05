import org.gradle.jvm.tasks.Jar

plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
}

dependencies {
    api(project(":core:client-test-api"))
    testRuntimeOnly(libs.asm)
}

tasks.named<Jar>("jar") {
    manifest.attributes(
        "Automatic-Module-Name" to "io.github.bertie_mc.testing.client.driver",
    )
}
