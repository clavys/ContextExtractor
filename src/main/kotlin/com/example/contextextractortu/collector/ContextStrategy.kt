package com.example.contextextractortu.collector

import com.example.contextextractortu.model.GenericContextModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

interface ContextStrategy {
    val id: String
    fun execute(project: Project, editor: Editor, scanner: PsiScanner): GenericContextModel
}
