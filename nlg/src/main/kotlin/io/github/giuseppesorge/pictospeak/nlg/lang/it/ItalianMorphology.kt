package io.github.giuseppesorge.pictospeak.nlg.lang.it

/**
 * Italian article selection and agreement — pure phonological/morphological rules
 * (docs/grammar-v1.md items 4-6). Word-specific facts stay in the lexicon; only
 * rule-derivable behavior lives here.
 */
object ItalianMorphology {
    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u', 'à', 'è', 'é', 'ì', 'ò', 'ù')

    /** lo/gli (and uno) contexts: s+consonant, z, gn, ps, pn, x, y. */
    fun requiresLoArticle(word: String): Boolean {
        val w = word.lowercase()
        if (w.isEmpty()) return false
        return w.startsWith("z") ||
            w.startsWith("gn") ||
            w.startsWith("ps") ||
            w.startsWith("pn") ||
            w.startsWith("x") ||
            w.startsWith("y") ||
            (w.length >= 2 && w[0] == 's' && w[1] !in VOWELS)
    }

    fun startsWithVowel(word: String): Boolean = word.firstOrNull()?.lowercaseChar() in VOWELS

    /**
     * Definite article joined to the word (elided forms attach directly: "l'acqua").
     */
    fun withDefiniteArticle(
        word: String,
        gender: Gender,
        plural: Boolean,
    ): String =
        when {
            gender == Gender.FEMININE && plural -> "le $word"
            gender == Gender.FEMININE && startsWithVowel(word) -> "l'$word"
            gender == Gender.FEMININE -> "la $word"
            plural && (requiresLoArticle(word) || startsWithVowel(word)) -> "gli $word"
            plural -> "i $word"
            requiresLoArticle(word) -> "lo $word"
            startsWithVowel(word) -> "l'$word"
            else -> "il $word"
        }

    /** Indefinite article joined to the word (singular only: "un'amica"). */
    fun withIndefiniteArticle(
        word: String,
        gender: Gender,
    ): String =
        when {
            gender == Gender.FEMININE && startsWithVowel(word) -> "un'$word"
            gender == Gender.FEMININE -> "una $word"
            requiresLoArticle(word) -> "uno $word"
            else -> "un $word"
        }

    /** The bare definite article for [word], used by [PrepositionContractor]. */
    fun definiteArticle(
        word: String,
        gender: Gender,
        plural: Boolean,
    ): String = withDefiniteArticle(word, gender, plural).substringBefore(word).trim().ifEmpty { "l'" }

    /**
     * Past-participle agreement (essere auxiliary): regular -o participles inflect like
     * -o adjectives; anything else is left untouched (never guess).
     */
    fun agreeParticiple(
        participle: String,
        feminine: Boolean,
        plural: Boolean,
    ): String {
        if (!participle.endsWith("o")) return participle
        val stem = participle.dropLast(1)
        return stem +
            when {
                feminine && plural -> "e"
                plural -> "i"
                feminine -> "a"
                else -> "o"
            }
    }

    /**
     * Adjective agreement from the four lexicon forms.
     */
    fun agreeAdjective(
        adjective: AdjectiveEntry,
        feminine: Boolean,
        plural: Boolean,
    ): String =
        when {
            feminine && plural -> adjective.femininePlural
            feminine -> adjective.feminineSingular
            plural -> adjective.masculinePlural
            else -> adjective.masculineSingular
        }
}
