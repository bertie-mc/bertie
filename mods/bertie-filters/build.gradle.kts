plugins {
    id("bertie.neoforge-mod")
    id("bertie.jvm-test")
    id("bertie.server-run")
}

dependencies {
    compileOnly(libs.ftb.filter.system)
    compileOnly(libs.architectury.neoforge)
    compileOnly(libs.slag)
}
