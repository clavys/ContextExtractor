package com.example.contextextractortu.model

/**
 * Represente une dependance extraite avec son type et son contenu
 */
data class DependencyInfo(
    val className: String,
    val classType: ClassType,
    val content: String,
    val hierarchy: HierarchyInfo,
    val depth: Int
)

/**
 * Informations sur la hierarchie d'une classe
 */
data class HierarchyInfo(
    val parentClass: String?,
    val interfaces: List<String>
)

/**
 * Types de classes pour determiner la strategie d'extraction
 */
enum class ClassType {
    DTO,        // Data Transfer Object - corps complet
    ENTITY,     // Entity JPA/Hibernate - corps complet
    SERVICE,    // Service metier - signatures seulement
    REPOSITORY, // Repository/data access - signatures seulement
    CONFIG,     // Configuration - signatures seulement
    UTIL,       // Utilitaires - corps complet
    OTHER       // Par defaut - decision contextuelle
}
