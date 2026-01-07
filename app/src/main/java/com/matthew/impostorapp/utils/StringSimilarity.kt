package com.matthew.impostorapp.utils

import java.text.Normalizer

/**
 * Utilidad para comparar similitud entre strings.
 * Útil para detectar errores tipográficos y variaciones en nombres.
 */
object StringSimilarity {

    /**
     * Normaliza texto removiendo acentos y convirtiendo a minúsculas
     */
    fun normalizeText(text: String): String {
        val normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        return normalized.replace("\\p{M}".toRegex(), "").lowercase()
    }

    /**
     * Calcula la distancia de Levenshtein entre dos strings.
     * Retorna el número mínimo de ediciones (inserción, eliminación, sustitución)
     * necesarias para transformar s1 en s2.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Matriz para programación dinámica
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // Inicializar primera fila y columna
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // Llenar la matriz
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // eliminación
                    dp[i][j - 1] + 1,      // inserción
                    dp[i - 1][j - 1] + cost // sustitución
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Calcula el porcentaje de similitud entre dos strings (0.0 a 1.0)
     * Basado en la distancia de Levenshtein normalizada.
     *
     * 1.0 = idénticos
     * 0.0 = completamente diferentes
     */
    fun similarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLength = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)

        return 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Verifica si dos strings son similares según un umbral.
     *
     * @param s1 Primer string
     * @param s2 Segundo string
     * @param threshold Umbral de similitud (0.0 a 1.0). Por defecto 0.85
     * @return true si la similitud es mayor o igual al umbral
     */
    fun isSimilar(s1: String, s2: String, threshold: Double = 0.85): Boolean {
        return similarity(s1, s2) >= threshold
    }

    /**
     * Busca strings similares en una lista.
     *
     * @param target String objetivo
     * @param candidates Lista de candidatos
     * @param threshold Umbral de similitud
     * @return Lista de strings similares encontrados
     */
    fun findSimilar(
        target: String,
        candidates: List<String>,
        threshold: Double = 0.85
    ): List<String> {
        val normalizedTarget = normalizeText(target)

        return candidates.filter { candidate ->
            val normalizedCandidate = normalizeText(candidate)

            // Verificar igualdad exacta primero (más eficiente)
            if (normalizedTarget == normalizedCandidate) {
                return@filter true
            }

            // Luego verificar similitud
            isSimilar(normalizedTarget, normalizedCandidate, threshold)
        }
    }

    /**
     * Encuentra la string más similar de una lista.
     *
     * @return Pair con el string más similar y su score de similitud, o null si no hay candidatos
     */
    fun findMostSimilar(target: String, candidates: List<String>): Pair<String, Double>? {
        if (candidates.isEmpty()) return null

        val normalizedTarget = normalizeText(target)

        return candidates
            .map { candidate ->
                val normalizedCandidate = normalizeText(candidate)
                candidate to similarity(normalizedTarget, normalizedCandidate)
            }
            .maxByOrNull { it.second }
    }
}