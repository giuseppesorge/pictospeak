package io.github.giuseppesorge.pictospeak.nlg.lang.it

/**
 * Preposizioni articolate (docs/grammar-v1.md item 5): di/a/da/in/su contract with the
 * definite article; per/tra/fra never contract. Not yet used by the v1 patterns — kept
 * here (tested) because it is part of the frozen grammar scope and future patterns
 * (e.g. destinations) will need it.
 */
object PrepositionContractor {
    private val TABLE: Map<String, Map<String, String>> =
        mapOf(
            "di" to
                mapOf(
                    "il" to "del",
                    "lo" to "dello",
                    "la" to "della",
                    "l'" to "dell'",
                    "i" to "dei",
                    "gli" to "degli",
                    "le" to "delle",
                ),
            "a" to
                mapOf(
                    "il" to "al",
                    "lo" to "allo",
                    "la" to "alla",
                    "l'" to "all'",
                    "i" to "ai",
                    "gli" to "agli",
                    "le" to "alle",
                ),
            "da" to
                mapOf(
                    "il" to "dal",
                    "lo" to "dallo",
                    "la" to "dalla",
                    "l'" to "dall'",
                    "i" to "dai",
                    "gli" to "dagli",
                    "le" to "dalle",
                ),
            "in" to
                mapOf(
                    "il" to "nel",
                    "lo" to "nello",
                    "la" to "nella",
                    "l'" to "nell'",
                    "i" to "nei",
                    "gli" to "negli",
                    "le" to "nelle",
                ),
            "su" to
                mapOf(
                    "il" to "sul",
                    "lo" to "sullo",
                    "la" to "sulla",
                    "l'" to "sull'",
                    "i" to "sui",
                    "gli" to "sugli",
                    "le" to "sulle",
                ),
        )

    /**
     * Contracts [preposition] with [article], or returns them side by side when the
     * preposition does not contract (per/tra/fra) or the pair is unknown.
     */
    fun contract(
        preposition: String,
        article: String,
    ): String = TABLE[preposition.lowercase()]?.get(article.lowercase()) ?: "$preposition $article"
}
