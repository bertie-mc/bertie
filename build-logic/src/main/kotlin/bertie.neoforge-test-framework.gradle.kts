import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("bertie.neoforge-unit-test")
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val testFramework = versionCatalog.findLibrary("neoforge-test-framework").orElseThrow {
    IllegalStateException("Library 'neoforge-test-framework' is missing from the root version catalog")
}

dependencies {
    testImplementation(testFramework)
}
