package io.github.bertie_mc.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/** Shared subject and instance conventions used by mod and pack test suites. */
abstract class BertieTestingExtension {
    abstract val subjectId: Property<String>
    abstract val gameTestNamespace: Property<String>
    abstract val license: Property<String>
    abstract val mainInstanceDirectory: DirectoryProperty
}
