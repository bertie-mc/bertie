package io.github.bertie_mc.gradle.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinecraftArtifactsTest {
    @Test
    fun `lock preserves selected representations and transitive evidence`() {
        val manifest = parseLock(lock(), components())

        val content = manifest.selectedArtifact("content")
        assertEquals(MinecraftArtifactKind.DATAPACK, content.kind)
        assertEquals(
            listOf(MinecraftArtifactKind.DATAPACK, MinecraftArtifactKind.RESOURCEPACK),
            content.installationKinds,
        )
        assertInstanceOf(ModrinthArtifactSource::class.java, content.source)
        assertEquals("content.zip", content.filename)

        val mod = manifest.selectedArtifact("content", requireMod = true)
        assertEquals(MinecraftArtifactKind.MOD, mod.kind)
        assertInstanceOf(CurseForgeArtifactSource::class.java, mod.source)

        val reachable = manifest.reachableArtifacts(listOf("content")).map { it.id }
        assertEquals(listOf("content-data", "paxi"), reachable)
        assertEquals(
            listOf("content-mod"),
            manifest.reachableArtifacts(listOf("content"), requireMod = true).map { it.id },
        )
        assertEquals("jei", mod.optional.single().missingModId)
        assertEquals("[19,)", mod.optional.single().versionRange)
        assertEquals(
            MinecraftComponentRelationship(
                source = "content",
                target = "paxi",
                kind = MinecraftComponentRelationshipKind.OPTIONAL_ADDON_FOR,
            ),
            manifest.relationships.single(),
        )
    }

    @Test
    fun `component requires at least one distribution`() {
        val missingDistribution =
            assertThrows(IllegalArgumentException::class.java) {
                parseComponent("example", "# No distributions.")
            }
        assertTrue(missingDistribution.message.orEmpty().contains("distribution"))
    }

    @Test
    fun `lock rejects unresolved required dependency`() {
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                parseLock(
                    lock().replace(
                        "artifact = \"modrinth:paxi:paxi-version\"",
                        "artifact = \"modrinth:missing:version\"",
                    ),
                    components(),
                )
            }

        assertTrue(failure.message.orEmpty().contains("unresolved required edge"))
    }

    @Test
    fun `lower camel aliases keep prefix-related component ids independent`() {
        assertEquals(
            listOf("accessories", "accessoriesCompatLayer"),
            listOf("accessories", "accessories-compat-layer")
                .map { id -> parseComponent(id, component()).catalogAlias },
        )
    }

    @Test
    fun `redistribution assignments reuse evidence across exports`() {
        val policy =
            parseRedistributionPolicy(
                """
                strict = true

                [evidence.additional-context]
                allowed = true
                text = "Project license"

                [evidence.shared-permission]
                allowed = false
                text = "The author permits both files to be redistributed."

                [exports.modrinth.artifacts."curseforge:12:34"]
                name = "first"
                evidence = ["shared-permission"]

                [exports.curseforge.artifacts."modrinth:project:version"]
                name = "second"
                component = "second"
                evidence = ["additional-context", "shared-permission"]
                """.trimIndent(),
            )

        assertTrue(policy.strict)
        assertEquals(listOf("additional-context", "shared-permission"), policy.evidence.map { it.id })
        val modrinthAssignment = policy.artifactsFor("modrinth").single()
        assertEquals(
            RedistributionArtifactPolicy(
                identity = "curseforge:12:34",
                name = "first",
                component = null,
                evidence = listOf("shared-permission"),
            ),
            modrinthAssignment,
        )
        val curseForgeAssignment = policy.artifactsFor("curseforge").single()
        assertEquals(
            listOf("additional-context", "shared-permission"),
            curseForgeAssignment.evidence,
        )
        assertEquals("second", curseForgeAssignment.component)
        assertEquals(false, policy.evidence.single { it.id == "shared-permission" }.allowed)
    }

    @Test
    fun `redistribution evidence rejects non-exact artifact identities`() {
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                parseRedistributionPolicy(
                    """
                    strict = false

                    [evidence.wildcard]
                    allowed = true
                    text = "Too broad"

                    [exports.modrinth.artifacts."modrinth:project:*"]
                    name = "wildcard"
                    evidence = ["wildcard"]
                    """.trimIndent(),
                )
            }

        assertTrue(failure.message.orEmpty().contains("invalid immutable identity"))
    }

    @Test
    fun `redistribution evidence requires an explicit allowed decision`() {
        val failure =
            assertThrows(IllegalStateException::class.java) {
                parseRedistributionPolicy(
                    """
                    strict = false

                    [evidence.permission]
                    text = "Permission"

                    [exports.modrinth.artifacts."modrinth:project:version"]
                    name = "project"
                    evidence = ["permission"]
                    """.trimIndent(),
                )
            }

        assertTrue(failure.message.orEmpty().contains("field 'allowed' must be a boolean"))
    }

    @Test
    fun `redistribution assignment rejects unknown evidence`() {
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                parseRedistributionPolicy(
                    """
                    strict = false

                    [exports.curseforge.artifacts."modrinth:project:version"]
                    name = "project"
                    evidence = ["missing"]
                    """.trimIndent(),
                )
            }

        assertTrue(failure.message.orEmpty().contains("unknown evidence 'missing'"))
    }

    private fun components(): Map<String, MinecraftComponent> =
        mapOf(
            "content" to parseComponent("content", component()),
            "paxi" to parseComponent("paxi", component()),
        )

    private fun component(): String =
        """
        [distributions.fixture]
        provider = "modrinth"
        kind = "mod"
        """.trimIndent()

    private fun lock(): String =
        """
        profile = "release-modrinth"
        inputs-hash = "fixture"
        relationships = [{ source = "content", target = "paxi", kind = "optional-addon-for" }]

        [components.content]
        any = "modrinth:content:data-version"
        mod = "curseforge:10:20"

        [components.paxi]
        any = "modrinth:paxi:paxi-version"
        mod = "modrinth:paxi:paxi-version"

        [artifacts."modrinth:content:data-version"]
        name = "content-data"
        component = "content"
        provider = "modrinth"
        kind = "datapack"
        side = "both"
        group = "maven.modrinth"
        module = "content"
        version = "data-version"
        filename = "content.zip"
        project-id = "content"
        version-id = "data-version"
        additional-kinds = ["resourcepack"]
        provides = []
        bundled-provides = []
        required = [{ artifact = "modrinth:paxi:paxi-version", mod-id = "paxi", side = "both", origin = "profile:release-modrinth:native-packs.datapack" }]
        optional = []
        incompatible = []
        bundled = []
        integrations = []

        [artifacts."curseforge:10:20"]
        name = "content-mod"
        component = "content"
        provider = "curseforge"
        kind = "mod"
        side = "both"
        group = "curse.maven"
        module = "content-10"
        version = "20"
        filename = "content.jar"
        slug = "content"
        project-id = 10
        file-id = 20
        provides = ["content"]
        bundled-provides = []
        required = []
        optional = [{ missing = "jei", mod-id = "jei", version-range = "[19,)", side = "both", origin = "archive" }]
        incompatible = []
        bundled = []
        integrations = []

        [artifacts."modrinth:paxi:paxi-version"]
        name = "paxi"
        component = "paxi"
        provider = "modrinth"
        kind = "mod"
        side = "both"
        group = "maven.modrinth"
        module = "paxi"
        version = "paxi-version"
        filename = "paxi.jar"
        project-id = "paxi"
        version-id = "paxi-version"
        provides = ["paxi"]
        bundled-provides = []
        required = []
        optional = []
        incompatible = []
        bundled = []
        integrations = []
        """.trimIndent()
}
