package fr.conscience.numerique.util

private const val MAX_LENGTH = 60

/** Nettoie la saisie d'une problématique personnalisée ; null si elle est vide. */
fun normalizeCustomProblem(raw: String): String? =
    raw.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH).trim().takeIf { it.isNotEmpty() }
