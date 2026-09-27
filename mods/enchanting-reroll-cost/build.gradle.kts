plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.client-test")
}

dependencies {
    compileOnly(deps.apothicEnchanting)
    compileOnly(deps.easyMagicApotheosisCompat)
    runtimeOnly(deps.apothicEnchanting)
    runtimeOnly(deps.easyMagicApotheosisCompat)
    testImplementation(deps.apothicEnchanting)
}
