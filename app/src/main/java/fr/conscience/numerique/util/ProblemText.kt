package fr.conscience.numerique.util

/** Longueur maximale d'une problématique personnalisée. */
const val MAX_CUSTOM_LENGTH = 60

/**
 * Longueur maximale quand on renomme une problématique du catalogue : plus large, car certains intitulés d'origine
 * dépassent [MAX_CUSTOM_LENGTH] (jusqu'à 70 caractères) et ne doivent pas être coupés en plein mot.
 */
const val MAX_CATALOG_LENGTH = 120

/** Nettoie la saisie d'une problématique ; null si elle est vide. [maxLength] : au-delà, le texte est coupé. */
fun normalizeCustomProblem(raw: String, maxLength: Int = MAX_CUSTOM_LENGTH): String? =
    raw.trim().replace(Regex("\\s+"), " ").take(maxLength).trim().takeIf { it.isNotEmpty() }
