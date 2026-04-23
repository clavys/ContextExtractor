package com.example.contextextractortu.formatter

import com.example.contextextractortu.model.GenericContextModel

class UniversalPromptGenerator {

    /**
     * Génère un prompt en remplaçant les placeholders et en supprimant les sections vides
     */
    fun generate(model: GenericContextModel, template: String): String {
        var finalPrompt = template

        // Remplacer tous les placeholders par leur contenu
        model.items.forEach { item ->
            finalPrompt = finalPrompt.replace("{{${item.title}}}", item.content)
        }

        // Supprimer les sections conditionnelles vides (syntaxe {{#hasX}}...{{/hasX}})
        finalPrompt = removeEmptyConditionalBlocks(finalPrompt)

        // Nettoyer les lignes vides consécutives
        finalPrompt = cleanEmptyLines(finalPrompt)

        return finalPrompt
    }

    /**
     * Génère un prompt avec instructions par défaut (quand utilisateur ne fournit rien)
     */
    fun generateWithDefaultPrompt(model: GenericContextModel, template: String): String {
        val defaultInstructions = """
            Génère un test unitaire complet qui couvre :
            - Le cas nominal (happy path)
            - Les cas limites (boundary cases)
            - La vérification des effets de bord (champs modifiés, appels externes)

            Le test doit être autonome et ne pas dépendre d'un environnement externe.
        """.trimIndent()
        return generateWithLayers(model, "Tu es un expert en génération de tests unitaires Java avec JUnit 5 et Mockito.", defaultInstructions)
    }

    /**
     * Génère un prompt multi-couches avec système, contexte et instructions utilisateur
     */
    fun generateWithLayers(
        model: GenericContextModel,
        systemPrompt: String,
        userInstructions: String? = null
    ): String {
        val contextPart = generate(model, template = buildString {
            append("# CONTEXTE CODE\n\n")
            model.items.forEach { item ->
                // Ne pas afficher les items vides
                if (item.content.isNotBlank()) {
                    append("## ${item.title}\n")
                    append("```\n${item.content}\n```\n\n")
                }
            }
        })

        return buildString {
            append("# RÔLE\n")
            append(systemPrompt)
            append("\n\n")

            append(contextPart)

            userInstructions?.let {
                append("# INSTRUCTIONS UTILISATEUR\n")
                append(it)
                append("\n\n")
            }

            append("# TÂCHE\n")
            append("Génère un test unitaire complet et compilable basé sur le contexte ci-dessus.\n")
        }
    }

    /**
     * Supprime les blocs conditionnels vides (syntaxe Mustache-like)
     * Ex: {{#hasInternalMethods}}...{{/hasInternalMethods}} -> supprimé si vide
     */
    private fun removeEmptyConditionalBlocks(input: String): String {
        // Pattern pour matcher {{#hasX}}...{{/hasX}} - les accolades sont échappées
        val pattern = Regex("\\{\\{#(has\\w+)\\}\\}(.*?)\\{\\{\\/\\1\\}\\}", RegexOption.DOT_MATCHES_ALL)

        return pattern.replace(input) { match ->
            val content = match.groupValues[2]
            // Garder le bloc seulement si le contenu n'est pas vide
            if (content.isNotBlank()) {
                content.trim()
            } else {
                ""
            }
        }
    }

    /**
     * Nettoie les lignes vides consécutives (max 1 ligne vide entre sections)
     */
    private fun cleanEmptyLines(input: String): String {
        return input.replace(Regex("\n{3,}"), "\n\n")
    }
}
