plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
}

dependencies {
    testImplementation("net.neoforged:testframework:${bertiePlatform.neoForge}")
}
