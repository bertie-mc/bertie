plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
    id("bertie.jarjar-compile")
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
    jarJarCompileOnly(libs.terra.curio)
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
    jarJarCompileOnly(libs.gensokyo.delight)
    compileOnly(libs.cognition)
    compileOnly(libs.stellaris)
    compileOnly(libs.twilight.delight)
    compileOnly(libs.slavic.delight)
    compileOnly(libs.cuisine.delight)
    compileOnly(libs.berries.and.cherries)
    compileOnly(libs.better.archeology)
    compileOnly(libs.l2.complements)
    jarJarCompileOnly(libs.anvilcraft)
    compileOnly(libs.advanced.loot.info)

    testImplementation(libs.emi)
}
