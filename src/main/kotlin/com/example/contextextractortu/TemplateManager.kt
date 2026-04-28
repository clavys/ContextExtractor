package com.example.contextextractortu

import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.util.Locale

/**
 * Gestionnaire de chargement des templates Markdown
 */
object TemplateManager {

    private val logger = Logger.getInstance(TemplateManager::class.java)
    private val templateCache = mutableMapOf<String, String>()

    /**
     * Charge un template depuis le dossier /templates/
     * @param name Nom du template sans extension (ex: "deep-unit-test")
     * @return Contenu du template ou null si non trouvé
     */
    fun loadTemplate(name: String): String? {
        val cacheKey = name.lowercase(Locale.ROOT)
        templateCache[cacheKey]?.let { return it }

        val path = "/templates/$name.md"
        return try {
            val content = javaClass.getResource(path)?.readText()
            if (content != null) {
                templateCache[cacheKey] = content
                content
            } else {
                logger.warn("Template not found: $path")
                null
            }
        } catch (e: IOException) {
            logger.warn("Error loading template: $path", e)
            null
        }
    }

    /**
     * Charge un template depuis le classpath avec encoding UTF-8
     */
    fun loadTemplateFromResource(resourcePath: String): String? {
        return try {
            javaClass.getResource(resourcePath)?.readText(Charsets.UTF_8)
        } catch (e: IOException) {
            logger.warn("Error loading resource: $resourcePath", e)
            null
        }
    }
}
