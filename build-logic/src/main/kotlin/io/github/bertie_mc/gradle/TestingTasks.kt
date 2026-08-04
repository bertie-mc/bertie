package io.github.bertie_mc.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

fun Project.runTestsTask(): TaskProvider<Task> {
    val existing = tasks.findByName("runTests")
    return if (existing == null) {
        tasks.register("runTests") {
            group = "verification"
            description = "Runs every test suite declared by this project"
        }
    } else {
        tasks.named("runTests")
    }
}

fun Task.requireTestReport(report: Provider<RegularFile>) {
    doFirst(PrepareTestReportAction(report))
    doLast(ValidateTestReportAction(report))
}

private class PrepareTestReportAction(
    private val report: Provider<RegularFile>,
) : Action<Task> {
    override fun execute(task: Task) {
        prepareTestReport(report.get().asFile)
    }
}

private class ValidateTestReportAction(
    private val report: Provider<RegularFile>,
) : Action<Task> {
    override fun execute(task: Task) {
        validateTestReport(report.get().asFile)
    }
}

fun prepareTestReport(report: File) {
    report.delete()
    report.parentFile.mkdirs()
}

fun validateTestReport(report: File) {
    require(report.isFile && report.length() > 0) {
        "Test process completed without producing ${report.absolutePath}"
    }

    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
    val testCases = document.getElementsByTagName("testcase").length
    val declaredTests = document.documentElement
        .getAttribute("tests")
        ?.takeIf(String::isNotBlank)
        ?.toIntOrNull()
    require((declaredTests ?: testCases) > 0) { "Test report ${report.absolutePath} contains no tests" }

    val failures = document.getElementsByTagName("failure").length
    val errors = document.getElementsByTagName("error").length
    require(failures == 0 && errors == 0) {
        "Test report ${report.absolutePath} contains $failures failures and $errors errors"
    }
}
