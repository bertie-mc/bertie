import io.github.bertie_mc.gradle.BertiePlatformVersions
import io.github.bertie_mc.gradle.registerModMetadataTemplates

plugins {
    id("bertie.neoforge-base")
}

val platformVersions = extensions.getByType<BertiePlatformVersions>()
registerModMetadataTemplates(platformVersions.templateProperties())
