package io.github.bertie_mc.gradle.model

data class TestSubject(
    val id: String,
    val license: String,
    val gameTestNamespace: String,
    val testedModId: String?,
)
