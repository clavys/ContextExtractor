package com.example.contextextractortu.model

enum class ContextType { CODE, DOCUMENTATION, METADATA, GIT_DIFF }

data class ContextItem(
    val title: String,
    val content: String,
    val type: ContextType,
    val metadata: Map<String, String> = emptyMap()
)

data class GenericContextModel(
    val items: MutableList<ContextItem> = mutableListOf()
)