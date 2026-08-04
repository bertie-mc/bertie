package io.github.bertie_mc.gradle

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.nio.file.Path

class TestingTasksTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `prepares and validates around the task action`() {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val report = temporaryDirectory.resolve("nested/TEST-suite.xml").toFile()
        report.parentFile.mkdirs()
        report.writeText("stale")
        val task = project.tasks.register("runSuite").get()
        task.doLast {
            report.writeText("<testsuite tests=\"1\"><testcase name=\"passes\"/></testsuite>")
        }

        task.requireTestReport(project.layout.file(project.provider { report }))
        task.actions.forEach { it.execute(task) }

        assertTrue(report.readText().contains("passes"))
    }

    @Test
    fun `preserves a task failure without running report validation`() {
        val project = ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build()
        val report = temporaryDirectory.resolve("nested/TEST-suite.xml").toFile()
        report.parentFile.mkdirs()
        report.writeText("stale")
        val launchFailure = IllegalStateException("launch failed")
        val task = project.tasks.register("runSuite").get()
        task.doLast { throw launchFailure }
        task.requireTestReport(project.layout.file(project.provider { report }))

        val failure = assertThrows(IllegalStateException::class.java) {
            task.actions.forEach { it.execute(task) }
        }

        assertSame(launchFailure, failure)
        assertFalse(report.exists())
    }

    @Test
    fun `prepares an empty report location`() {
        val report = temporaryDirectory.resolve("nested/TEST-suite.xml").toFile()
        report.parentFile.mkdirs()
        report.writeText("stale")

        prepareTestReport(report)

        assertFalse(report.exists())
        assertTrue(report.parentFile.isDirectory)
    }

    @Test
    fun `accepts a client testsuite report`() {
        val report = writeReport(
            """
            <testsuite tests="1" failures="0" errors="0">
              <testcase classname="client" name="opens screen"/>
            </testsuite>
            """,
        )

        validateTestReport(report.toFile())
    }

    @Test
    fun `accepts a nested vanilla gametest report`() {
        val report = writeReport(
            """
            <testsuite>
              <testsuite>
                <testcase classname="bertie:empty" name="loads"/>
              </testsuite>
            </testsuite>
            """,
        )

        validateTestReport(report.toFile())
    }

    @Test
    fun `rejects reports without tests`() {
        val report = writeReport("<testsuite tests=\"0\"/>")

        val failure = assertThrows(IllegalArgumentException::class.java) {
            validateTestReport(report.toFile())
        }

        assertTrue(failure.message.orEmpty().contains("contains no tests"))
    }

    @Test
    fun `rejects failed and errored test cases`() {
        val report = writeReport(
            """
            <testsuite tests="2" failures="1" errors="1">
              <testcase name="failed"><failure message="no"/></testcase>
              <testcase name="errored"><error message="crash"/></testcase>
            </testsuite>
            """,
        )

        val failure = assertThrows(IllegalArgumentException::class.java) {
            validateTestReport(report.toFile())
        }

        assertTrue(failure.message.orEmpty().contains("1 failures and 1 errors"))
    }

    @Test
    fun `rejects a missing report`() {
        val report = temporaryDirectory.resolve("missing.xml").toFile()

        val failure = assertThrows(IllegalArgumentException::class.java) {
            validateTestReport(report)
        }

        assertTrue(failure.message.orEmpty().contains("without producing"))
    }

    @Test
    fun `rejects an empty report as incomplete execution`() {
        val report = writeReport("")

        val failure = assertThrows(IllegalArgumentException::class.java) {
            validateTestReport(report.toFile())
        }

        assertTrue(failure.message.orEmpty().contains("without producing"))
    }

    private fun writeReport(contents: String): Path =
        temporaryDirectory.resolve("TEST-suite.xml").also {
            Files.writeString(it, contents.trimIndent())
        }
}
