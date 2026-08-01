import org.gradle.api.tasks.testing.Test

plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.server-run")
}

dependencies {
    // Ponder is published directly by Create's Maven; use the slim Create artifact so
    // its nested libraries do not have to be unpacked into this build.
    compileOnly("com.simibubi.create:create-1.21.1:${libs.versions.create.maven.get()}:slim") {
        isTransitive = false
    }
    compileOnly(libs.ponder) {
        isTransitive = false
    }
}

tasks.named<Test>("test") {
    systemProperty("bertie.projectDir", layout.projectDirectory.asFile.absolutePath)
}
