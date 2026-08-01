import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test

plugins {
    id("bertie.neoforge-mod")
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("testImplementation", platform(versionCatalog.findLibrary("junit-bom").get()))
    add("testImplementation", versionCatalog.findLibrary("junit-jupiter").get())
    add("testRuntimeOnly", versionCatalog.findLibrary("junit-launcher").get())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
