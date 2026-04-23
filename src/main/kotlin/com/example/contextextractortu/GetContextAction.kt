package com.example.contextextractortu

import com.example.contextextractortu.collector.ContextSearcher
import com.example.contextextractortu.collector.PsiScanner
import com.example.contextextractortu.formatter.UniversalPromptGenerator
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiJavaFile

class GetContextAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = psiFile is PsiJavaFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        // Initialisation des composants
        val scanner = PsiScanner()
        val searcher = ContextSearcher(project)
        val generator = UniversalPromptGenerator()

        // 1. COLLECTE : Extraction du contexte (DeepExtractionStrategy est la seule stratégie)
        val genericModel = searcher.gather(editor, scanner)

        // Vérification si du contenu a été trouvé
        if (genericModel.items.isEmpty()) {
            Messages.showWarningDialog(project, "Aucun contexte trouvé. Place ton curseur dans une méthode !", "Erreur")
            return
        }

        // 2. CHARGEMENT DU TEMPLATE
        val templateContent = TemplateManager.loadTemplate("deep-unit-test")

        if (templateContent == null) {
            Messages.showWarningDialog(
                project,
                "Template 'deep-unit-test' non trouvé.",
                "Erreur"
            )
            return
        }

        // 3. Demander des instructions utilisateur optionnelles
        val userInstructions = Messages.showInputDialog(
            project,
            "Instructions spécifiques pour le test (optionnel) :\n\n" +
                    "Ex: Teste le cas où l'email est invalide\n" +
                    "Laisser vide pour utiliser les instructions par défaut",
            "Instructions Utilisateur",
            Messages.getQuestionIcon()
        )

        // 4. GÉNÉRATION DU PROMPT
        val finalPrompt = if (userInstructions.isNullOrBlank()) {
            // Utiliser un prompt par défaut quand pas d'instructions utilisateur
            generator.generateWithDefaultPrompt(genericModel, templateContent)
        } else {
            generator.generateWithLayers(genericModel, getDefaultSystemPrompt(), userInstructions)
        }

        // 5. AFFICHAGE AVEC OPTION DE COPIE
        showPromptWithCopyOption(project, finalPrompt)
    }

    /**
     * Affiche le prompt généré avec une option pour le copier dans le presse-papier
     */
    private fun showPromptWithCopyOption(project: com.intellij.openapi.project.Project, prompt: String) {
        // Afficher le prompt dans une boîte de dialogue avec bouton copier
        val dialog = PromptCopyDialog(project, prompt)
        dialog.show()
    }

    private fun getDefaultSystemPrompt(): String {
        return "Tu es un expert en génération de tests unitaires Java avec JUnit 5 et Mockito."
    }
}
