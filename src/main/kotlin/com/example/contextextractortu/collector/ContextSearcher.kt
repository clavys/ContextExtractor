package com.example.contextextractortu.collector

import com.example.contextextractortu.model.GenericContextModel
import com.example.contextextractortu.model.RecursiveContextResultMapper
import com.example.contextextractortu.strategy.RecursiveExtractionStrategy
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

class ContextSearcher(private val project: Project) {
    fun gather(editor: Editor, scanner: PsiScanner): GenericContextModel {
        // Nouvelle strategie recursive selon l'algorithme du prompt.txt
        val strategy = RecursiveExtractionStrategy(scanner, project)
        val recursiveResult = strategy.execute(editor)
        return RecursiveContextResultMapper.map(recursiveResult)
    }
}