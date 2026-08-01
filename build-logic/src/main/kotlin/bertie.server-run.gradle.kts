import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("bertie.neoforge-mod")
}

extensions.configure<NeoForgeExtension> {
    runs.register("server") {
        server()
        programArgument("--nogui")
    }
}
