plugins {
    id("bertie.mod")
}

dependencies {
    compileOnly(deps.forbiddenArcanus)
    compileOnly(deps.valhelsiaCore)
    runtimeOnly(deps.forbiddenArcanus)
    runtimeOnly(deps.ironsSpellsNSpellbooks)
}
