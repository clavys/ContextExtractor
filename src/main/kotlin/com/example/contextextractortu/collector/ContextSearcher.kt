package com.example.contextextractortu.collector

import com.example.contextextractortu.model.GenericContextModel
import com.example.contextextractortu.strategy.DeepExtractionStrategy
import com.example.contextextractortu.util.ClassClassification
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class ContextSearcher(private val project: Project) {
    fun gather(editor: Editor, scanner: PsiScanner): GenericContextModel {
        // DeepExtractionStrategy est maintenant la seule stratégie
        val strategy = DeepExtractionStrategy(scanner, ClassClassification(project))
        return strategy.execute(project, editor, scanner)
    }
}