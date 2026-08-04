package io.github.bertie_mc.gradle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinecraftRuntimeSidesTest {
    @Test
    fun `client runtime excludes only server modules`() {
        val manifest = manifest()

        assertEquals(
            setOf(MinecraftModule("example.server", "server-only")),
            manifest.modulesExcludedFrom(MinecraftArtifactSide.CLIENT),
        )
    }

    @Test
    fun `server runtime excludes only client modules`() {
        val manifest = manifest()

        assertEquals(
            setOf(MinecraftModule("example.client", "client-only")),
            manifest.modulesExcludedFrom(MinecraftArtifactSide.SERVER),
        )
    }

    @Test
    fun `projection uses the Gradle provider coordinate`() {
        val manifest = parseMinecraftArtifacts(
            """
            [mods.client-only]
            side = "client"
            maven = { module = "example.maven:client-only", version = "1" }
            modrinth = { project-id = "modrinth-id", version-id = "version-id", filename = "client-only.jar" }
            """.trimIndent(),
        )

        assertEquals(
            setOf(MinecraftModule("example.maven", "client-only")),
            manifest.modulesExcludedFrom(MinecraftArtifactSide.SERVER),
        )
    }

    private fun manifest(): MinecraftArtifactManifest = parseMinecraftArtifacts(
        """
        [mods.shared]
        maven = { module = "example.shared:shared", version = "1" }

        [mods.client-only]
        side = "client"
        maven = { module = "example.client:client-only", version = "1" }

        [mods.server-only]
        side = "server"
        maven = { module = "example.server:server-only", version = "1" }
        """.trimIndent(),
    )
}
