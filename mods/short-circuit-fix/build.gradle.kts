plugins {
    id("bertie.neoforge-mod")
    id("bertie.client-test")
}

dependencies {
    clienttestRuntimeOnly(mods.shortCircuit)
}
