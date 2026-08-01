import mc.bertie.gradle.BertieModMetadata

plugins {
    id("bertie.neoforge-unit-test")
}

val modMetadata = extensions.getByType<BertieModMetadata>()

dependencies {
    add("testImplementation", "net.neoforged:testframework:${modMetadata.neoForgeVersion}")
}
