package io.github.bertie_mc.gradle.tasks

import io.github.bertie_mc.gradle.model.PackwizArtifact
import io.github.bertie_mc.gradle.model.RedistributionArtifactPolicy
import io.github.bertie_mc.gradle.model.RedistributionEvidence
import io.github.bertie_mc.gradle.model.identity
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun writeEmbeddingAudit(
    release: String,
    artifacts: List<PackwizArtifact>,
    strict: Boolean,
    evidence: List<RedistributionEvidence>,
    artifactPolicies: List<RedistributionArtifactPolicy>,
    output: Path,
) {
    require(artifactPolicies.distinctBy(RedistributionArtifactPolicy::identity).size == artifactPolicies.size) {
        "Redistribution policy repeats an artifact identity"
    }
    val policiesByArtifact = artifactPolicies.associateBy(RedistributionArtifactPolicy::identity)
    val artifactsByIdentity = artifacts.groupBy(PackwizArtifact::identity)
    val stalePolicies = policiesByArtifact.keys - artifactsByIdentity.keys
    require(stalePolicies.isEmpty()) {
        "Redistribution policy contains artifacts not embedded by $release: " +
            stalePolicies.sorted().joinToString()
    }
    artifactPolicies.forEach { policy ->
        val selected = artifactsByIdentity.getValue(policy.identity)
        require(selected.all { artifact -> artifact.id == policy.name }) {
            "Redistribution policy name '${policy.name}' does not match ${policy.identity}"
        }
        require(selected.all { artifact -> artifact.component == policy.component }) {
            "Redistribution policy component '${policy.component}' does not match ${policy.identity}"
        }
    }
    val evidenceById = evidence.associateBy(RedistributionEvidence::id)
    val evidenceByArtifact =
        policiesByArtifact.mapValues { (_, policy) ->
            policy.evidence
                .map { reference ->
                    evidenceById[reference]
                        ?: error("Redistribution artifact ${policy.identity} references unknown evidence '$reference'")
                }.sortedBy(RedistributionEvidence::id)
        }
    val statusByArtifact =
        artifacts.associateWith { artifact ->
            val records = evidenceByArtifact[artifact.identity].orEmpty()
            when {
                records.isEmpty() -> RedistributionStatus.UNKNOWN
                records.any { record -> !record.allowed } -> RedistributionStatus.DENIED
                else -> RedistributionStatus.ALLOWED
            }
        }
    if (strict) {
        val denied = statusByArtifact.filterValues { status -> status == RedistributionStatus.DENIED }.keys
        val unknown = statusByArtifact.filterValues { status -> status == RedistributionStatus.UNKNOWN }.keys
        val failures =
            buildList {
                if (denied.isNotEmpty()) {
                    add("redistribution is denied for: ${denied.joinToString { it.identity }}")
                }
                if (unknown.isNotEmpty()) {
                    add("redistribution evidence is missing for: ${unknown.joinToString { it.identity }}")
                }
            }
        if (failures.isNotEmpty()) error(failures.joinToString("; "))
    }
    val audit =
        buildString {
            append(release).append(" embedding audit\n")
            append("strict = ").append(strict).append("\n\n")
            artifacts.forEach { artifact ->
                append("overrides/")
                    .append(artifact.destination)
                    .append('/')
                    .append(artifact.filename)
                    .append("\n  name: ")
                    .append(artifact.id)
                    .append("\n  component: ")
                    .append(artifact.component ?: "TRANSITIVE_ONLY")
                    .append("\n  identity: ")
                    .append(artifact.identity)
                    .append("\n  sha256: ")
                    .append(clientPackFileHash(artifact.file.toPath(), "SHA-256"))
                    .append("\n  redistribution: ")
                    .append(statusByArtifact.getValue(artifact))
                    .append("\n  evidence:")
                val records = evidenceByArtifact[artifact.identity].orEmpty()
                if (records.isEmpty()) {
                    append(" MISSING")
                } else {
                    records.forEach { record ->
                        append("\n    [").append(record.id).append("] allowed = ").append(record.allowed)
                        record.text.lineSequence().forEach { line ->
                            append("\n      ").append(line)
                        }
                    }
                }
                append("\n\n")
            }
        }
    Files.createDirectories(output.parent)
    Files.writeString(output, audit, StandardCharsets.UTF_8)
}

private enum class RedistributionStatus {
    ALLOWED,
    DENIED,
    UNKNOWN,
}

internal fun writeDirectory(
    zip: ZipOutputStream,
    root: Path,
    destination: String,
) {
    if (!Files.isDirectory(root)) return
    Files.walk(root).use { paths ->
        paths
            .filter(Files::isRegularFile)
            .sorted()
            .forEach { path ->
                val relative = root.relativize(path).toString().replace(path.fileSystem.separator, "/")
                zip.writeFile("$destination/$relative", path)
            }
    }
}

internal fun ZipOutputStream.writeFile(
    name: String,
    path: Path,
) {
    putNextEntry(reproducibleEntry(name))
    Files.copy(path, this)
    closeEntry()
}

internal fun ZipOutputStream.writeEntry(
    name: String,
    contents: ByteArray,
) {
    putNextEntry(reproducibleEntry(name))
    write(contents)
    closeEntry()
}

private fun reproducibleEntry(name: String): ZipEntry = ZipEntry(name).apply { time = 0L }

internal fun clientPackFileHash(
    path: Path,
    algorithm: String,
): String {
    val digest = MessageDigest.getInstance(algorithm)
    Files.newInputStream(path).buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
