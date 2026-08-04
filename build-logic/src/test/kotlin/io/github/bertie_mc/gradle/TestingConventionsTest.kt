package io.github.bertie_mc.gradle

import net.neoforged.moddevgradle.dsl.ModModel
import net.neoforged.moddevgradle.dsl.UnitTest
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TestingConventionsTest {
    @Test
    fun `derives carrier ids from the test subject`() {
        assertEquals("configmigrations_clienttests", clientTestCarrierId("configmigrations"))
        assertEquals("bertiepacktests_gametests", gameTestCarrierId("bertiepacktests"))
    }

    @Test
    fun `unit tests load only their subject mod`() {
        val project = ProjectBuilder.builder().build()
        val mods = project.container(ModModel::class.java)
        val subject = mods.create("subject")
        val clientTests = mods.create("subject_clienttests")
        val unitTest = project.objects.newInstance(UnitTest::class.java, project)
        unitTest.loadedMods.add(clientTests)

        unitTest.loadOnly(subject)

        assertSame(subject, unitTest.testedMod.get())
        assertEquals(setOf(subject), unitTest.loadedMods.get())
    }
}
