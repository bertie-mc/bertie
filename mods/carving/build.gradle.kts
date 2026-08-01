import org.gradle.api.tasks.testing.Test

plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.gametest")
    id("bertie.server-run")
}

dependencies {
    compileOnly(libs.slag)
    compileOnly(libs.emi)
}

tasks.named<Test>("test") {
    systemProperty("bertie.projectDir", layout.projectDirectory.asFile.absolutePath)
}
