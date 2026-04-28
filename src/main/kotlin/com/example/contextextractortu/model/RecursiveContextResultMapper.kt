package com.example.contextextractortu.model

import com.example.contextextractortu.model.*

/**
 * Mapper qui transforme RecursiveContextResult en GenericContextModel
 * Adapte le resultat de la strategie recursive au systeme de template existant
 */
object RecursiveContextResultMapper {

    /**
     * Mappe un RecursiveContextResult vers GenericContextModel
     */
    fun map(result: RecursiveContextResult): GenericContextModel {
        val model = GenericContextModel()

        // --- Hierarchie ---
        if (result.hierarchy.isNotEmpty()) {
            val hierarchyText = result.hierarchy.joinToString("\n") { level ->
                "- `${level.className}` ${if (level.annotations.isNotEmpty()) "[${level.annotations.joinToString(",")}]" else ""}"
            }
            model.items.add(ContextItem(
                title = "hierarchy",
                content = hierarchyText,
                type = ContextType.METADATA
            ))
        }

        // --- Methode cible ---
        result.targetMethod?.let { target ->
            model.items.add(ContextItem(
                title = "targetMethodSignature",
                content = target.signature,
                type = ContextType.METADATA
            ))

            model.items.add(ContextItem(
                title = "targetMethod",
                content = target.sourceCode,
                type = ContextType.CODE
            ))

            if (target.exceptionsThrown.isNotEmpty()) {
                model.items.add(ContextItem(
                    title = "exceptionsThrown",
                    content = target.exceptionsThrown.joinToString("\n") { "- `$it`" },
                    type = ContextType.METADATA
                ))
            }

            if (target.exceptionsCaught.isNotEmpty()) {
                val caughtText = target.exceptionsCaught.joinToString("\n") { exc ->
                    "- `$exc`"
                }
                model.items.add(ContextItem(
                    title = "exceptionsCaught",
                    content = caughtText,
                    type = ContextType.METADATA
                ))
            }
        }

        // --- Champs ---
        if (result.fields.isNotEmpty()) {
            val fieldsText = result.fields.joinToString("\n") { field ->
                "- `${field.visibility}` ${field.type.rawType} ${field.name}${if (field.annotations.isNotEmpty()) " [${field.annotations.joinToString(",")}]" else ""}"
            }
            model.items.add(ContextItem(
                title = "classFields",
                content = fieldsText,
                type = ContextType.CODE
            ))
        }

        // --- Constructeur ---
        if (result.instantiationPlan.constructorParams.isNotEmpty()) {
            val ctorParams = result.instantiationPlan.constructorParams.joinToString(", ") { param ->
                "${param.type.rawType} ${param.name}"
            }
            model.items.add(ContextItem(
                title = "constructor",
                content = "constructor($ctorParams)",
                type = ContextType.CODE
            ))
        }

        // --- Appel parent ---
        if (result.instantiationPlan.superValues.isNotEmpty()) {
            model.items.add(ContextItem(
                title = "parentConstructorCall",
                content = result.instantiationPlan.superValues.joinToString("\n"),
                type = ContextType.CODE
            ))
        }

        // --- Setters ---
        if (result.instantiationPlan.setters.isNotEmpty()) {
            val settersText = result.instantiationPlan.setters.joinToString("\n") { setter ->
                "- `${setter.methodName}(${setter.type.rawType})`"
            }
            model.items.add(ContextItem(
                title = "settersNeeded",
                content = settersText,
                type = ContextType.CODE
            ))
        }

        // --- @PostConstruct ---
        if (result.instantiationPlan.postConstructMethods.isNotEmpty()) {
            model.items.add(ContextItem(
                title = "postConstructMethods",
                content = result.instantiationPlan.postConstructMethods.joinToString("\n") { "- `$it`" },
                type = ContextType.METADATA
            ))
        }

        // --- Methodes internes ---
        if (result.internalMethods.isNotEmpty()) {
            val internalText = buildInternalMethodsText(result.internalMethods)
            model.items.add(ContextItem(
                title = "internalMethods",
                content = internalText,
                type = ContextType.CODE
            ))
        }

        // --- Mocks ---
        if (result.mocks.isNotEmpty()) {
            val mocksText = buildMocksText(result.mocks)
            model.items.add(ContextItem(
                title = "externalDependencies",
                content = mocksText,
                type = ContextType.CODE
            ))
        }

        // --- Structures de donnees ---
        if (result.dataStructures.isNotEmpty()) {
            val dataText = buildDataStructuresText(result.dataStructures)
            model.items.add(ContextItem(
                title = "complexObjects",
                content = dataText,
                type = ContextType.CODE
            ))
        }

        return model
    }

    private fun buildInternalMethodsText(methods: Map<String, InternalMethodAnalysis>): String {
        return buildString {
            var index = 0
            methods.forEach { (_, method) ->
                if (index > 0) append("\n---\n\n")
                append("### ${method.visitKey}\n\n")
                append("```\n${method.body}\n```\n")
                index++
            }
        }
    }

    private fun buildMocksText(mocks: Map<String, MockInfo>): String {
        return buildString {
            append("## Dependances externes a mocker\n\n")
            mocks.forEach { (_, mock) ->
                append("### ${mock.concreteClass}\n\n")
                mock.interfaceName?.let { iface ->
                    append("Interface: `$iface`\n\n")
                }
                if (mock.classAnnotations.isNotEmpty()) {
                    append("Annotations: ${mock.classAnnotations.joinToString(", ")}\n\n")
                }
                if (mock.requiredSignatures.isNotEmpty()) {
                    append("Methodes a stubber:\n")
                    mock.requiredSignatures.forEach { sig ->
                        append("- `$sig`\n")
                    }
                    append("\n")
                }
            }
        }
    }

    private fun buildDataStructuresText(dataStructures: Map<String, DataStructureInfo>): String {
        return buildString {
            append("## Structures de donnees\n\n")
            dataStructures.forEach { (_, ds) ->
                append("### ${ds.className} [${ds.constructionPattern}]\n\n")
                if (ds.fields.isNotEmpty()) {
                    append("Champs:\n")
                    ds.fields.forEach { field ->
                        val annotations = if (field.annotations.isNotEmpty()) " [${field.annotations.joinToString(",")}]" else ""
                        append("- `${field.name}: ${field.type.rawType}$annotations`\n")
                    }
                    append("\n")
                }
                if (ds.collections.isNotEmpty()) {
                    append("Collections:\n")
                    ds.collections.forEach { coll ->
                        append("- `${coll.fieldName}: ${coll.collectionType}<${coll.elementType.rawType}>`\n")
                    }
                    append("\n")
                }
            }
        }
    }
}
