package com.example.contextextractortu.formatter

import com.example.contextextractortu.model.GenericContextModel

class UniversalPromptGenerator {
    fun generate(model: GenericContextModel, template: String): String {
        val contextSection = model.items.joinToString("\n\n") { item ->
            "### ${item.title}\n${item.content}"
        }
        return template.replace("{{CONTEXT}}", contextSection)
    }
}