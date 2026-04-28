package com.example.contextextractortu.util

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

/**
 * Module de classification des classes selon l'automate DeepUnitTestStrategy
 *
 * Définit 3 catégories :
 * - Excluded : Classes système (java.*, javax.*), primitives, wrappers
 * - Internal : Classe cible et ses super-classes
 * - External : Toutes les autres classes du projet
 *
 * Architecture modulaire permettant la personnalisation des règles
 */
class ClassClassification(private val project: Project) {

    /**
     * Classe une référence de classe selon les règles de l'automate
     *
     * @param className Nom complet ou simple de la classe à classifier
     * @return ClassificationResult indiquant le type de classe
     */
    fun classify(className: String): ClassificationResult {
        // Condition 3 : Exclusions - Classes système
        if (isExcluded(className)) {
            return ClassificationResult.Excluded(className)
        }

        // Résolution de la classe
        val psiClass = resolveClass(className)
        if (psiClass == null) {
            // Classe non trouvée : considérée comme externe par défaut
            return ClassificationResult.External(className, null, isDataClass = false)
        }

        // La classification dépend du contexte - par défaut externe
        // L'identification comme "Internal" se fait au niveau de la stratégie
        return ClassificationResult.External(
            className = psiClass.qualifiedName ?: className,
            psiClass = psiClass,
            isDataClass = isDataClass(psiClass)
        )
    }

    /**
     * Classe une classe PSI directement (utile pour l'identification interne)
     */
    fun classifyPsiClass(psiClass: PsiClass): ClassificationResult {
        return if (isExcluded(psiClass.qualifiedName ?: psiClass.name ?: "")) {
            ClassificationResult.Excluded(psiClass.qualifiedName ?: psiClass.name ?: "")
        } else {
            ClassificationResult.External(
                className = psiClass.qualifiedName ?: psiClass.name ?: "Unknown",
                psiClass = psiClass,
                isDataClass = isDataClass(psiClass)
            )
        }
    }

    /**
     * Résout une classe à partir de son nom complet
     */
    private fun resolveClass(fqn: String): PsiClass? {
        val facade = JavaPsiFacade.getInstance(project)
        return facade.findClass(fqn, GlobalSearchScope.projectScope(project))
    }

    /**
     * Vérifie si une classe est exclue de l'analyse
     *
     * Exclusions :
     * - java.*, javax.* (classes système)
     * - Primitives (int, long, double, boolean, char, byte, short, float)
     * - Wrappers (Integer, Long, Double, Boolean, Character, Byte, Short, Float)
     * - String, Object, Enum, Comparable
     * - List, Set, Map, Collection, Iterator et leurs implémentation courantes
     */
    private fun isExcluded(className: String): Boolean {
        // Packages système
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return true
        }

        // Primitives
        if (className in PRIMITIVES) {
            return true
        }

        // Wrappers et classes courantes de java.lang
        if (className in JAVA_LANG_WRAPPERS) {
            return true
        }

        // Collections courantes
        if (className in COMMON_COLLECTIONS || className.startsWith("java.util.")) {
            return true
        }

        // Types courants exclus
        if (className in COMMON_EXCLUDED) {
            return true
        }

        return false
    }

    /**
     * Détermine si une classe est un objet de données (DTO, Entity, Value Object)
     * Ces objets nécessitent un corps complet dans l'extraction
     */
    private fun isDataClass(psiClass: PsiClass): Boolean {
        // Annotations indiquant un objet de données
        val hasDataAnnotation = psiClass.annotations.any { ann ->
            ann.qualifiedName?.let { qn ->
                qn.contains("DTO") ||
                qn.contains("Entity") ||
                qn.contains("Record") ||
                qn.contains("ValueObject") ||
                qn.contains("Data") ||
                qn.contains("Document")
            } ?: false
        }

        if (hasDataAnnotation) return true

        // Conventions de nommage
        val simpleName = psiClass.name ?: ""
        return when {
            simpleName.endsWith("DTO") -> true
            simpleName.endsWith("Entity") -> true
            simpleName.endsWith("Request") -> true
            simpleName.endsWith("Response") -> true
            simpleName.endsWith("Command") -> true
            simpleName.endsWith("Query") -> true
            simpleName.endsWith("ValueObject") -> true
            else -> false
        }
    }

    companion object {
        private val PRIMITIVES = setOf("int", "long", "double", "float", "boolean", "char", "byte", "short")
        private val JAVA_LANG_WRAPPERS = setOf(
            "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short",
            "String", "Object", "Enum", "Comparable", "CharSequence", "Number"
        )
        private val COMMON_COLLECTIONS = setOf(
            "List", "Set", "Map", "Collection", "Iterator", "Iterable",
            "ArrayList", "LinkedList", "HashSet", "TreeSet", "HashMap", "TreeMap",
            "LinkedHashMap", "LinkedHashSet"
        )
        private val COMMON_EXCLUDED = setOf(
            "Optional", "Stream", "OptionalInt", "OptionalLong", "OptionalDouble",
            "LocalDate", "LocalTime", "LocalDateTime", "Instant", "Date", "Calendar"
        )
    }
}

/**
 * Résultat de la classification d'une classe
 */
sealed class ClassificationResult {
    /** Classe exclue (JDK, primitives, librairies externes) */
    data class Excluded(val className: String) : ClassificationResult()

    /** Classe interne (même classe ou super-classe) */
    data class Internal(
        val className: String,
        val psiClass: PsiClass
    ) : ClassificationResult()

    /** Classe externe (autre classe du projet) */
    data class External(
        val className: String,
        val psiClass: PsiClass?,
        val isDataClass: Boolean = false
    ) : ClassificationResult()
}
