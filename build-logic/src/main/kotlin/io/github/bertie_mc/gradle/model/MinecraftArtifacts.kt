package io.github.bertie_mc.gradle.model

import com.electronwill.nightconfig.core.UnmodifiableConfig
import com.electronwill.nightconfig.toml.TomlParser
import java.io.StringReader

enum class MinecraftArtifactKind(
    val table: String,
    val destination: String,
    val extension: String,
) {
    MOD("mods", "mods", "jar"),
    SHADERPACK("shaderpacks", "shaderpacks", "zip"),
}

enum class MinecraftArtifactSide(
    val value: String,
) {
    BOTH("both"),
    CLIENT("client"),
    SERVER("server"),
    ;

    fun isIncludedOn(target: MinecraftArtifactSide): Boolean = this == BOTH || this == target

    companion object {
        fun parse(value: String): MinecraftArtifactSide =
            values().singleOrNull { side ->
                side.value == value
            } ?: throw IllegalArgumentException(
                "Minecraft artifact side '$value' must be one of: " +
                    values().joinToString { side -> side.value },
            )
    }
}

sealed interface MinecraftArtifactSource {
    val group: String
    val module: String
    val version: String

    fun notation(extension: String? = null): String = "$group:$module:$version" + extension?.let { "@$it" }.orEmpty()
}

data class MavenArtifactSource(
    private val coordinate: String,
    override val version: String,
) : MinecraftArtifactSource {
    override val group: String = coordinate.substringBefore(':')
    override val module: String = coordinate.substringAfter(':')
}

data class ModrinthArtifactSource(
    val projectId: String,
    val versionId: String,
    val filename: String,
) : MinecraftArtifactSource {
    override val group: String = "maven.modrinth"
    override val module: String = projectId
    override val version: String = versionId
}

data class CurseForgeArtifactSource(
    val slug: String,
    val projectId: Long,
    val fileId: Long,
) : MinecraftArtifactSource {
    override val group: String = "curse.maven"
    override val module: String = "$slug-$projectId"
    override val version: String = fileId.toString()
}

data class MinecraftArtifact(
    val id: String,
    val kind: MinecraftArtifactKind,
    val side: MinecraftArtifactSide,
    val maven: MavenArtifactSource?,
    val modrinth: ModrinthArtifactSource?,
    val curseForge: CurseForgeArtifactSource?,
) {
    val gradleSource: MinecraftArtifactSource
        get() =
            maven ?: modrinth ?: curseForge
                ?: error("Minecraft artifact '$id' has no Gradle source")

    val packwizSource: MinecraftArtifactSource
        get() =
            modrinth ?: curseForge
                ?: error("Minecraft artifact '$id' has no Modrinth or CurseForge source")

    val catalogAlias: String
        get() =
            id.split('-', '_').let { words ->
                words.first() +
                    words.drop(1).joinToString("") { word ->
                        word.replaceFirstChar(Char::uppercaseChar)
                    }
            }
}

data class MinecraftArtifactManifest(
    val mods: List<MinecraftArtifact>,
    val shaderpacks: List<MinecraftArtifact>,
) {
    val all: List<MinecraftArtifact>
        get() = mods + shaderpacks
}

fun parseMinecraftArtifacts(contents: String): MinecraftArtifactManifest {
    val root = TomlParser().parse(StringReader(contents))
    val mods = root.artifacts(MinecraftArtifactKind.MOD)
    val shaderpacks = root.artifacts(MinecraftArtifactKind.SHADERPACK)
    val aliases =
        mods
            .groupBy(MinecraftArtifact::catalogAlias)
            .filterValues { artifacts -> artifacts.size > 1 }
    require(aliases.isEmpty()) {
        "Minecraft artifact IDs produce colliding Gradle aliases: " +
            aliases.values.flatten().joinToString { artifact -> artifact.id }
    }
    return MinecraftArtifactManifest(mods, shaderpacks)
}

private fun UnmodifiableConfig.artifacts(kind: MinecraftArtifactKind): List<MinecraftArtifact> {
    val section = config(kind.table) ?: return emptyList()
    return section
        .entrySet()
        .map { entry ->
            val id = entry.key
            val artifact =
                entry.getRawValue<Any?>() as? UnmodifiableConfig
                    ?: error("Minecraft artifact '$id' must be a TOML table")
            MinecraftArtifact(
                id = id,
                kind = kind,
                side =
                    artifact
                        .optionalString("side")
                        ?.let(MinecraftArtifactSide::parse)
                        ?: MinecraftArtifactSide.BOTH,
                maven =
                    artifact.config("maven")?.let { source ->
                        MavenArtifactSource(
                            coordinate = source.string("module"),
                            version = source.string("version"),
                        )
                    },
                modrinth =
                    artifact.config("modrinth")?.let { source ->
                        ModrinthArtifactSource(
                            projectId = source.string("project-id"),
                            versionId = source.string("version-id"),
                            filename = source.string("filename"),
                        )
                    },
                curseForge =
                    artifact.config("curseforge")?.let { source ->
                        CurseForgeArtifactSource(
                            slug = source.string("slug"),
                            projectId = source.long("project-id"),
                            fileId = source.long("file-id"),
                        )
                    },
            )
        }.sortedBy(MinecraftArtifact::id)
}

private fun UnmodifiableConfig.config(name: String): UnmodifiableConfig? = get<Any?>(name) as? UnmodifiableConfig

private fun UnmodifiableConfig.string(name: String): String =
    get<Any?>(name) as? String
        ?: error("Minecraft artifact field '$name' must be a string")

private fun UnmodifiableConfig.optionalString(name: String): String? {
    val value = get<Any?>(name) ?: return null
    return value as? String
        ?: error("Minecraft artifact field '$name' must be a string")
}

private fun UnmodifiableConfig.long(name: String): Long =
    (get<Any?>(name) as? Number)?.toLong()
        ?: error("Minecraft artifact field '$name' must be an integer")
