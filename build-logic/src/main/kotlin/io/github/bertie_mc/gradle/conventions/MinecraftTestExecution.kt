package io.github.bertie_mc.gradle.conventions

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

internal abstract class MinecraftTestExecution :
    BuildService<BuildServiceParameters.None>

internal fun Project.useMinecraftTestExecutionSlot(taskName: String) {
    val execution = gradle.sharedServices.registerIfAbsent(
        "bertieMinecraftTestExecution",
        MinecraftTestExecution::class.java,
    ) {
        maxParallelUsages.set(1)
    }
    tasks.named(taskName).configure { usesService(execution) }
}
