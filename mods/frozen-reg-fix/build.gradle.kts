plugins {
    id("bertie.mod")
    id("bertie.gametest")
}

dependencies {
    gametestRuntimeOnly(deps.immersiveArmors)
}
