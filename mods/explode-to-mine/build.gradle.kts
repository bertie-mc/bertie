plugins {
    id("bertie.mod")
    id("bertie.neoforge-test")
    id("bertie.dev-runs")
    id("bertie.datagen")
}

dependencies {
    testImplementation("net.neoforged:testframework:${bertiePlatform.neoForge}")
}
