package io.github.jeefdevelopment.cobalt.script.annotation

// Apply the annotation to the whole file
@Target(AnnotationTarget.FILE)
// The retention must be SOURCE for the compiler to see it, or RUNTIME if needed later
@Retention(AnnotationRetention.SOURCE)
annotation class ImportScript(
    // The path to the script file(s) to import, relative to the current script
    vararg val paths: String
)