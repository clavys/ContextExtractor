package com.example.contextextractortu

import com.example.contextextractortu.formatter.MarkdownFormatter
import com.example.contextextractortu.formatter.PromptTemplateEngine
import com.example.contextextractortu.model.AiContextReport
import com.example.contextextractortu.model.ClassContext
import com.example.contextextractortu.model.TargetMethod
import com.example.contextextractortu.model.VariableInfo

fun main() {
    println("--- Démarrage de l'extraction de contexte ---")

    // 1. Simulation des données (ce que vous aviez dans votre test)
    val context = AiContextReport(
        targetMethod = TargetMethod(
            className = "UserService",
            methodName = "create",
            sourceCode = "public void create() {}",
            parameters = emptyList(),
            methodCalls = emptyList()
        ),
        classUnderTest = ClassContext(
            name = "UserService",
            fields = listOf(
                VariableInfo("userRepository", "UserRepository")
            ),
            methods = emptyList()
        ),
        dependencies = emptyList()
    )

    // 2. Appel de votre formateur
    val formatter = MarkdownFormatter(PromptTemplateEngine())
    val result = formatter.generate(context)

    // 3. Affichage du résultat dans la console
    println("RÉSULTAT GÉNÉRÉ :")
    println("---------------------------------------")
    println(result)
    println("---------------------------------------")
}