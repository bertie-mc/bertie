package io.github.bertie_mc.gradle

import net.neoforged.moddevgradle.dsl.ModModel
import net.neoforged.moddevgradle.dsl.UnitTest

internal fun clientTestCarrierId(subjectId: String): String = "${subjectId}_clienttests"

internal fun gameTestCarrierId(subjectId: String): String = "${subjectId}_gametests"

internal fun UnitTest.loadOnly(subjectMod: ModModel) {
    testedMod.set(subjectMod)
    loadedMods.set(setOf(subjectMod))
}
