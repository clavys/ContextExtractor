package com.example.contextextractortu.formatter

import com.example.contextextractortu.model.GenericContextModel

class UniversalPromptGenerator {
    fun generate(model: GenericContextModel, template: String): String {
        var finalPrompt = template
        model.items.forEach { item ->
            // Remplace {{titre}} par le contenu de l'item
            finalPrompt = finalPrompt.replace("{{${item.title}}}", item.content)
        }
        return finalPrompt
    }
}