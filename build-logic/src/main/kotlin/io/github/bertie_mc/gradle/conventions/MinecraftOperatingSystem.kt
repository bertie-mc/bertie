package io.github.bertie_mc.gradle.conventions

import net.neoforged.minecraftdependencies.OperatingSystem
import org.gradle.api.Project

private const val OPERATING_SYSTEM_OVERRIDE = "bertie.minecraft-operating-system"

/** Allows another platform's Minecraft variants to be checked without running on that platform. */
internal fun Project.configureMinecraftOperatingSystemOverride() {
    val operatingSystem = providers.gradleProperty(OPERATING_SYSTEM_OVERRIDE).orNull ?: return
    require(
        operatingSystem in
            setOf(OperatingSystem.LINUX, OperatingSystem.MACOSX, OperatingSystem.WINDOWS),
    ) {
        "Invalid -P$OPERATING_SYSTEM_OVERRIDE=$operatingSystem; " +
            "expected linux, osx, or windows"
    }
    configurations.configureEach {
        if (attributes.getAttribute(OperatingSystem.ATTRIBUTE) == null) {
            attributes.attribute(
                OperatingSystem.ATTRIBUTE,
                objects.named(OperatingSystem::class.java, operatingSystem),
            )
        }
    }
}
