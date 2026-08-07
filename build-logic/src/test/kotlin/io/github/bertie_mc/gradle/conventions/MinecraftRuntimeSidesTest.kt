package io.github.bertie_mc.gradle.conventions

import io.github.bertie_mc.gradle.model.CurseForgeArtifactSource
import io.github.bertie_mc.gradle.model.MinecraftArtifact
import io.github.bertie_mc.gradle.model.MinecraftArtifactKind
import io.github.bertie_mc.gradle.model.MinecraftArtifactManifest
import io.github.bertie_mc.gradle.model.MinecraftArtifactSide
import io.github.bertie_mc.gradle.model.MinecraftComponent
import io.github.bertie_mc.gradle.model.MinecraftComponentSelection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinecraftRuntimeSidesTest {
    @Test
    fun `client projection excludes server-only locked modules`() {
        assertEquals(
            setOf(MinecraftModule("curse.maven", "server-2")),
            manifest().modulesExcludedFrom(MinecraftArtifactSide.CLIENT),
        )
    }

    @Test
    fun `server projection excludes client-only locked modules`() {
        assertEquals(
            setOf(MinecraftModule("curse.maven", "client-1")),
            manifest().modulesExcludedFrom(MinecraftArtifactSide.SERVER),
        )
    }

    private fun manifest(): MinecraftArtifactManifest {
        val artifacts =
            listOf(
                artifact("client", 1, MinecraftArtifactSide.CLIENT),
                artifact("server", 2, MinecraftArtifactSide.SERVER),
                artifact("both", 3, MinecraftArtifactSide.BOTH),
            ).associateBy(MinecraftArtifact::identity)
        return MinecraftArtifactManifest(
            profile = "development",
            inputHash = "fixture",
            components =
                artifacts.values.associate { artifact ->
                    artifact.id to MinecraftComponent(artifact.id)
                },
            selections =
                artifacts.values.associate { artifact ->
                    artifact.id to MinecraftComponentSelection(artifact.identity, artifact.identity)
                },
            artifacts = artifacts,
        )
    }

    private fun artifact(
        id: String,
        projectId: Long,
        side: MinecraftArtifactSide,
    ): MinecraftArtifact =
        MinecraftArtifact(
            identity = "curseforge:$projectId:$projectId",
            id = id,
            component = id,
            kind = MinecraftArtifactKind.MOD,
            side = side,
            source = CurseForgeArtifactSource(id, projectId, projectId),
            filename = "$id.jar",
            provides = listOf(id),
            bundledProvides = emptyList(),
            required = emptyList(),
            optional = emptyList(),
        )
}
