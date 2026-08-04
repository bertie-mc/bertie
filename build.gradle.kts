plugins {
    base
    id("bertie.dependency-verification")
}

val testInfrastructure = tasks.register("testInfrastructure") {
    group = "verification"
    description = "Runs tests for shared Gradle and in-game testing infrastructure"
    dependsOn(
        gradle.includedBuild("build-logic").task(":test"),
        ":testing:client-test-api:test",
        ":testing:client-test-driver:test",
        ":testing:gametest-driver:test",
    )
}

tasks.named("check") {
    dependsOn(testInfrastructure)
}
