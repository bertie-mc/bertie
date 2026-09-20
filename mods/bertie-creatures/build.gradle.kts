plugins {
    id("bertie.mod")
    id("bertie.gametest")
    id("bertie.client-test")
    id("bertie.dev-runs")
}

dependencies {
    compileOnly(deps.citadel)
    runtimeOnly(deps.citadel)
    testImplementation(libs.gson)
}

tasks.jar { from("LICENSE", "NOTICE") }
