package io.github.bertie_mc.gradle.model

import com.electronwill.nightconfig.core.UnmodifiableConfig
import com.electronwill.nightconfig.toml.TomlParser
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File
import java.io.Serializable
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest

private const val LOCK_POLICY = "bertie-deps-lock-v1"

enum class MinecraftArtifactKind(
    val value: String,
    val destination: String,
    val extension: String,
) {
    MOD("mod", "mods", "jar"),
    DATAPACK("datapack", "datapacks", "zip"),
    RESOURCEPACK("resourcepack", "config/paxi/resourcepacks", "zip"),
    SHADERPACK("shaderpack", "shaderpacks", "zip"),
    ;

    companion object {
        fun parse(value: String): MinecraftArtifactKind =
            values().singleOrNull { it.value == value }
                ?: throw IllegalArgumentException(
                    "Minecraft artifact kind '$value' must be one of: " +
                        values().joinToString { it.value },
                )
    }
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
            values().singleOrNull { side -> side.value == value }
                ?: throw IllegalArgumentException(
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
    override val group: String,
    override val module: String,
    override val version: String,
) : MinecraftArtifactSource

data class ModrinthArtifactSource(
    val projectId: String,
    val versionId: String,
    override val group: String = "maven.modrinth",
    override val module: String = projectId,
    override val version: String = versionId,
) : MinecraftArtifactSource

data class CurseForgeArtifactSource(
    val slug: String,
    val projectId: Long,
    val fileId: Long,
    override val group: String = "curse.maven",
    override val module: String = "$slug-$projectId",
    override val version: String = fileId.toString(),
) : MinecraftArtifactSource

data class MinecraftDependencyEdge(
    val artifact: String?,
    val missingModId: String?,
    val modId: String?,
    val versionRange: String?,
    val side: MinecraftArtifactSide,
    val origin: String,
)

data class MinecraftArtifact(
    val identity: String,
    val id: String,
    val component: String?,
    val kind: MinecraftArtifactKind,
    val side: MinecraftArtifactSide,
    val source: MinecraftArtifactSource,
    val filename: String,
    val provides: List<String>,
    val bundledProvides: List<String>,
    val required: List<MinecraftDependencyEdge>,
    val optional: List<MinecraftDependencyEdge>,
    val additionalKinds: List<MinecraftArtifactKind> = emptyList(),
) {
    init {
        require(additionalKinds.distinct().size == additionalKinds.size) {
            "Minecraft artifact '$identity' repeats an additional installation kind"
        }
        require(kind !in additionalKinds && MinecraftArtifactKind.MOD !in additionalKinds) {
            "Minecraft artifact '$identity' has invalid additional installation kinds"
        }
    }

    val installationKinds: List<MinecraftArtifactKind>
        get() = listOf(kind) + additionalKinds

    val gradleSource: MinecraftArtifactSource
        get() = source

    val packwizSource: MinecraftArtifactSource
        get() = source

    fun filename(
        @Suppress("UNUSED_PARAMETER") source: MinecraftArtifactSource,
    ): String = filename

    fun notation(source: MinecraftArtifactSource = this.source): String {
        val extension = source.fileExtension ?: kind.extension
        return source.notation(extension.takeUnless { it == "jar" })
    }
}

data class MinecraftComponentSelection(
    val any: String,
    val mod: String?,
)

enum class MinecraftComponentRelationshipKind(
    val value: String,
) {
    OPTIONAL_ADDON_FOR("optional-addon-for"),
    ;

    companion object {
        fun parse(value: String): MinecraftComponentRelationshipKind =
            values().singleOrNull { kind -> kind.value == value }
                ?: throw IllegalArgumentException(
                    "Minecraft component relationship kind '$value' must be one of: " +
                        values().joinToString { kind -> kind.value },
                )
    }
}

data class MinecraftComponentRelationship(
    val source: String,
    val target: String,
    val kind: MinecraftComponentRelationshipKind,
)

data class MinecraftComponent(
    val id: String,
) {
    val catalogAlias: String
        get() = id.catalogAlias()
}

data class MinecraftArtifactManifest(
    val profile: String,
    val inputHash: String,
    val components: Map<String, MinecraftComponent>,
    val selections: Map<String, MinecraftComponentSelection>,
    val artifacts: Map<String, MinecraftArtifact>,
    val relationships: List<MinecraftComponentRelationship> = emptyList(),
) {
    val all: List<MinecraftArtifact>
        get() = artifacts.values.sortedBy(MinecraftArtifact::id)
    val mods: List<MinecraftArtifact>
        get() = all.filter { it.kind == MinecraftArtifactKind.MOD }
    val datapacks: List<MinecraftArtifact>
        get() = all.filter { it.kind == MinecraftArtifactKind.DATAPACK }
    val resourcepacks: List<MinecraftArtifact>
        get() = all.filter { it.kind == MinecraftArtifactKind.RESOURCEPACK }
    val shaderpacks: List<MinecraftArtifact>
        get() = all.filter { it.kind == MinecraftArtifactKind.SHADERPACK }

    fun selectedArtifact(
        componentId: String,
        requireMod: Boolean = false,
    ): MinecraftArtifact {
        val selection = selections[componentId] ?: error("Unknown Minecraft component '$componentId'")
        val identity =
            if (requireMod) {
                selection.mod
                    ?: error("Minecraft component '$componentId' has no mod distribution in profile '$profile'")
            } else {
                selection.any
            }
        return artifacts[identity]
            ?: error("Minecraft component '$componentId' selects missing artifact '$identity'")
    }

    fun reachableArtifacts(
        rootComponents: Collection<String>,
        requireMod: Boolean = false,
    ): List<MinecraftArtifact> {
        val pending = ArrayDeque(rootComponents.map { selectedArtifact(it, requireMod).identity })
        val reached = linkedSetOf<String>()
        while (pending.isNotEmpty()) {
            val identity = pending.removeFirst()
            if (!reached.add(identity)) continue
            val artifact = artifacts[identity] ?: error("Locked artifact '$identity' is missing")
            artifact.required.mapNotNullTo(pending) { it.artifact }
        }
        return reached.map { identity -> artifacts.getValue(identity) }
    }
}

data class RedistributionEvidence(
    @get:Input val id: String,
    @get:Input val allowed: Boolean,
    @get:Input val text: String,
) : Serializable

data class RedistributionArtifactPolicy(
    @get:Input val identity: String,
    @get:Input val name: String,
    @get:Input
    @get:Optional
    val component: String?,
    @get:Input val evidence: List<String>,
) : Serializable

data class RedistributionPolicy(
    val strict: Boolean,
    val evidence: List<RedistributionEvidence>,
    val exports: Map<String, List<RedistributionArtifactPolicy>>,
) {
    fun artifactsFor(export: String): List<RedistributionArtifactPolicy> = exports[export].orEmpty()
}

fun loadRedistributionPolicy(rootDirectory: File): RedistributionPolicy {
    val path = rootDirectory.resolve("deps/redistribution.toml")
    require(path.isFile) { "Redistribution policy not found: ${path.absolutePath}" }
    return parseRedistributionPolicy(path.readText(StandardCharsets.UTF_8))
}

internal fun parseRedistributionPolicy(contents: String): RedistributionPolicy {
    val root = TomlParser().parse(StringReader(contents))
    val strict =
        root.get<Any?>("strict") as? Boolean
            ?: error("Redistribution policy field 'strict' must be a boolean")
    val evidence =
        root
            .config("evidence")
            ?.entrySet()
            ?.sortedBy { entry -> entry.key }
            ?.map { entry ->
                val id = entry.key
                val owner = "Redistribution evidence '$id'"
                val record =
                    entry.getRawValue<Any?>() as? UnmodifiableConfig
                        ?: error("$owner must be a table")
                val allowed = record.requiredBoolean("allowed", owner)
                val text = record.requiredString("text", owner).trim()
                require(text.isNotBlank()) { "$owner field 'text' must not be blank" }
                RedistributionEvidence(id, allowed, text)
            }.orEmpty()
    val evidenceById = evidence.associateBy(RedistributionEvidence::id)
    val exports =
        root
            .config("exports")
            ?.entrySet()
            ?.sortedBy { entry -> entry.key }
            ?.associate { entry ->
                val export = entry.key
                require(export in setOf("modrinth", "curseforge")) {
                    "Unsupported redistribution export '$export'"
                }
                val owner = "Redistribution export '$export'"
                val record =
                    entry.getRawValue<Any?>() as? UnmodifiableConfig
                        ?: error("$owner must be a table")
                val artifacts =
                    record
                        .requireConfig("artifacts", owner)
                        .entrySet()
                        .sortedBy { artifact -> artifact.key }
                        .map { artifact ->
                            val identity = artifact.key
                            val artifactOwner = "$owner artifact '$identity'"
                            require(isImmutableDistributionIdentity(identity)) {
                                "$artifactOwner has an invalid immutable identity"
                            }
                            val artifactRecord =
                                artifact.getRawValue<Any?>() as? UnmodifiableConfig
                                    ?: error("$artifactOwner must be a table")
                            val name = artifactRecord.requiredString("name", artifactOwner)
                            require(name.isNotBlank()) { "$artifactOwner field 'name' must not be blank" }
                            val component = artifactRecord.optionalString("component", artifactOwner)
                            require(component == null || component.isNotBlank()) {
                                "$artifactOwner field 'component' must not be blank"
                            }
                            val references = artifactRecord.stringList("evidence", artifactOwner)
                            require(references.isNotEmpty()) {
                                "$artifactOwner must reference at least one evidence record"
                            }
                            require(references.distinct().size == references.size) {
                                "$artifactOwner repeats an evidence reference"
                            }
                            references.forEach { reference ->
                                require(reference in evidenceById) {
                                    "$artifactOwner references unknown evidence '$reference'"
                                }
                            }
                            RedistributionArtifactPolicy(identity, name, component, references)
                        }
                export to artifacts
            }.orEmpty()
    val referencedEvidence =
        exports.values.flatten().flatMapTo(mutableSetOf(), RedistributionArtifactPolicy::evidence)
    val unusedEvidence = evidenceById.keys - referencedEvidence
    require(unusedEvidence.isEmpty()) {
        "Unused redistribution evidence: ${unusedEvidence.sorted().joinToString()}"
    }
    return RedistributionPolicy(strict, evidence, exports)
}

private fun isImmutableDistributionIdentity(identity: String): Boolean {
    if (identity.any { character -> character in "*?[]" }) return false
    val parts = identity.split(':')
    return when (parts.firstOrNull()) {
        "modrinth" -> {
            parts.size == 3 && parts.drop(1).all(String::isNotBlank)
        }

        "curseforge" -> {
            parts.size == 3 &&
                parts.drop(1).all { part -> part.toLongOrNull()?.let { it > 0 } == true }
        }

        else -> {
            false
        }
    }
}

fun loadMinecraftArtifacts(
    rootDirectory: File,
    profile: String,
): MinecraftArtifactManifest {
    val deps = rootDirectory.resolve("deps")
    val componentsDirectory = deps.resolve("components")
    val componentFiles =
        componentsDirectory
            .listFiles { file -> file.isFile && file.extension == "toml" }
            ?.sortedBy(File::getName)
            ?: emptyList()
    require(componentFiles.isNotEmpty()) {
        "No Minecraft component manifests found under ${componentsDirectory.absolutePath}"
    }
    val components =
        componentFiles.associate { file ->
            val id = file.nameWithoutExtension
            id to parseComponent(id, file.readText(StandardCharsets.UTF_8))
        }
    val aliases =
        components.values
            .groupBy(MinecraftComponent::catalogAlias)
            .filterValues { values -> values.size > 1 }
    require(aliases.isEmpty()) {
        "Minecraft component IDs produce colliding Gradle aliases: " +
            aliases.values.flatten().joinToString { it.id }
    }

    val lockFile = deps.resolve("locks/$profile.lock.toml")
    require(lockFile.isFile) { "Minecraft dependency lock not found: ${lockFile.absolutePath}" }
    val parsed = parseLock(lockFile.readText(StandardCharsets.UTF_8), components)
    require(parsed.profile == profile) {
        "Minecraft dependency lock ${lockFile.absolutePath} declares profile '${parsed.profile}', expected '$profile'"
    }
    val expectedHash = dependencyInputsHash(rootDirectory, profile)
    require(parsed.inputHash == expectedHash) {
        "Minecraft dependency lock ${lockFile.absolutePath} is stale; run 'bertie-ci deps-lock'"
    }
    require(parsed.selections.keys == components.keys) {
        val missing = components.keys - parsed.selections.keys
        val extra = parsed.selections.keys - components.keys
        "Minecraft dependency lock component mismatch; missing=$missing, extra=$extra"
    }
    return parsed
}

internal fun parseComponent(
    id: String,
    contents: String,
): MinecraftComponent {
    val root = TomlParser().parse(StringReader(contents))
    val distributions = root.config("distributions")
    require(distributions != null && distributions.entrySet().isNotEmpty()) {
        "Minecraft component '$id' must declare at least one distribution"
    }
    return MinecraftComponent(id)
}

internal fun parseLock(
    contents: String,
    components: Map<String, MinecraftComponent>,
): MinecraftArtifactManifest {
    val root = TomlParser().parse(StringReader(contents))
    val profile = root.requiredString("profile", "Minecraft dependency lock")
    val inputHash = root.requiredString("inputs-hash", "Minecraft dependency lock")
    val relationships =
        root.list("relationships", "Minecraft dependency lock").mapIndexed { index, value ->
            val owner = "Minecraft dependency lock relationships[$index]"
            val relationship = value as? UnmodifiableConfig ?: error("$owner must be a table")
            MinecraftComponentRelationship(
                source = relationship.requiredString("source", owner),
                target = relationship.requiredString("target", owner),
                kind =
                    MinecraftComponentRelationshipKind.parse(
                        relationship.requiredString("kind", owner),
                    ),
            )
        }
    val selections =
        root
            .requireConfig("components", "Minecraft dependency lock")
            .entrySet()
            .associate { entry ->
                val id = entry.key
                val config =
                    entry.getRawValue<Any?>() as? UnmodifiableConfig
                        ?: error("Minecraft dependency lock component '$id' must be a table")
                id to
                    MinecraftComponentSelection(
                        any = config.requiredString("any", "Minecraft component '$id' selection"),
                        mod = config.optionalString("mod", "Minecraft component '$id' selection"),
                    )
            }
    val artifacts =
        root
            .requireConfig("artifacts", "Minecraft dependency lock")
            .entrySet()
            .associate { entry ->
                val identity = entry.key
                val config =
                    entry.getRawValue<Any?>() as? UnmodifiableConfig
                        ?: error("Minecraft locked artifact '$identity' must be a table")
                identity to config.artifact(identity)
            }
    selections.forEach { (id, selection) ->
        require(id in components) { "Minecraft dependency lock selects unknown component '$id'" }
        require(selection.any in artifacts) {
            "Minecraft component '$id' selects missing artifact '${selection.any}'"
        }
        selection.mod?.let { identity ->
            val artifact =
                artifacts[identity]
                    ?: error("Minecraft component '$id' selects missing mod artifact '$identity'")
            require(artifact.kind == MinecraftArtifactKind.MOD) {
                "Minecraft component '$id' mod selection '$identity' is ${artifact.kind.value}"
            }
        }
    }
    artifacts.values.forEach { artifact ->
        artifact.required.forEach { edge ->
            require(edge.artifact in artifacts) {
                "Minecraft artifact '${artifact.identity}' has unresolved required edge '${edge.artifact}'"
            }
        }
    }
    relationships.forEach { relationship ->
        require(relationship.source in components) {
            "Minecraft component relationship names unknown source '${relationship.source}'"
        }
        require(relationship.target in components) {
            "Minecraft component relationship names unknown target '${relationship.target}'"
        }
        require(relationship.source != relationship.target) {
            "Minecraft component relationship cannot refer to itself: '${relationship.source}'"
        }
    }
    return MinecraftArtifactManifest(
        profile,
        inputHash,
        components,
        selections,
        artifacts,
        relationships,
    )
}

private fun UnmodifiableConfig.artifact(identity: String): MinecraftArtifact {
    val provider = requiredString("provider", "Minecraft artifact '$identity'")
    val source =
        when (provider) {
            "maven" -> {
                MavenArtifactSource(
                    group = requiredString("group", "Minecraft artifact '$identity'"),
                    module = requiredString("module", "Minecraft artifact '$identity'"),
                    version = requiredString("version", "Minecraft artifact '$identity'"),
                )
            }

            "modrinth" -> {
                ModrinthArtifactSource(
                    projectId = requiredString("project-id", "Minecraft artifact '$identity'"),
                    versionId = requiredString("version-id", "Minecraft artifact '$identity'"),
                )
            }

            "curseforge" -> {
                CurseForgeArtifactSource(
                    slug = requiredString("slug", "Minecraft artifact '$identity'"),
                    projectId = requiredLong("project-id", "Minecraft artifact '$identity'"),
                    fileId = requiredLong("file-id", "Minecraft artifact '$identity'"),
                )
            }

            else -> {
                error("Minecraft artifact '$identity' has unsupported provider '$provider'")
            }
        }
    return MinecraftArtifact(
        identity = identity,
        id = optionalString("name", "Minecraft artifact '$identity'") ?: safeArtifactId(identity),
        component = optionalString("component", "Minecraft artifact '$identity'"),
        kind = MinecraftArtifactKind.parse(requiredString("kind", "Minecraft artifact '$identity'")),
        side = MinecraftArtifactSide.parse(requiredString("side", "Minecraft artifact '$identity'")),
        source = source,
        filename = requiredString("filename", "Minecraft artifact '$identity'"),
        provides = stringList("provides", "Minecraft artifact '$identity'"),
        bundledProvides = stringList("bundled-provides", "Minecraft artifact '$identity'"),
        required = edgeList("required", "Minecraft artifact '$identity'"),
        optional = edgeList("optional", "Minecraft artifact '$identity'"),
        additionalKinds =
            optionalStringList("additional-kinds", "Minecraft artifact '$identity'")
                .map(MinecraftArtifactKind::parse),
    )
}

private fun UnmodifiableConfig.edgeList(
    field: String,
    owner: String,
): List<MinecraftDependencyEdge> =
    list(field, owner).mapIndexed { index, value ->
        val edge = value as? UnmodifiableConfig ?: error("$owner $field[$index] must be a table")
        MinecraftDependencyEdge(
            artifact = edge.optionalString("artifact", "$owner $field[$index]"),
            missingModId = edge.optionalString("missing", "$owner $field[$index]"),
            modId = edge.optionalString("mod-id", "$owner $field[$index]"),
            versionRange = edge.optionalString("version-range", "$owner $field[$index]"),
            side =
                edge
                    .optionalString("side", "$owner $field[$index]")
                    ?.let(MinecraftArtifactSide::parse)
                    ?: MinecraftArtifactSide.BOTH,
            origin = edge.requiredString("origin", "$owner $field[$index]"),
        )
    }

internal fun dependencyInputsHash(
    rootDirectory: File,
    profile: String,
): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(LOCK_POLICY.toByteArray(StandardCharsets.UTF_8))
    digest.update(0.toByte())
    val deps = rootDirectory.resolve("deps")
    val paths =
        buildList {
            add(deps.resolve("platform.toml"))
            addAll(
                deps
                    .resolve("components")
                    .listFiles { file -> file.isFile && file.extension == "toml" }
                    ?.sortedBy(File::getName)
                    ?: emptyList(),
            )
            add(deps.resolve("profiles/$profile.toml"))
        }
    paths.forEach { file ->
        require(file.isFile) { "Minecraft dependency input not found: ${file.absolutePath}" }
        val relative =
            rootDirectory
                .toPath()
                .relativize(file.toPath())
                .toString()
                .replace(File.separatorChar, '/')
        digest.update(relative.toByteArray(StandardCharsets.UTF_8))
        digest.update(0.toByte())
        Files.newInputStream(file.toPath()).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.update(0.toByte())
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

private fun String.catalogAlias(): String =
    split('-', '_').let { words ->
        words.first() + words.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    }

private fun safeArtifactId(identity: String): String =
    identity
        .lowercase()
        .replace(Regex("[^a-z0-9_-]+"), "-")
        .trim('-')
        .take(80)

private fun UnmodifiableConfig.config(name: String): UnmodifiableConfig? = get<Any?>(name) as? UnmodifiableConfig

private fun UnmodifiableConfig.requireConfig(
    name: String,
    owner: String,
): UnmodifiableConfig = config(name) ?: error("$owner field '$name' must be a table")

private fun UnmodifiableConfig.requiredString(
    name: String,
    owner: String,
): String =
    get<Any?>(name) as? String
        ?: error("$owner field '$name' must be a string")

private fun UnmodifiableConfig.optionalString(
    name: String,
    owner: String,
): String? {
    val value = get<Any?>(name) ?: return null
    return value as? String ?: error("$owner field '$name' must be a string")
}

private fun UnmodifiableConfig.requiredLong(
    name: String,
    owner: String,
): Long =
    (get<Any?>(name) as? Number)?.toLong()
        ?: error("$owner field '$name' must be an integer")

private fun UnmodifiableConfig.requiredBoolean(
    name: String,
    owner: String,
): Boolean =
    get<Any?>(name) as? Boolean
        ?: error("$owner field '$name' must be a boolean")

private fun UnmodifiableConfig.list(
    name: String,
    owner: String,
): List<Any?> =
    get<Any?>(name) as? List<Any?>
        ?: error("$owner field '$name' must be an array")

private fun UnmodifiableConfig.stringList(
    name: String,
    owner: String,
): List<String> =
    list(name, owner).mapIndexed { index, value ->
        value as? String ?: error("$owner $name[$index] must be a string")
    }

private fun UnmodifiableConfig.optionalStringList(
    name: String,
    owner: String,
): List<String> {
    if (get<Any?>(name) == null) return emptyList()
    return stringList(name, owner)
}

val MinecraftArtifactSource.fileExtension: String?
    get() = null
