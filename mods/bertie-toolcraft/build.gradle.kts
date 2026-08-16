plugins {
    id("bertie.mod")
    id("bertie.dev-runs")
}

dependencies {
    // Magitech owns the part/assembly system this mod reshapes; Slag owns the metallurgy we keep.
    // Both are compile-only: every reference is behind a ModList guard or a non-required mixin.
    compileOnly(deps.magitech)
    compileOnly(deps.slagNEmbers)
}
