plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
    id("bertie.gametest")
    id("bertie.server-run")
}

dependencies {
    testImplementation(libs.gson)
}
