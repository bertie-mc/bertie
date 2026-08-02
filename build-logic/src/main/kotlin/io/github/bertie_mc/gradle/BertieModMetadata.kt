package io.github.bertie_mc.gradle

import java.io.StringReader
import java.util.Properties

data class BertieModMetadata(
    val id: String,
    val displayName: String,
    val license: String,
    val version: String,
    val group: String,
    val authors: String,
    val description: String,
    val archiveName: String,
    val neoForgeVersion: String,
    val gameTestNamespace: String,
) {
    fun templateProperties(minecraftVersion: String): Map<String, String> = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to "[1.21.1,1.22)",
        "neo_version" to neoForgeVersion,
        "neo_version_range" to "[21,)",
        "loader_version_range" to "[4,)",
        "mod_id" to id,
        "mod_name" to displayName,
        "mod_license" to license,
        "mod_version" to version,
        "mod_authors" to authors,
        "mod_description" to description,
    )

    fun clientTestTemplateProperties(minecraftVersion: String): Map<String, String> =
        templateProperties(minecraftVersion) + mapOf(
            "client_test_mod_id" to "${id}test",
            "client_test_mod_version" to "1",
            "client_test_mod_name" to "$displayName client tests",
            "client_test_mod_description" to "Test-only assertions for $displayName",
        )

    companion object {
        fun parse(text: String, defaultNeoForgeVersion: String): BertieModMetadata {
            val properties = Properties().apply {
                StringReader(text).use(::load)
            }

            fun required(name: String): String =
                properties.getProperty(name)?.takeIf(String::isNotBlank)
                    ?: error("Required mod metadata property '$name' is missing")

            val id = required("mod_id")
            return BertieModMetadata(
                id = id,
                displayName = required("mod_name"),
                license = required("mod_license"),
                version = required("mod_version"),
                group = required("mod_group_id"),
                authors = required("mod_authors"),
                description = required("mod_description"),
                archiveName = properties.getProperty("archive_name", id),
                neoForgeVersion = properties.getProperty("neo_version", defaultNeoForgeVersion),
                gameTestNamespace = properties.getProperty("game_test_namespace", id),
            )
        }
    }
}
