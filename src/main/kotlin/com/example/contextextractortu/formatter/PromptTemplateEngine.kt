package com.example.contextextractortu.formatter

class PromptTemplateEngine {

    fun getMetaPrompt(): String {
        return """
            You are an expert Java developer specialized in unit testing.

            Your task:
            - Generate unit tests using JUnit 5 and Mockito
            - Mock all external dependencies
            - Cover edge cases and exceptions
            - Follow clean code principles

            Return only the test code.
        """.trimIndent()
    }
}