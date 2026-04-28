package com.example.contextextractortu.model

/**
 * Resultat complet de l'extraction recursive selon l'algorithme ExplorerContexte
 */
data class RecursiveContextResult(
    // SUT_BOOTSTRAP - Bloc 1: Hierarchie
    val hierarchy: MutableList<HierarchyLevel> = mutableListOf(),

    // SUT_BOOTSTRAP - Bloc 2: Methode cible
    var targetMethod: TargetMethodAnalysis? = null,

    // SUT_BOOTSTRAP - Bloc 3: Champs
    val fields: MutableList<Field> = mutableListOf(),

    // SUT_BOOTSTRAP - Bloc 4: Plan d'instanciation
    var instantiationPlan: InstantiationPlan = InstantiationPlan(),

    // INTERNAL_LOGIC - clé = "Classe#methode"
    val internalMethods: MutableMap<String, InternalMethodAnalysis> = mutableMapOf(),

    // MOCK_EXTERNAL - clé = nom classe concrete
    val mocks: MutableMap<String, MockInfo> = mutableMapOf(),

    // DATA_STRUCTURE - clé = nom classe
    val dataStructures: MutableMap<String, DataStructureInfo> = mutableMapOf()
)

/**
 * Niveau de hierarchie de classe
 */
data class HierarchyLevel(
    val className: String,
    val annotations: List<String> = emptyList()
)

/**
 * Analyse complete de la methode cible
 */
data class TargetMethodAnalysis(
    val signature: String,
    val exceptionsThrown: List<String> = emptyList(),
    val exceptionsCaught: List<String> = emptyList(),
    val sourceCode: String = ""
)

/**
 * Type resolu avec generiques
 */
data class ResolvedType(
    val rawType: String,
    val typeArguments: List<ResolvedType> = emptyList(),
    val isCollection: Boolean = false,
    val isNullable: Boolean = false
)

/**
 * Parametre de methode/constructeur
 */
data class SimpleParameter(
    val name: String,
    val type: ResolvedType,
    val annotations: List<String> = emptyList()
)

/**
 * Champ de classe
 */
data class Field(
    val name: String,
    val type: ResolvedType,
    val visibility: String = "private",
    val annotations: List<String> = emptyList()
)

/**
 * Plan d'instanciation du SUT
 */
data class InstantiationPlan(
    val constructorParams: List<SimpleParameter> = emptyList(),
    val superValues: List<String> = emptyList(),
    val setters: List<Setter> = emptyList(),
    val postConstructMethods: List<String> = emptyList()
)

/**
 * Setter / wither
 */
data class Setter(
    val methodName: String,
    val fieldName: String,
    val type: ResolvedType
)

/**
 * Analyse d'une methode interne
 */
data class InternalMethodAnalysis(
    val visitKey: String,
    val signature: String,
    val exceptionsThrown: List<String> = emptyList(),
    val exceptionsCaught: List<String> = emptyList(),
    val body: String = ""
)

/**
 * Information sur un mock
 */
data class MockInfo(
    val concreteClass: String,
    val interfaceName: String? = null,
    val classAnnotations: List<String> = emptyList(),
    val requiredSignatures: List<String> = emptyList()
)

/**
 * Information sur une structure de donnees
 */
data class DataStructureInfo(
    val className: String,
    val constructionPattern: ConstructionPattern,
    val fields: List<DataField> = emptyList(),
    val collections: List<CollectionInfo> = emptyList()
)

/**
 * Pattern de construction
 */
enum class ConstructionPattern {
    RECORD, BUILDER, CONSTRUCTOR, SETTER_BASED
}

/**
 * Champ de structure de donnees
 */
data class DataField(
    val name: String,
    val type: ResolvedType,
    val annotations: List<String> = emptyList(),
    val defaultValue: String? = null
)

/**
 * Information sur une collection
 */
data class CollectionInfo(
    val fieldName: String,
    val collectionType: String,
    val elementType: ResolvedType
)

