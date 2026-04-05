package com.example.contextextractortu.formatter

import com.example.contextextractortu.model.AiContextReport
import com.example.contextextractortu.model.ClassContext
import kotlin.collections.joinToString

class MarkdownFormatter(
    private val promptTemplateEngine: PromptTemplateEngine
) {

    fun generate(context: AiContextReport): String {

        val metaPrompt = promptTemplateEngine.getMetaPrompt()

        return """
            $metaPrompt
            
            # CONTEXT
            
            ## Class Under Test
            ${context.classUnderTest.name}
            
            Fields:
            ${
            context.classUnderTest.fields.joinToString("\n") {
                "- ${it.type} ${it.name}"
            }
        }
            
            ---
            
            ## Method to Test
            ```java
            ${context.targetMethod.sourceCode}
            ```
            
            ---
            
            ## Dependencies
            ${formatDependencies(context.dependencies)}
            
            ---
            
            ## Method Calls
            ${
            context.targetMethod.methodCalls.joinToString("\n") {
                "- ${context.targetMethod.className}.${context.targetMethod.methodName} → ${it.className}.${it.methodName}"
            }
        }
            
        """.trimIndent()
    }

    private fun formatDependencies(dependencies: List<ClassContext>): String {
        return dependencies.joinToString("\n\n") { clazz ->
            """
            ### ${clazz.name}
            ${
                clazz.methods.joinToString("\n") { method ->
                    "- ${method.name}(${method.parameters.joinToString(", ")})"
                }
            }
            """.trimIndent()
        }
    }
}