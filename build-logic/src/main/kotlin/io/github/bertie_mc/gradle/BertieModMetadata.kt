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
