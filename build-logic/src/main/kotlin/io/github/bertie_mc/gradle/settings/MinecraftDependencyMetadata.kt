package io.github.bertie_mc.gradle.settings

import org.gradle.api.artifacts.dsl.ComponentMetadataHandler

private const val MINECRAFT_DEPENDENCIES = "net.neoforged:minecraft-dependencies"
private const val MINECRAFT_VERSION = "1.21.1"
private const val JAVA_OBJC_BRIDGE = "ca.weblite:java-objc-bridge:1.1"
private const val NETTY_NATIVE_EPOLL =
    "io.netty:netty-transport-native-epoll:4.1.97.Final"

private val dependenciesMissingFromClientNativeVariants =
    mapOf(
        "clientLinuxNatives" to listOf(JAVA_OBJC_BRIDGE),
        "clientOsxNatives" to listOf(NETTY_NATIVE_EPOLL),
        "clientWindowsNatives" to listOf(JAVA_OBJC_BRIDGE, NETTY_NATIVE_EPOLL),
    )

/**
 * Keeps the locked module graph independent of the host operating system.
 *
 * NeoForge's Minecraft 1.21.1 metadata has Linux-only Netty epoll and macOS-only Java/Objective-C
 * bridge modules. Gradle lock state is configuration-specific, so every client native variant must
 * expose the union of those modules for one lock file to work on every operating system. LWJGL's
 * platform differences are classifier-only and therefore do not change module lock state.
 */
internal fun ComponentMetadataHandler.normalizeMinecraftDependencyMetadata() {
    withModule(MINECRAFT_DEPENDENCIES) {
        if (id.version == MINECRAFT_VERSION) {
            dependenciesMissingFromClientNativeVariants.forEach { (variant, dependencies) ->
                withVariant(variant) {
                    withDependencies { dependencies.forEach(::add) }
                }
            }
        }
    }
}
