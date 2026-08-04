import io.github.bertie_mc.gradle.runTestsTask
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(platform(versionCatalog.findLibrary("junit-bom").get()))
    testImplementation(versionCatalog.findLibrary("junit-jupiter").get())
    testRuntimeOnly(versionCatalog.findLibrary("junit-launcher").get())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

runTestsTask().configure {
    dependsOn(tasks.named("test"))
}
