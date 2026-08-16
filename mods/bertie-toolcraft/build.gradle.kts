plugins {
    id("bertie.mod")
    id("bertie.dev-runs")
}

dependencies {
    // Magitech owns the part/assembly system this mod reshapes; Slag owns the metallurgy we keep.
    // Both are compile-only: every reference is behind a ModList guard or a non-required mixin.
    compileOnly(deps.magitech)
    compileOnly(deps.slagNEmbers)
    // Carving is ours, but routing its output to Magitech parts is Bertie pack policy rather than
    // carving behaviour — the standalone mod still produces Slag parts. So it is mixed in from
    // here instead of written into the mod.
    compileOnly(project(":mods:berlords-carving"))
}
