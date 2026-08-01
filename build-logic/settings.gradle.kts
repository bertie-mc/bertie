pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.neoforged.net/releases") }
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.neoforged.net/releases") }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "bertie-build-logic"
