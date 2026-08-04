package io.github.bertie_mc.gradle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinecraftArtifactsTest {
    @Test
    fun `provider presence determines Gradle and packwiz sources`() {
        val manifest = parseMinecraftArtifacts(
            """
            [mods.create]
            side = "client"

            [mods.create.maven]
            module = "com.example:create"
            version = "1.0"

            [mods.create.modrinth]
            project-id = "project"
            version-id = "version"
            filename = "create.jar"

            [mods.create.curseforge]
            slug = "create"
            project-id = 12
            file-id = 34

            [mods.curse-only.curseforge]
            slug = "curse-only"
            project-id = 56
            file-id = 78

            [mods.maven-and-curse]
            side = "server"

            [mods.maven-and-curse.maven]
            module = "com.example:maven-and-curse"
            version = "2.0"

            [mods.maven-and-curse.curseforge]
            slug = "maven-and-curse"
            project-id = 90
            file-id = 12

            [mods.modrinth-and-curse.modrinth]
            project-id = "preferred-project"
            version-id = "preferred-version"
            filename = "preferred.jar"

            [mods.modrinth-and-curse.curseforge]
            slug = "modrinth-and-curse"
            project-id = 34
            file-id = 56

            [shaderpacks.example]
            side = "client"

            [shaderpacks.example.modrinth]
            project-id = "shader"
            version-id = "release"
            filename = "shader.zip"
            """.trimIndent(),
        )

        val create = manifest.mods.single { artifact -> artifact.id == "create" }
        assertEquals(MinecraftArtifactSide.CLIENT, create.side)
        assertInstanceOf(MavenArtifactSource::class.java, create.gradleSource)
        assertInstanceOf(ModrinthArtifactSource::class.java, create.packwizSource)

        val curseOnly = manifest.mods.single { artifact -> artifact.id == "curse-only" }
        assertEquals(MinecraftArtifactSide.BOTH, curseOnly.side)
        assertInstanceOf(CurseForgeArtifactSource::class.java, curseOnly.gradleSource)
        assertInstanceOf(CurseForgeArtifactSource::class.java, curseOnly.packwizSource)
        assertEquals("curseOnly", curseOnly.catalogAlias)

        val mavenAndCurse = manifest.mods.single { artifact ->
            artifact.id == "maven-and-curse"
        }
        assertEquals(MinecraftArtifactSide.SERVER, mavenAndCurse.side)
        assertInstanceOf(MavenArtifactSource::class.java, mavenAndCurse.gradleSource)
        assertInstanceOf(CurseForgeArtifactSource::class.java, mavenAndCurse.packwizSource)

        val modrinthAndCurse = manifest.mods.single { artifact ->
            artifact.id == "modrinth-and-curse"
        }
        assertInstanceOf(ModrinthArtifactSource::class.java, modrinthAndCurse.gradleSource)
        assertInstanceOf(ModrinthArtifactSource::class.java, modrinthAndCurse.packwizSource)

        val shaderpack = manifest.shaderpacks.single()
        assertEquals(MinecraftArtifactKind.SHADERPACK, shaderpack.kind)
        assertEquals(MinecraftArtifactSide.CLIENT, shaderpack.side)
        assertEquals("shaderpacks", shaderpack.kind.destination)
        assertEquals("zip", shaderpack.kind.extension)
    }

    @Test
    fun `lower camel aliases keep prefix-related artifact ids independent`() {
        val manifest = parseMinecraftArtifacts(
            """
            [mods.accessories.modrinth]
            project-id = "base"
            version-id = "1"
            filename = "accessories.jar"

            [mods.accessories-compat-layer.modrinth]
            project-id = "compat"
            version-id = "1"
            filename = "accessories-compat-layer.jar"
            """.trimIndent(),
        )

        assertEquals(
            listOf("accessories", "accessoriesCompatLayer"),
            manifest.mods.map(MinecraftArtifact::catalogAlias),
        )
    }

    @Test
    fun `rejects artifact ids that collapse to the same catalog alias`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            parseMinecraftArtifacts(
                """
                [mods.some-mod.modrinth]
                project-id = "first"
                version-id = "1"
                filename = "first.jar"

                [mods.someMod.modrinth]
                project-id = "second"
                version-id = "1"
                filename = "second.jar"
                """.trimIndent(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("colliding Gradle aliases"))
    }

    @Test
    fun `rejects artifact sides that are not lowercase canonical values`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            parseMinecraftArtifacts(
                """
                [mods.example]
                side = "CLIENT"

                [mods.example.modrinth]
                project-id = "example"
                version-id = "1"
                filename = "example.jar"
                """.trimIndent(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("must be one of: both, client, server"))
    }

    @Test
    fun `rejects non-string artifact sides`() {
        val failure = assertThrows(IllegalStateException::class.java) {
            parseMinecraftArtifacts(
                """
                [mods.example]
                side = true

                [mods.example.modrinth]
                project-id = "example"
                version-id = "1"
                filename = "example.jar"
                """.trimIndent(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("field 'side' must be a string"))
    }
}
