package mc.bertie.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

@CacheableTask
abstract class ExtractNestedJars : DefaultTask() {
    @get:Classpath
    abstract val archives: ConfigurableFileCollection

    @get:Input
    abstract val libraryNames: ListProperty<String>

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @TaskAction
    fun extract() {
        val destination = destinationDirectory.get().asFile
        destination.deleteRecursively()
        destination.mkdirs()

        val wanted = libraryNames.get()
        archives.files.forEach { parent ->
            ZipFile(parent).use { archive ->
                archive.entries().asSequence()
                    .filter { entry ->
                        entry.name.startsWith("META-INF/jarjar/") &&
                            entry.name.endsWith(".jar") &&
                            wanted.any(entry.name::contains)
                    }
                    .forEach { entry ->
                        val target = destination.resolve(entry.name.substringAfterLast('/'))
                        archive.getInputStream(entry).use { input ->
                            target.outputStream().use(input::copyTo)
                        }
                        logger.lifecycle("Extracted ${target.name} from ${parent.name}")
                    }
            }
        }
    }
}
