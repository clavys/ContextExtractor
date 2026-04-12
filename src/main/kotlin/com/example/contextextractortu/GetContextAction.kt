package com.example.contextextractortu

import com.example.contextextractortu.collector.ContextSearcher
import com.example.contextextractortu.formatter.UniversalPromptGenerator
import com.example.contextextractortu.strategy.UnitTestStrategy
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
        // L'action n'est visible que si on est dans un fichier Java/Kotlin
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = psiFile is PsiJavaFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        // 1. Initialisation des composants de la nouvelle architecture
        // Le searcher utilise ton PsiScanner interne pour extraire les données
        val searcher = ContextSearcher(project)
        val strategy = UnitTestStrategy()
        val generator = UniversalPromptGenerator()

        // 2. COLLECTE : On lance la stratégie de recherche
        // gather() appelle en interne strategy.execute(project, editor, scanner)
        val genericModel = searcher.gather(strategy, editor)

        // Vérification rapide si la stratégie a trouvé du contenu
        if (genericModel.items.isEmpty()) {
            Messages.showWarningDialog(project, "Aucun contexte trouvé. Place ton curseur dans une méthode !", "Erreur")
            return
        }

        // 3. CHARGEMENT DU TEMPLATE
        // Ici, tu peux charger le texte de ton fichier 'example.md'
        // Pour l'instant, voici une simulation du chargement :
        val templateContent = """
        # CONTEXTE
        Classe : {{className}}
        Code de la méthode :
        {{methodCode}}
        Appels détectés :
        {{methodCalls}}
    """.trimIndent()

        // 4. GÉNÉRATION ET AFFICHAGE
        // Le générateur remplace les {{titre}} par le contenu des items du modèle
        val finalPrompt = generator.generate(genericModel, templateContent)

        Messages.showInfoMessage(project, finalPrompt, "Contexte IA Généré (Version Générique)")
    }
}