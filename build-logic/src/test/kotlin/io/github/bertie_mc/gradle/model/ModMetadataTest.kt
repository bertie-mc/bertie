package io.github.bertie_mc.gradle.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ModMetadataTest {
    @Test
    fun `derives metadata ranges from exact platform versions`() {
        val platform = PlatformVersions(
            minecraft = "1.20.4",
            neoForge = "20.4.237",
            javaFmlLoader = "3",
        )

        assertEquals("[1.20.4,1.21)", platform.minecraftVersionRange)
        assertEquals("[20.4,)", platform.neoForgeVersionRange)
        assertEquals("[3,)", platform.javaFmlLoaderVersionRange)
        assertEquals(
            mapOf(
                "minecraft_version" to "1.20.4",
                "minecraft_version_range" to "[1.20.4,1.21)",
                "neo_version" to "20.4.237",
                "neo_version_range" to "[20.4,)",
                "loader_version" to "3",
                "loader_version_range" to "[3,)",
            ),
            platform.templateProperties(),
        )
    }

    @Test
    fun `mod metadata uses the shared platform properties`() {
        val platform = PlatformVersions(
            minecraft = "1.20.4",
            neoForge = "20.4.237",
            javaFmlLoader = "3",
        )
        val metadata = ModMetadata(
            id = "example",
            displayName = "Example",
            license = "Unlicense",
            version = "1.0.0",
            group = "example",
            authors = "Example",
            description = "Example mod",
            archiveName = "example",
            gameTestNamespace = "example",
        )

        val properties = metadata.templateProperties(platform)

        assertEquals("[1.20.4,1.21)", properties["minecraft_version_range"])
        assertEquals("[20.4,)", properties["neo_version_range"])
        assertEquals("[3,)", properties["loader_version_range"])
        assertEquals("example", properties["mod_id"])
    }

    @Test
    fun `rejects versions that cannot produce platform ranges`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlatformVersions(
                minecraft = "1.21.1-pre1",
                neoForge = "21.1.233",
                javaFmlLoader = "4",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlatformVersions(
                minecraft = "1.21.1",
                neoForge = "21",
                javaFmlLoader = "4",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlatformVersions(
                minecraft = "1.21.1",
                neoForge = "21.1.233",
                javaFmlLoader = "4.0",
            )
        }
    }
}
