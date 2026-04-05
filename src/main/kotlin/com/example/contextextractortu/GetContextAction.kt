package com.example.contextextractortu

import com.example.contextextractortu.collector.PsiScanner
import com.example.contextextractortu.formatter.MarkdownFormatter
import com.example.contextextractortu.formatter.PromptTemplateEngine
import com.example.contextextractortu.model.AiContextReport
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

class GetContextAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    // On initialise nos composants
    private val scanner = PsiScanner()
    private val formatter = MarkdownFormatter(PromptTemplateEngine())

    override fun update(e: AnActionEvent) {
        // L'action n'est visible que si on est dans un fichier Java/Kotlin
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = psiFile is PsiJavaFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // 1. Trouver où est le curseur
        val offset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(offset)
        val method = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod::class.java)

        if (method == null) {
            Messages.showWarningDialog(project, "Place ton curseur dans une méthode !", "Erreur")
            return
        }

        // 2. COLLECTE (Orchestration)
        // On récupère la classe parente
        val parentClass = method.containingClass ?: return

        // On construit le rapport via le scanner
        val targetMethodModel = scanner.extractTargetMethod(method)
        val mainClassContext = scanner.extractClassContext(parentClass)

        // Pour l'instant, on laisse les dépendances vides ou on en ajoute quelques-unes
        val report = AiContextReport(
            targetMethod = targetMethodModel,
            classUnderTest = mainClassContext,
            dependencies = emptyList() // On améliorera le ClassResolver après
        )

        // 3. FORMATAGE
        val markdown = formatter.generate(report)

        // 4. AFFICHAGE
        // Au lieu d'une simple popup, on pourrait copier dans le presse-papier plus tard
        Messages.showInfoMessage(project, markdown, "Contexte IA Généré")
    }
}