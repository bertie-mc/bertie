package io.github.bertie_mc.gradle.plugins

import io.github.bertie_mc.gradle.conventions.configureJvmRole
import io.github.bertie_mc.gradle.conventions.configureNeoForgeRole
import org.gradle.api.Plugin
import org.gradle.api.Project

class MinecraftLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configureJvmRole()
        project.configureNeoForgeRole()
    }
}
