plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.client-test")
}

dependencies {
    runtimeOnly(deps.mekanism)
    runtimeOnly(deps.mekanismCovers)
    clienttestCompileOnly(deps.mekanismCovers)
}
