git add .package io.github.jeefdevelopment.cobalt.script.annotation.handler

import io.github.jeefdevelopment.cobalt.script.annotation.ImportScript
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource

object ImportScriptHandler {

    // This is the handler function referenced in the configuration
    fun handleScriptImports(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {

        // 1. Get the list of @ImportScript annotations collected by the compiler
        val annotations = context.collectedData
            ?.get(ScriptCollectedData.collectedAnnotations)
            ?.filter { it.annotation is ImportScript }
            ?: return context.compilationConfiguration.asSuccess() // No annotations found, proceed

        val importedScripts = mutableListOf<FileScriptSource>()
        val reports = mutableListOf<ScriptDiagnostic>()

        val scriptBaseDir = File("config/cobalt/util/").absoluteFile

        // 2. Process each annotation to find the script paths
        for (annotation in annotations) {
            val importScriptAnnotation = annotation.annotation as ImportScript

            for (relativePath in importScriptAnnotation.paths) {
                val scriptFile = File(scriptBaseDir, relativePath).canonicalFile

                if (scriptFile.exists()) {
                        importedScripts.add(FileScriptSource(scriptFile))
                        reports.add(
                            ScriptDiagnostic(ScriptDiagnostic.incompleteCode,"Importing script: ${scriptFile.name}",
                                severity = ScriptDiagnostic.Severity.DEBUG)
                        )
                } else {
                    reports.add(
                        ScriptDiagnostic(ScriptDiagnostic.incompleteCode,"Script file not found: $scriptFile",
                            severity = ScriptDiagnostic.Severity.ERROR)
                    )
                }
            }
        }

        // 3. Return the new configuration with the scripts added
        return if (importedScripts.isEmpty()) {
            // --- THE FIX: If there are no NEW imports, return the EXACT same configuration to break the loop! ---
            context.compilationConfiguration.asSuccess(reports)
        } else {
            context.compilationConfiguration.with {
                // Append the resolved scripts to the 'importScripts' property
                importScripts.append(importedScripts)
            }.asSuccess(reports)
        }
    }
}