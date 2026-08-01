import mc.bertie.gradle.tasks.ExtractNestedJars

plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
}

val jarJarParents by configurations.creating
val extractedLibsDir = layout.buildDirectory.dir("extracted-jarjar-libs")
val extractNestedJars = tasks.register<ExtractNestedJars>("extractNestedJars") {
    description = "Extract integration APIs published only inside their parent mods"
    archives.from(jarJarParents)
    libraryNames.set(
        listOf(
            "anvillib-neoforge-1.21.1-1.4.0",
            "l2core-3.0.8",
            "l2serial-3.0.9",
            "org.confluence.lib.confluence_magic_lib",
        ),
    )
    destinationDirectory.set(extractedLibsDir)
}

dependencies {
    compileOnly(libs.emi)
    compileOnly(libs.slag)
    compileOnly(variantOf(libs.create.maven) { classifier("slim") }) {
        isTransitive = false
    }
    compileOnly(libs.cataclysm)
    compileOnly(libs.irons.spells)
    compileOnly(libs.forbidden.arcanus)
    compileOnly(libs.terra.curio)
    compileOnly(libs.enderio)
    compileOnly(libs.malum)
    compileOnly(libs.lodestone)
    compileOnly(libs.l2library)
    compileOnly(libs.extra.delight)
    compileOnly(libs.dungeons.delight)
    compileOnly(libs.expanded.delight)
    compileOnly(libs.avaritias.delight)
    compileOnly(libs.farmers.pizzeria)
    compileOnly(libs.farmers.delight)
    compileOnly(libs.gensokyo.delight)
    compileOnly(libs.cognition)
    compileOnly(libs.stellaris)
    compileOnly(libs.twilight.delight)
    compileOnly(libs.slavic.delight)
    compileOnly(libs.cuisine.delight)
    compileOnly(libs.berries.and.cherries)
    compileOnly(libs.better.archeology)
    compileOnly(libs.l2.complements)
    compileOnly(libs.anvilcraft)
    compileOnly(libs.advanced.loot.info)

    add(jarJarParents.name, libs.anvilcraft)
    add(jarJarParents.name, libs.gensokyo.delight)
    add(jarJarParents.name, libs.terra.curio)
    compileOnly(
        fileTree(extractedLibsDir) {
            include("*.jar")
        }.builtBy(extractNestedJars),
    )

    testImplementation(libs.emi)
}
