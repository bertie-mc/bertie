plugins {
    id("bertie.neoforge-mod")
    id("bertie.neoforge-unit-test")
}

dependencies {
    runtimeOnly(mods.rusticEngineer)
    // Fabric and NeoForge publish the same version number; this is the pack's NeoForge file ID.
    runtimeOnly(mods.geckolib)
}
