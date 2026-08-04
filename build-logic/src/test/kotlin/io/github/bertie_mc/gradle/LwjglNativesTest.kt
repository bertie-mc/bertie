package io.github.bertie_mc.gradle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class LwjglNativesTest {
    @ParameterizedTest
    @ValueSource(strings = ["aarch64", "arm64"])
    fun `selects ARM64 natives on Linux`(architecture: String) {
        assertEquals(
            LINUX_ARM64_LWJGL_NATIVE_CLASSIFIER,
            lwjglNativeClassifier("Linux", architecture),
        )
    }

    @ParameterizedTest
    @CsvSource(
        "Linux, amd64",
        "Linux, x86_64",
        "Linux, x86",
        "Mac OS X, aarch64",
        "Windows 11, arm64",
    )
    fun `does not select ARM64 Linux natives on other platforms`(
        osName: String,
        architecture: String,
    ) {
        assertNull(lwjglNativeClassifier(osName, architecture))
    }

    @Test
    fun `defines the LWJGL module set for Minecraft 1_21_1`() {
        assertEquals(
            listOf(
                "lwjgl",
                "lwjgl-freetype",
                "lwjgl-glfw",
                "lwjgl-jemalloc",
                "lwjgl-openal",
                "lwjgl-opengl",
                "lwjgl-stb",
                "lwjgl-tinyfd",
            ),
            MINECRAFT_1_21_1_LWJGL_MODULES,
        )
    }
}
