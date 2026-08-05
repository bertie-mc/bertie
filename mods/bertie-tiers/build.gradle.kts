plugins {
    id("bertie.mod")
    id("bertie.gametest")
    id("bertie.dev-runs")
}

dependencies {
    testImplementation(libs.gson)
}
