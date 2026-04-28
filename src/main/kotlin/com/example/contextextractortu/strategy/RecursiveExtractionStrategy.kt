package com.example.contextextractortu.strategy

import com.example.contextextractortu.collector.PsiScanner
import com.example.contextextractortu.model.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

/**
 * Strategie d'extraction de contexte basee sur un algorithme recursif descendant
 * selon le pseudo-code du prompt.txt
 *
 * Algorithme ExplorerContexte(SUT, methodeCible) avec 5 modes :
 * - SUT_BOOTSTRAP : Point d'entree, analyse complete de la methode cible
 * - INTERNAL_LOGIC : Exploration du corps des sous-methodes internes
 * - MOCK_EXTERNAL : Capture des signatures pour les dependances a mocker
 * - DATA_STRUCTURE : Compréhension de la construction des DTOs/objets de donnees
 * - SYSTEM_IGNORE : Arrêt pur pour les classes système
 */
class RecursiveExtractionStrategy(
    private val scanner: PsiScanner,
    private val project: Project
) {

    private val visited = mutableSetOf<String>()
    private val result = RecursiveContextResult()

    /**
     * Point d'entree : execute l'extraction recursive
     */
    fun execute(editor: Editor): RecursiveContextResult {
        visited.clear()
        result.hierarchy.clear()
        result.fields.clear()
        result.internalMethods.clear()
        result.mocks.clear()
        result.dataStructures.clear()
        result.targetMethod = null
        result.instantiationPlan = InstantiationPlan()

        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return result
        val offset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(offset)
        val targetMethod = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod::class.java) ?: return result
        val parentClass = targetMethod.containingClass ?: return result

        // Mode SUT_BOOTSTRAP
        recurse(parentClass, targetMethod, ExtractionMode.SUT_BOOTSTRAP)

        return result
    }

    /**
     * Fonction recursive principale selon l'algorithme
     */
    fun recurse(psiClass: PsiClass, method: PsiMethod?, mode: ExtractionMode) {
        when (mode) {
            ExtractionMode.SUT_BOOTSTRAP -> handleSutBootstrap(psiClass, method)
            ExtractionMode.INTERNAL_LOGIC -> handleInternalLogic(psiClass, method)
            ExtractionMode.MOCK_EXTERNAL -> handleMockExternal(psiClass, method)
            ExtractionMode.DATA_STRUCTURE -> handleDataStructure(psiClass)
            ExtractionMode.SYSTEM_IGNORE -> { /* Arrêt pur */ }
        }
    }

    /**
     * Mode SUT_BOOTSTRAP - Point d'entree unique
     * Comprendre comment instancier le SUT et identifier toutes ses dependances
     */
    private fun handleSutBootstrap(sut: PsiClass, method: PsiMethod?) {
        val visitKey = sut.qualifiedName ?: sut.name ?: return
        if (visitKey in visited) return
        visited.add(visitKey)

        if (method == null) return

        // BLOC 1 : HIERARCHIE
        captureHierarchy(sut)

        // BLOC 2 : ANALYSE COMPLETE DE method
        val methodAnalysis = analyzeTargetMethod(method)
        result.targetMethod = methodAnalysis

        // BLOC 3 : INVENTAIRE DES CHAMPS
        captureFields(sut, method)

        // BLOC 4 : PROTOCOLE DE CONSTRUCTION
        captureInstantiationPlan(sut)

        // BLOC 5 : LEVIERS DE MUTATION
        captureSetters(sut)

        // BLOC 6 : POINTS D'ENTREE RECURSIFS

        // 6a. Appel handleInternalLogic pour detecter les appels internes recursivement
        handleInternalLogic(sut, method)

        // 6b. Dependances utiles a la construction
        result.instantiationPlan.constructorParams.forEach { param ->
            val mode = classify(param.type.rawType)
            if (mode != ExtractionMode.SYSTEM_IGNORE) {
                resolveClass(param.type.rawType)?.let { clazz ->
                    recurse(clazz, null, mode)
                }
            }
        }

        // 6c. Parametres de method
        method.parameterList.parameters.forEach { param ->
            val type = ResolvedType(param.type.presentableText)
            val mode = classify(type.rawType)
            if (mode != ExtractionMode.SYSTEM_IGNORE) {
                resolveClass(type.rawType)?.let { clazz ->
                    recurse(clazz, null, mode)
                }
            }
        }

        // 6d. Type de retour
        method.returnType?.let { returnType ->
            val type = ResolvedType(returnType.presentableText)
            if (classify(type.rawType) == ExtractionMode.DATA_STRUCTURE) {
                resolveClass(type.rawType)?.let { clazz ->
                    recurse(clazz, null, ExtractionMode.DATA_STRUCTURE)
                }
            }
        }
    }

    /**
     * Mode INTERNAL_LOGIC - Explorer le corps d'une sous-methode interne
     */
    private fun handleInternalLogic(psiClass: PsiClass, method: PsiMethod?) {
        if (method == null) return

        val visitKey = "${psiClass.qualifiedName}#${method.name}"
        if (visitKey in visited) return
        visited.add(visitKey)

        // Phase 1 : Capture du Corps
        val analysis = InternalMethodAnalysis(
            visitKey = visitKey,
            signature = buildMethodSignatureText(method),
            body = method.text
        )

        result.internalMethods[visitKey] = analysis

        // Phase 2 : Scan des Appels
        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val resolved = expression.resolveMethod() ?: return
                val calledMethod = resolved
                val targetClass = resolved.containingClass ?: return

                // Verifier si c'est un appel interne (meme classe ou hierarchie)
                val hierarchyNames = getHierarchyClassNames(psiClass)
                val isInternalCall = hierarchyNames.contains(targetClass.qualifiedName) ||
                        hierarchyNames.contains(targetClass.name)

                // Debug logging
                System.err.println("[DEBUG] Appel metode: ${calledMethod.name}, targetClass: ${targetClass.qualifiedName}, isInternal: $isInternalCall, hierarchy: $hierarchyNames")

                if (isInternalCall) {
                    // Appel interne - recursion avec INTERNAL_LOGIC
                    System.err.println("[DEBUG] Recursion INTERNAL_LOGIC sur ${targetClass.qualifiedName}#${calledMethod.name}")
                    recurse(targetClass, calledMethod, ExtractionMode.INTERNAL_LOGIC)
                } else {
                    // Appel externe - classifier et recursir selon le mode
                    val mode = classify(targetClass.qualifiedName ?: "")
                    System.err.println("[DEBUG] Appel externe: ${targetClass.qualifiedName}, mode: $mode")
                    when (mode) {
                        ExtractionMode.MOCK_EXTERNAL -> {
                            recurse(targetClass, calledMethod, ExtractionMode.MOCK_EXTERNAL)
                        }
                        ExtractionMode.DATA_STRUCTURE -> {
                            recurse(targetClass, null, ExtractionMode.DATA_STRUCTURE)
                        }
                        ExtractionMode.SYSTEM_IGNORE, ExtractionMode.SUT_BOOTSTRAP, ExtractionMode.INTERNAL_LOGIC -> {}
                    }
                }
            }
        })

        // Phase 3 : Exceptions - extraction simplifiee depuis la signature
        val throwsList = method.throwsList.referencedTypes.map { it.presentableText }
        result.internalMethods[visitKey] = analysis.copy(exceptionsThrown = throwsList)

        // Phase 4 & 5 : Variables locales et type de retour (simplifie)
        val returnType = method.returnType?.presentableText
        if (returnType != null && classify(returnType) == ExtractionMode.DATA_STRUCTURE) {
            resolveClass(returnType)?.let { clazz ->
                recurse(clazz, null, ExtractionMode.DATA_STRUCTURE)
            }
        }
    }

    /**
     * Mode MOCK_EXTERNAL - Capturer ce dont le test a besoin pour configurer le mock
     */
    private fun handleMockExternal(psiClass: PsiClass, method: PsiMethod?) {
        val visitKey = "MOCK#${psiClass.qualifiedName}#${method?.name ?: "CLASS"}"
        if (visitKey in visited) return
        visited.add(visitKey)

        // Phase 1 : Resolution de l'Interface
        val interfaceName = psiClass.interfaces.firstOrNull()?.qualifiedName

        // Phase 2 : Capture de la Signature de la Methode Appelee
        val requiredSignatures = mutableListOf<String>()
        if (method != null) {
            requiredSignatures.add(buildMethodSignatureText(method))
        }

        // Phase 3 & 4 : Info construction du mock
        val mockInfo = MockInfo(
            concreteClass = psiClass.qualifiedName ?: psiClass.name ?: "",
            interfaceName = interfaceName,
            classAnnotations = psiClass.annotations.mapNotNull { it.qualifiedName },
            requiredSignatures = requiredSignatures
        )

        result.mocks[mockInfo.concreteClass] = mockInfo

        // STOP : Ne jamais lire le corps des methodes externes
    }

    /**
     * Mode DATA_STRUCTURE - Comprendre comment construire un objet de donnees
     */
    private fun handleDataStructure(psiClass: PsiClass) {
        val visitKey = "DATA#${psiClass.qualifiedName ?: psiClass.name}"
        if (visitKey in visited) return
        visited.add(visitKey)

        // Phase 1 : Detection du Pattern de Construction
        val pattern = detectConstructionPattern(psiClass)

        // Phase 2 : Inventaire des Champs
        val dataFields = psiClass.fields.map { field ->
            DataField(
                name = field.name,
                type = ResolvedType(field.type.presentableText),
                annotations = field.annotations.mapNotNull { it.qualifiedName },
                defaultValue = field.initializer?.text?.removeSurrounding("\"")
            )
        }

        // Phase 3 & 4 : Recursion sur les champs complexes
        val collections = mutableListOf<CollectionInfo>()
        dataFields.forEach { field ->
            if (classify(field.type.rawType) == ExtractionMode.DATA_STRUCTURE) {
                resolveClass(field.type.rawType)?.let { clazz ->
                    recurse(clazz, null, ExtractionMode.DATA_STRUCTURE)
                }
            }
            // Detection des collections
            if (field.type.isCollection) {
                field.type.typeArguments.firstOrNull()?.let { elementType ->
                    if (classify(elementType.rawType) == ExtractionMode.DATA_STRUCTURE) {
                        resolveClass(elementType.rawType)?.let { clazz ->
                            recurse(clazz, null, ExtractionMode.DATA_STRUCTURE)
                        }
                    }
                    collections.add(CollectionInfo(
                        fieldName = field.name,
                        collectionType = field.type.rawType,
                        elementType = elementType
                    ))
                }
            }
        }

        val dataStructureInfo = DataStructureInfo(
            className = psiClass.qualifiedName ?: psiClass.name ?: "",
            constructionPattern = pattern,
            fields = dataFields,
            collections = collections
        )

        result.dataStructures[dataStructureInfo.className] = dataStructureInfo
    }

    // ==================== UTILITAIRES ====================

    /**
     * Capture la hierarchie complete de la classe
     */
    private fun captureHierarchy(sut: PsiClass) {
        var currentClass: PsiClass? = sut
        while (currentClass != null && currentClass.name != "Object") {
            val level = HierarchyLevel(
                className = currentClass.qualifiedName ?: currentClass.name ?: "",
                annotations = currentClass.annotations.mapNotNull { it.qualifiedName }
            )
            result.hierarchy.add(level)
            currentClass = currentClass.superClass
        }
    }

    /**
     * Analyse complete de la methode cible
     */
    private fun analyzeTargetMethod(method: PsiMethod): TargetMethodAnalysis {
        // Extraction simplifiee des exceptions depuis la signature
        val exceptionsThrown = method.throwsList.referencedTypes.map { it.presentableText }

        return TargetMethodAnalysis(
            signature = buildMethodSignatureText(method),
            exceptionsThrown = exceptionsThrown,
            exceptionsCaught = emptyList(),
            sourceCode = method.text
        )
    }

    /**
     * Construit la signature de methode sous forme de String
     */
    private fun buildMethodSignatureText(method: PsiMethod): String {
        val modifiers = method.modifierList?.text?.takeIf { it.isNotBlank() } ?: ""
        val returnType = method.returnType?.presentableText ?: "void"
        val paramNameList = method.parameterList.parameters.joinToString(", ") { param ->
            "${param.type.presentableText} ${param.name ?: "arg"}"
        }
        val throwsClause = if (method.throwsList.referencedTypes.isNotEmpty()) {
            " throws ${method.throwsList.referencedTypes.joinToString(", ") { it.presentableText }}"
        } else ""
        return "$modifiers $returnType ${method.name}($paramNameList)$throwsClause"
    }

    /**
     * Capture les champs utilises dans la methode
     */
    private fun captureFields(sut: PsiClass, method: PsiMethod) {
        val fieldsUsed = mutableSetOf<String>()

        // Trouver les champs utilises dans la methode
        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                super.visitReferenceExpression(expression)
                val resolved = expression.resolve()
                if (resolved is PsiField && resolved.containingClass == sut) {
                    fieldsUsed.add(resolved.name)
                }
            }
        })

        // Capturer les champs pertinents (utilises ou injectes)
        val injectAnnotations = setOf("Autowired", "Inject", "Resource", "Value")
        collectAllFields(sut).forEach { field ->
            val isUsed = field.name in fieldsUsed
            val isInjected = field.annotations.any { ann ->
                ann.qualifiedName?.any { injectAnnotations.any { it in it } } ?: false
            }

            if (isUsed || isInjected) {
                result.fields.add(Field(
                    name = field.name,
                    type = ResolvedType(field.type.presentableText),
                    visibility = getVisibility(field.modifierList),
                    annotations = field.annotations.mapNotNull { it.qualifiedName }
                ))
            }
        }
    }

    /**
     * Capture le plan d'instanciation
     */
    private fun captureInstantiationPlan(sut: PsiClass) {
        val constructors = sut.constructors.toList()

        // Choisir le constructeur : @Autowired prioritaire, sinon le plus complet
        val chosenConstructor = constructors.find { ctor ->
            ctor.annotations.any { ann -> ann.qualifiedName?.contains("Autowired") == true }
        } ?: constructors.maxByOrNull { it.parameterList.parameters.size }

        val params = mutableListOf<SimpleParameter>()
        chosenConstructor?.let { ctor ->
            params.addAll(ctor.parameterList.parameters.map { param ->
                SimpleParameter(
                    name = param.name ?: "arg",
                    type = ResolvedType(param.type.presentableText),
                    annotations = param.annotations.mapNotNull { it.qualifiedName }
                )
            })

            // Capturer appel super() si present
            val body = ctor.body?.text ?: ""
            if (body.startsWith("super(")) {
                result.instantiationPlan = result.instantiationPlan.copy(
                    superValues = listOf("/* valeurs super() a extraire */")
                )
            }
        }

        result.instantiationPlan = result.instantiationPlan.copy(
            constructorParams = params
        )
    }

    /**
     * Capture les setters disponibles
     */
    private fun captureSetters(sut: PsiClass) {
        val setters = sut.methods.filter { method ->
            method.name.startsWith("set") &&
                    method.parameterList.parameters.size == 1
        }

        val setterList = setters.map { method ->
            Setter(
                methodName = method.name,
                fieldName = method.name.removePrefix("set").replaceFirstChar { it.lowercase() },
                type = ResolvedType(method.parameterList.parameters.first().type.presentableText)
            )
        }

        val postConstructMethods = sut.methods
            .filter { it.annotations.any { ann -> ann.qualifiedName?.contains("PostConstruct") == true } }
            .map { it.name }

        result.instantiationPlan = result.instantiationPlan.copy(
            setters = setterList,
            postConstructMethods = postConstructMethods
        )
    }

    /**
     * Extrait les appels de methode depuis un corps de methode
     */
    private fun extractCallsFromMethod(method: PsiMethod): List<MethodCall> {
        val calls = mutableSetOf<MethodCall>()

        method.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val resolved = expression.resolveMethod()
                resolved?.containingClass?.let { clazz ->
                    calls.add(MethodCall(
                        className = clazz.qualifiedName ?: clazz.name ?: "",
                        methodName = resolved.name
                    ))
                }
            }
        })

        return calls.toList()
    }

    /**
     * Classifier une classe selon son type
     */
    private fun classify(className: String): ExtractionMode {
        // SYSTEM_IGNORE : packages systeme
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return ExtractionMode.SYSTEM_IGNORE
        }
        if (className in setOf("int", "long", "double", "float", "boolean", "String", "Object")) {
            return ExtractionMode.SYSTEM_IGNORE
        }

        // Resoudre la classe
        val psiClass = resolveClass(className) ?: return ExtractionMode.SYSTEM_IGNORE

        // MOCK_EXTERNAL : annotations Spring
        val springAnnotations = setOf("Service", "Repository", "Component", "Controller")
        if (psiClass.annotations.any { ann ->
            ann.qualifiedName?.any { springAnnotations.any { it in it } } ?: false
        }) {
            return ExtractionMode.MOCK_EXTERNAL
        }

        // Convention de nommage
        when {
            className.endsWith("Service") || className.endsWith("Repository") ||
                    className.endsWith("Manager") || className.endsWith("Client") ->
                return ExtractionMode.MOCK_EXTERNAL

            className.endsWith("DTO") || className.endsWith("Entity") ||
                    className.endsWith("Request") || className.endsWith("Response") ||
                    className.endsWith("Record") ->
                return ExtractionMode.DATA_STRUCTURE
        }

        // Par defaut : DATA_STRUCTURE pour les objets de donnees
        return ExtractionMode.DATA_STRUCTURE
    }

    /**
     * Detecter le pattern de construction d'une classe
     */
    private fun detectConstructionPattern(psiClass: PsiClass): ConstructionPattern {
        // Record Java
        if (psiClass.isRecord) {
            return ConstructionPattern.RECORD
        }

        // Builder pattern
        if (psiClass.methods.any { it.name == "builder" && it.hasModifierProperty(PsiModifier.STATIC) }) {
            return ConstructionPattern.BUILDER
        }

        // Constructor
        if (psiClass.constructors.any { it.parameterList.parameters.isNotEmpty() }) {
            return ConstructionPattern.CONSTRUCTOR
        }

        // Setter-based
        return ConstructionPattern.SETTER_BASED
    }

    /**
     * Resoudre une classe a partir de son nom
     */
    private fun resolveClass(className: String): PsiClass? {
        val facade = JavaPsiFacade.getInstance(project)
        return facade.findClass(className, com.intellij.psi.search.GlobalSearchScope.projectScope(project))
    }

    /**
     * Collecter tous les champs (classe + super-classes)
     */
    private fun collectAllFields(psiClass: PsiClass): List<PsiField> {
        val fields = mutableListOf<PsiField>()
        var current: PsiClass? = psiClass

        while (current != null) {
            fields.addAll(current.fields)
            current = current.superClass
        }

        return fields
    }

    /**
     * Obtenir les noms de classes de la hierarchie
     */
    private fun getHierarchyClassNames(sut: PsiClass): Set<String> {
        val names = mutableSetOf<String>()
        var current: PsiClass? = sut

        while (current != null) {
            current.qualifiedName?.let { names.add(it) }
            current.name?.let { names.add(it) }
            current = current.superClass
        }

        return names
    }

    /**
     * Obtenir la visibilite d'un element
     */
    private fun getVisibility(modifierList: PsiModifierList?): String {
        return when {
            modifierList?.hasModifierProperty(PsiModifier.PUBLIC) == true -> "public"
            modifierList?.hasModifierProperty(PsiModifier.PROTECTED) == true -> "protected"
            modifierList?.hasModifierProperty(PsiModifier.PRIVATE) == true -> "private"
            else -> "package-private"
        }
    }

    /**
     * Data class pour les appels de methode
     */
    data class MethodCall(
        val className: String,
        val methodName: String
    )
}
