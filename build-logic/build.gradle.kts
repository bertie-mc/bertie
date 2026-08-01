plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.moddev.gradle)
}

kotlin {
    jvmToolchain(21)
}
