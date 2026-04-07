package com.example.contextextractortu.collector

import com.example.contextextractortu.model.GenericContextModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

interface ContextStrategy {
    val id: String
    fun execute(project: Project, editor: Editor, scanner: PsiScanner): GenericContextModel
}

// Exemple d'implémentation pour tes tests unitaires actuels
class UnitTestStrategy : ContextStrategy {
    override val id = "UNIT_TEST"
    override fun execute(project: Project, editor: Editor, scanner: PsiScanner): GenericContextModel {
        val model = GenericContextModel()
        // logique actuelle de GetContextAction (trouver la méthode, etc.)
        return model
    }
}