plugins {
    id("bertie.mod")
    id("bertie.client-test")
}

dependencies {
    clienttestRuntimeOnly(mods.shortCircuit)
}
