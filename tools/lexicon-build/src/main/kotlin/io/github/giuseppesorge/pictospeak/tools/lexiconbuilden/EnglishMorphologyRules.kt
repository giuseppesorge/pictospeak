package io.github.giuseppesorge.pictospeak.tools.lexiconbuilden

/**
 * Regular English inflection spelling rules. Irregular forms are supplied by curated data
 * (irregular_en.csv) and override these; genuine doublers not covered here are listed as
 * irregulars too.
 */
object EnglishMorphologyRules {
    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')
    private val SIBILANT_ENDINGS = listOf("s", "x", "z", "ch", "sh")

    /** 3rd person singular present: base+s, +es after sibilants/o, consonant+y -> ies. */
    fun thirdSingular(base: String): String =
        when {
            consonantY(base) -> base.dropLast(1) + "ies"
            base.endsWith("o") || SIBILANT_ENDINGS.any { base.endsWith(it) } -> base + "es"
            else -> base + "s"
        }

    /** Regular past / past participle: base+ed, e-drop, consonant+y -> ied, CVC doubling. */
    fun past(base: String): String =
        when {
            base.endsWith("e") -> base + "d"
            consonantY(base) -> base.dropLast(1) + "ied"
            doublesFinalConsonant(base) -> base + base.last() + "ed"
            else -> base + "ed"
        }

    /** Regular plural: base+s, +es after sibilants, consonant+y -> ies. */
    fun plural(base: String): String =
        when {
            consonantY(base) -> base.dropLast(1) + "ies"
            SIBILANT_ENDINGS.any { base.endsWith(it) } -> base + "es"
            else -> base + "s"
        }

    private fun consonantY(word: String): Boolean =
        word.endsWith("y") && word.length >= 2 && word[word.length - 2].lowercaseChar() !in VOWELS

    // Monosyllabic consonant-vowel-consonant words double the final consonant (stop->stopped),
    // excluding final w/x/y. A conservative check adequate for the AAC core vocabulary.
    @Suppress("MagicNumber") // CVC positional indices
    private fun doublesFinalConsonant(word: String): Boolean {
        if (word.length < 3) return false
        val a = word[word.length - 3].lowercaseChar()
        val b = word[word.length - 2].lowercaseChar()
        val c = word[word.length - 1].lowercaseChar()
        return a !in VOWELS && b in VOWELS && c !in VOWELS && c !in setOf('w', 'x', 'y') && vowelCount(word) == 1
    }

    private fun vowelCount(word: String): Int = word.count { it.lowercaseChar() in VOWELS }
}
