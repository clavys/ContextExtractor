package com.example.contextextractortu.collector

import com.example.contextextractortu.model.GenericContextModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class ContextSearcher(private val project: Project) {
    private val scanner = PsiScanner()

    fun gather(strategy: ContextStrategy, editor: Editor): GenericContextModel {
        // La stratégie décide quoi chercher, le searcher fournit les outils (scanner)
        return strategy.execute(project, editor, scanner)
    }
}