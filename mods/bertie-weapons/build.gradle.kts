plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
    id("bertie.gametest")
    id("bertie.server-run")
}

dependencies {
    // Iron's Spells is a hard runtime dependency. The remaining entries are the
    // dependency closure required by the standalone GameTest development run.
    compileOnly(libs.irons.spells)
    runtimeOnly(libs.irons.spells)
    runtimeOnly(libs.curios)
    runtimeOnly(libs.geckolib)
    runtimeOnly(libs.irons.lib)
    runtimeOnly(libs.playeranimator)
    runtimeOnly(libs.simply.swords)
    runtimeOnly(libs.fzzy.config)
    runtimeOnly(libs.kotlin.forge)
    runtimeOnly(libs.simply.tooltips)
    runtimeOnly(libs.architectury.api)
}
