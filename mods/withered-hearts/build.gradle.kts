import net.neoforged.moddevgradle.dsl.NeoForgeExtension

plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
    id("bertie.client-test")
}

extensions.configure<NeoForgeExtension> {
    // The heart-rendering mixin references a private nested enum and method.
    accessTransformers.from(file("src/main/resources/META-INF/accesstransformer.cfg"))
}
