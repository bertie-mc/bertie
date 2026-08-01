import org.gradle.api.tasks.testing.Test

plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
    id("bertie.gametest")
    id("bertie.server-run")
}

dependencies {
    testImplementation(libs.gson)
}

tasks.named<Test>("test") {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
