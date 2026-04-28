package com.example.contextextractortu.model

/**
 * Modes d'extraction selon l'algorithme recursif
 */
enum class ExtractionMode {
    SUT_BOOTSTRAP,      // Point d'entree - analyse complete
    INTERNAL_LOGIC,     // Methodes internes du SUT
    MOCK_EXTERNAL,      // Dependances a mocker
    DATA_STRUCTURE,     // DTOs, Entities, POJOs
    SYSTEM_IGNORE       // Classes systeme - arret
}
