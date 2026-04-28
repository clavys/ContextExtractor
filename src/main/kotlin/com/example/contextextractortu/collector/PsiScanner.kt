package com.example.contextextractortu.collector

import com.example.contextextractortu.model.*
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType

/**
 * Scanner PSI ameliore - Transforme en "Context Test Crawler"
 *
 * Capable de naviguer entre les fichiers pour reconstruire l'arbre complet des besoins du test :
 * - Extraction des constructeurs et setters pour les classes externes
 * - Detection des modifications de champs (side-effects)
 * - Recursion sur les types de parametres et de retours
 */
class PsiScanner {

    /**
     * Extrait la methode cible avec toutes ses informations
     */
    fun extractTargetMethod(method: PsiMethod): TargetMethod {
        val calls = mutableListOf<MethodCall>()
        val sideEffects = mutableListOf<SideEffect>()

        // Extraction des appels de methodes et des side-effects
        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val resolved = expression.resolveMethod()
                val className = resolved?.containingClass?.name ?: "Unknown"
                calls.add(MethodCall(className, expression.methodExpression.referenceName ?: ""))
            }

            override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
                super.visitAssignmentExpression(expression)
                val resolved = expression.lExpression?.reference?.resolve()
                if (resolved is com.intellij.psi.PsiField) {
                    sideEffects.add(SideEffect(resolved.name, "assignment"))
                }
            }
        })

        return TargetMethod(
            className = method.containingClass?.name ?: "Unknown",
            methodName = method.name,
            sourceCode = method.text,
            parameters = method.parameterList.parameters.map {
                VariableInfo(it.name, it.type.presentableText)
            },
            methodCalls = calls,
            sideEffects = sideEffects
        )
    }

    /**
     * Extrait le contexte complet d'une classe
     */
    fun extractClassContext(psiClass: PsiClass): ClassContext {
        return ClassContext(
            name = psiClass.name ?: "Unknown",
            qualifiedName = psiClass.qualifiedName,
            fields = psiClass.fields.map { VariableInfo(it.name, it.type.presentableText) },
            constructors = psiClass.constructors.map { ctor ->
                ConstructorInfo(
                    name = ctor.name ?: psiClass.name ?: "constructor",
                    parameters = ctor.parameterList.parameters.map { param ->
                        VariableInfo(param.name, param.type.presentableText)
                    },
                    isAutowired = ctor.annotations.any { it.qualifiedName?.contains("Autowired") == true }
                )
            },
            methods = psiClass.methods.map { m ->
                MethodSignature(
                    name = m.name,
                    parameters = m.parameterList.parameters.map { it.type.presentableText },
                    returnType = m.returnType?.presentableText ?: "void"
                )
            }
        )
    }

    /**
     * Extrait les appels de methodes d'une methode donnee
     * Avec resolution complete du FQN
     */
    fun extractMethodCalls(method: PsiMethod): List<MethodCall> {
        val calls = mutableSetOf<MethodCall>()

        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val resolved = expression.resolveMethod()
                if (resolved != null) {
                    val className = resolved.containingClass?.qualifiedName ?: resolved.containingClass?.name ?: "Unknown"
                    val methodName = expression.methodExpression.referenceName ?: resolved.name
                    calls.add(MethodCall(className, methodName))
                }
            }
        })

        return calls.toList()
    }

    /**
     * Extrait les constructeurs et setters d'une classe externe
     * Essentiel pour savoir comment instancier/mocker une classe
     */
    fun extractExternalClassInfo(className: String, project: com.intellij.openapi.project.Project): ExternalClassInfo? {
        val facade = com.intellij.psi.JavaPsiFacade.getInstance(project)
        val psiClass = facade.findClass(className, com.intellij.psi.search.GlobalSearchScope.projectScope(project)) ?: return null

        return extractExternalClassInfo(psiClass)
    }

    /**
     * Extrait les informations d'une classe externe
     */
    fun extractExternalClassInfo(psiClass: PsiClass): ExternalClassInfo {
        val constructors = psiClass.constructors.map { ctor ->
            ConstructorInfo(
                name = ctor.name ?: psiClass.name ?: "constructor",
                parameters = ctor.parameterList.parameters.map { param ->
                    VariableInfo(param.name, param.type.presentableText)
                },
                isAutowired = ctor.annotations.any { it.qualifiedName?.contains("Autowired") == true }
            )
        }

        val setters = psiClass.methods
            .filter { it.name.startsWith("set") && it.parameterList.parameters.size == 1 }
            .map { method ->
                SetterInfo(
                    name = method.name,
                    parameterType = method.parameterList.parameters.first().type.presentableText,
                    fieldName = method.name.removePrefix("set").replaceFirstChar { it.lowercase() }
                )
            }

        val methods = psiClass.methods.map { m ->
            MethodSignature(
                name = m.name,
                parameters = m.parameterList.parameters.map { it.type.presentableText },
                returnType = m.returnType?.presentableText ?: "void"
            )
        }

        // Interface principale si existe
        val mainInterface = psiClass.interfaces.firstOrNull()
        val interfaceMethods = mainInterface?.methods?.map { m ->
            MethodSignature(
                name = m.name,
                parameters = m.parameterList.parameters.map { it.type.presentableText },
                returnType = m.returnType?.presentableText ?: "void"
            )
        } ?: emptyList()

        return ExternalClassInfo(
            className = psiClass.qualifiedName ?: psiClass.name ?: "Unknown",
            simpleName = psiClass.name ?: "Unknown",
            interfaceName = mainInterface?.qualifiedName,
            interfaceMethods = interfaceMethods,
            constructors = constructors,
            setters = setters,
            methods = methods,
            fields = psiClass.fields.map { VariableInfo(it.name, it.type.presentableText) }
        )
    }

    /**
     * Extrait les side-effects d'une methode (modifications de champs)
     * Essentiel pour savoir ce qu'il faut verifier dans les tests
     */
    fun extractSideEffects(method: PsiMethod, parentClass: PsiClass): List<SideEffect> {
        val sideEffects = mutableSetOf<SideEffect>()
        val methodBody = method.text
        val parentFields = parentClass.fields.associateBy { it.name }

        // Analyse statique du code source
        // Detecter les increments/decrements (champ++)
        val incrementPattern = Regex("(\\w+)\\+\\+")
        incrementPattern.findAll(methodBody).forEach { match ->
            val fieldName = match.groupValues[1]
            if (fieldName in parentFields.keys && fieldName !in setOf("i", "j", "k", "idx", "index")) {
                sideEffects.add(SideEffect(fieldName, "increment"))
            }
        }

        // Detecter les assignations de champs (this.champ =)
        val assignmentPattern = Regex("this\\.(\\w+)\\s*=")
        assignmentPattern.findAll(methodBody).forEach { match ->
            val fieldName = match.groupValues[1]
            if (fieldName in parentFields.keys) {
                sideEffects.add(SideEffect(fieldName, "assignment"))
            }
        }

        return sideEffects.toList()
    }

    /**
     * Crawl recursive des dependances d'une classe externe
     * Pour chaque dependance trouvee, extraire ses propres dependances
     */
    fun crawlDependencies(externalClassInfo: ExternalClassInfo, project: com.intellij.openapi.project.Project, visited: MutableSet<String> = mutableSetOf()): Set<ExternalClassInfo> {
        if (externalClassInfo.className in visited) return emptySet()
        visited.add(externalClassInfo.className)

        val dependencies = mutableSetOf<ExternalClassInfo>()

        // Analyser les types des constructeurs
        externalClassInfo.constructors.forEach { ctor ->
            ctor.parameters.forEach { param ->
                val paramType = extractBaseType(param.type)
                if (!isExcludedType(paramType) && paramType !in visited) {
                    val depInfo = extractExternalClassInfo(paramType, project)
                    if (depInfo != null) {
                        dependencies.add(depInfo)
                        // Recursion
                        dependencies.addAll(crawlDependencies(depInfo, project, visited))
                    }
                }
            }
        }

        // Analyser les types des methodes
        externalClassInfo.methods.forEach { method ->
            // Parametres
            method.parameters.forEach { paramType ->
                val baseType = extractBaseType(paramType)
                if (!isExcludedType(baseType) && baseType !in visited) {
                    val depInfo = extractExternalClassInfo(baseType, project)
                    if (depInfo != null) {
                        dependencies.add(depInfo)
                        dependencies.addAll(crawlDependencies(depInfo, project, visited))
                    }
                }
            }
            // Type de retour
            if (!isExcludedType(method.returnType)) {
                val returnType = extractBaseType(method.returnType)
                val depInfo = extractExternalClassInfo(returnType, project)
                if (depInfo != null) {
                    dependencies.add(depInfo)
                    dependencies.addAll(crawlDependencies(depInfo, project, visited))
                }
            }
        }

        return dependencies
    }

    /**
     * Extrait le type de base d'une declaration (ex: List<Item> -> Item, BigDecimal -> BigDecimal)
     */
    private fun extractBaseType(typeText: String): String {
        val genericRegex = Regex("<(.+?)>")
        return genericRegex.find(typeText)?.groupValues?.get(1)?.trim() ?: typeText
    }

    /**
     * Verifie si un type est exclu (JDK, primitives, etc.)
     */
    private fun isExcludedType(type: String): Boolean {
        val cleanType = type.trim()
        return cleanType.startsWith("java.") ||
                cleanType.startsWith("javax.") ||
                cleanType in setOf("int", "long", "double", "float", "boolean", "char", "byte", "short") ||
                cleanType in setOf("Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short", "String", "Object", "List", "ArrayList", "BigDecimal", "Map", "HashMap", "Set", "HashSet", "Optional")
    }
}

/**
 * Informations sur une classe externe (pour le mocking)
 */
data class ExternalClassInfo(
    val className: String,
    val simpleName: String,
    val interfaceName: String?,
    val interfaceMethods: List<MethodSignature>,
    val constructors: List<ConstructorInfo>,
    val setters: List<SetterInfo>,
    val methods: List<MethodSignature>,
    val fields: List<VariableInfo>
)

/**
 * Informations sur un setter
 */
data class SetterInfo(
    val name: String,
    val parameterType: String,
    val fieldName: String
)
