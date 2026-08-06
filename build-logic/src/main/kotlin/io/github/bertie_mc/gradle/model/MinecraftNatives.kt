package io.github.bertie_mc.gradle.model

internal const val LINUX_ARM64_LWJGL_NATIVE_CLASSIFIER = "natives-linux-arm64"

internal val MINECRAFT_1_21_1_LWJGL_NATIVE_CLASSIFIERS =
    listOf(
        "natives-linux",
        LINUX_ARM64_LWJGL_NATIVE_CLASSIFIER,
        "natives-macos",
        "natives-macos-arm64",
        "natives-windows",
        "natives-windows-arm64",
        "natives-windows-x86",
    )

internal fun lwjglNativeClassifier(
    osName: String,
    architecture: String,
): String? {
    val normalizedOsName = osName.lowercase()
    val normalizedArchitecture = architecture.lowercase()
    return LINUX_ARM64_LWJGL_NATIVE_CLASSIFIER.takeIf {
        normalizedOsName.startsWith("linux") &&
            normalizedArchitecture in setOf("aarch64", "arm64")
    }
}
