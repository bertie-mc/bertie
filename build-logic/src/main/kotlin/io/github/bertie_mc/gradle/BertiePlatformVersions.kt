package io.github.bertie_mc.gradle

import org.gradle.api.artifacts.VersionCatalog

/** Exact platform versions and the NeoForge metadata ranges derived from them. */
data class BertiePlatformVersions(
    val minecraft: String,
    val neoForge: String,
    val javaFmlLoader: String,
) {
    val minecraftVersionRange: String
    val neoForgeVersionRange: String
    val javaFmlLoaderVersionRange: String

    init {
        val minecraftParts = numericVersion("Minecraft", minecraft, minimumParts = 2)
        val neoForgeParts = numericVersion("NeoForge", neoForge, minimumParts = 2)
        val loaderParts = numericVersion("JavaFML loader", javaFmlLoader, minimumParts = 1)
        require(loaderParts.size == 1) {
            "JavaFML loader version must be one numeric major version: $javaFmlLoader"
        }

        minecraftVersionRange =
            "[$minecraft,${minecraftParts[0]}.${minecraftParts[1] + 1})"
        neoForgeVersionRange = "[${neoForgeParts[0]}.${neoForgeParts[1]},)"
        javaFmlLoaderVersionRange = "[${loaderParts.single()},)"
    }

    fun templateProperties(): Map<String, String> = mapOf(
        "minecraft_version" to minecraft,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoForge,
        "neo_version_range" to neoForgeVersionRange,
        "loader_version" to javaFmlLoader,
        "loader_version_range" to javaFmlLoaderVersionRange,
    )
}

fun VersionCatalog.bertiePlatformVersions(): BertiePlatformVersions = BertiePlatformVersions(
    minecraft = requiredVersion("minecraft"),
    neoForge = requiredVersion("neoforge"),
    javaFmlLoader = requiredVersion("javafml-loader"),
)

private fun VersionCatalog.requiredVersion(name: String): String =
    findVersion(name).orElseThrow {
        IllegalStateException("Version '$name' is missing from the root version catalog")
    }.requiredVersion

private fun numericVersion(
    label: String,
    version: String,
    minimumParts: Int,
): List<Int> {
    val parts = version.split('.').map(String::toIntOrNull)
    require(parts.size >= minimumParts && parts.all { it != null }) {
        "$label version must be an exact numeric version: $version"
    }
    return parts.filterNotNull()
}
