plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
    id("bertie.gametest")
    id("bertie.server-run")
}

dependencies {
    // Iron's Spells is a hard runtime dependency. The remaining entries are the
    // dependency closure required by the standalone GameTest development run.
    compileOnly(mods.ironsSpellsNSpellbooks)
    runtimeOnly(mods.ironsSpellsNSpellbooks)
    runtimeOnly(mods.curios)
    runtimeOnly(mods.geckolib)
    runtimeOnly(mods.ironsLib)
    runtimeOnly(mods.playeranimator)
    runtimeOnly(mods.simplySwords)
    runtimeOnly(mods.fzzyConfig)
    runtimeOnly(mods.kotlinForForge)
    runtimeOnly(mods.simplyTooltips)
    runtimeOnly(mods.architecturyApi)
}
