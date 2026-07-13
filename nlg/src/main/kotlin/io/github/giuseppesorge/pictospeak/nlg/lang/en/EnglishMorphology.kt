package io.github.giuseppesorge.pictospeak.nlg.lang.en

/**
 * English article selection — the only English morphology decided at realization time
 * (all inflected verb/noun forms are precomputed in the lexicon). The a/an choice depends
 * on the SOUND of the following word, so genuine exceptions ("an hour", "a university")
 * are carried as an explicit override on the entry; otherwise the vowel-letter rule holds.
 */
object EnglishMorphology {
    private val VOWEL_LETTERS = setOf('a', 'e', 'i', 'o', 'u')

    fun indefiniteArticle(
        nextWord: String,
        override: String?,
    ): String =
        override
            ?: if (nextWord.firstOrNull()?.lowercaseChar() in VOWEL_LETTERS) "an" else "a"
}
