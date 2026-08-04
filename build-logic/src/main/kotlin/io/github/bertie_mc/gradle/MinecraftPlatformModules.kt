package io.github.bertie_mc.gradle

/**
 * Minecraft libraries whose presence in the dependency graph depends on the host platform.
 *
 * Mojang's 1.21.1 library manifest gates these behind OS rules, so they enter the graph on one
 * platform and are absent on every other. A lock file written on Linux therefore demands a module
 * that a Windows resolution can never produce, and vice versa - `Did not resolve
 * 'io.netty:netty-transport-native-epoll:4.1.97.Final' which is part of the dependency lock state`.
 *
 * These are excluded from lock state by name so that every other module stays strictly locked.
 * Relaxing the lock mode instead would have dropped that guarantee for the whole graph.
 *
 * Adding an entry here means editing every existing lock file too: Gradle rejects a lock file that
 * still lists an ignored module.
 */
internal val MINECRAFT_1_21_1_PLATFORM_CONDITIONAL_MODULES = listOf(
    // Linux-only epoll transport. Its classes counterpart, netty-transport-classes-epoll, is
    // unconditional and stays locked.
    "io.netty:netty-transport-native-epoll",
)
