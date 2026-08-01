import mc.bertie.gradle.JavaAgentArgumentProvider
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test

plugins {
    id("bertie.jvm-test")
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val mockito = versionCatalog.findLibrary("mockito").get()
val mockitoAgent = configurations.create("mockitoAgent") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val mockitoAgentDependency = dependencies.create(mockito.get()) as ExternalModuleDependency
mockitoAgentDependency.isTransitive = false

dependencies {
    add("testImplementation", mockito)
    add(mockitoAgent.name, mockitoAgentDependency)
}

tasks.named<Test>("test") {
    jvmArgumentProviders.add(
        project.objects.newInstance<JavaAgentArgumentProvider>().apply {
            agentClasspath.from(mockitoAgent)
        },
    )
}
