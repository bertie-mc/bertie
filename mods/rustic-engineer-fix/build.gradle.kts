plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
}

dependencies {
    runtimeOnly(mods.rusticEngineer)
    // Fabric and NeoForge publish the same version number; this is the pack's NeoForge file ID.
    runtimeOnly(mods.geckolib)
}
