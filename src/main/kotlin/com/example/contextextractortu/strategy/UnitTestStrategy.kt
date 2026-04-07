package com.example.contextextractortu.strategy

import com.example.contextextractortu.collector.ContextStrategy
import com.example.contextextractortu.collector.PsiScanner
import com.example.contextextractortu.model.ContextItem
import com.example.contextextractortu.model.ContextType
import com.example.contextextractortu.model.GenericContextModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

class UnitTestStrategy : ContextStrategy {
    override val id: String = "UNIT_TEST"

    override fun execute(project: Project, editor: Editor, scanner: PsiScanner): GenericContextModel {
        val model = GenericContextModel()

        // Récupération du contexte via IntelliJ API (équivalent à ton Action actuelle)
        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return model
        val offset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(offset)
        val method = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod::class.java) ?: return model
        val parentClass = method.containingClass ?: return model

        // Extraction technique via ton scanner existant
        val targetMethodModel = scanner.extractTargetMethod(method)
        val mainClassContext = scanner.extractClassContext(parentClass)

        // Mapping vers le modèle générique pour alimenter ton template example.md
        model.items.add(ContextItem("className", mainClassContext.name, ContextType.METADATA))

        val fieldsText = mainClassContext.fields.joinToString("\n") { "- ${it.type} ${it.name}" }
        model.items.add(ContextItem("fields", fieldsText, ContextType.CODE))

        model.items.add(ContextItem("methodCode", targetMethodModel.sourceCode, ContextType.CODE))

        val callsText = targetMethodModel.methodCalls.joinToString("\n") {
            "- ${targetMethodModel.className}.${targetMethodModel.methodName} → ${it.className}.${it.methodName}"
        }
        model.items.add(ContextItem("methodCalls", callsText, ContextType.CODE))

        return model
    }
}