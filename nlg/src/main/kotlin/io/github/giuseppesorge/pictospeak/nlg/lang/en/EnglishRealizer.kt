package io.github.giuseppesorge.pictospeak.nlg.lang.en

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.lang.LanguageRealizer

/**
 * English realizer — the frozen v1 scope for English (docs/grammar-v1.md), which is a
 * DIFFERENT grammar from Italian: prenominal adjectives ("the big pizza"), do-support
 * negation ("he does not eat"), a "to"-infinitive after `want` but a bare infinitive after
 * the modals `can`/`must`, and the irregular copula (am/is/are).
 *
 * Sentence shapes (S = subject pronoun or leading noun; parentheses = optional):
 *   (S) want (not) to INFINITIVE (OBJECT)   "I want to eat pizza" / "he does not want to eat"
 *   (S) MODAL (not) INFINITIVE (OBJECT)     "I can go" / "he cannot go"
 *   (S) (not) VERB (OBJECT)                 "the mum eats pizza" / "I do not eat"
 *   (S) be (not) ADJECTIVE                  "she is tired" / "I am not happy"
 * Object NP: (the | a/an) (ADJ) NOUN.
 *
 * Defaults, resolved by proposal (the user cycles alternatives): subject omitted -> "I";
 * object article definite ("the", indefinite offered); tense present (simple past offered).
 */
class EnglishRealizer(
    lexicon: EnglishLexicon,
) : LanguageRealizer {
    private val entries = lexicon.entries.associateBy { it.lemma.lowercase() }

    private enum class Subject(
        val word: String?,
        val thirdSingular: Boolean,
        val be: String,
        val bePast: String,
    ) {
        I("I", false, "am", "was"),
        YOU("you", false, "are", "were"),
        HE("he", true, "is", "was"),
        SHE("she", true, "is", "was"),
        IT("it", true, "is", "was"),
        WE("we", false, "are", "were"),
        THEY("they", false, "are", "were"),

        // Third-person singular noun subject ("the mum eats"): word supplied separately.
        NOUN_3S(null, true, "is", "was"),
    }

    private data class ObjectPhrase(
        val nounWord: String,
        val adjective: EnAdjective?,
        val adjectiveLabel: String?,
    )

    private data class ParsedSentence(
        val subject: Subject,
        val subjectNounPhrase: String?,
        val negated: Boolean,
        val modal: EnVerb?,
        val verb: EnVerb?,
        val infinitive: EnVerb?,
        val objectPhrase: ObjectPhrase?,
        val predicateAdjective: EnAdjective?,
    )

    override fun realize(tokens: List<PictogramToken>): List<SentenceCandidate> {
        val s = parse(tokens) ?: return emptyList()
        return buildList {
            present(s, definiteObject = true)?.let(::add)
            if (s.objectPhrase != null) present(s, definiteObject = false)?.let(::add)
            past(s)?.let(::add)
        }
    }

    // ---------------------------------------------------------------- parsing

    @Suppress("ReturnCount", "CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth", "ComplexCondition")
    private fun parse(tokens: List<PictogramToken>): ParsedSentence? {
        var subject: Subject? = null
        var subjectNoun: EnNoun? = null
        var negated = false
        var modal: EnVerb? = null
        var verb: EnVerb? = null
        var infinitive: EnVerb? = null
        var objectNoun: EnNoun? = null
        var objectLabel: String? = null
        var adjective: EnAdjective? = null
        var adjectiveLabel: String? = null
        var sawVerb = false

        for (token in tokens) {
            val lemma = token.lemma.lowercase()
            val entry = entries[lemma]
            when {
                lemma == "not" -> {
                    // English negation: "not" may precede the verb (do-support / copula:
                    // "I do not eat", "I am not happy") or follow a modal ("I cannot go").
                    if (negated) return null
                    negated = true
                }
                lemma in PRONOUNS -> {
                    if (subject != null || subjectNoun != null || sawVerb || negated) return null
                    subject = PRONOUNS.getValue(lemma)
                }
                entry is EnVerb -> {
                    when {
                        !sawVerb -> {
                            if (lemma in MODAL_LEMMAS) modal = entry else verb = entry
                            sawVerb = true
                        }
                        (modal != null || verb?.base == "want") && infinitive == null -> {
                            infinitive = entry
                        }
                        else -> return null
                    }
                }
                entry is EnNoun || (entry == null && token.pos == Pos.NOUN) -> {
                    when {
                        !sawVerb && subjectNoun == null && subject == null && !negated -> {
                            subjectNoun = entry as? EnNoun
                            if (subjectNoun == null && entry == null) return null // multi-word/unknown subject
                        }
                        // English adjectives are PRENOMINAL, so an object noun may follow a
                        // pending adjective ("the big pizza") — the adjective attaches here.
                        sawVerb && objectLabel == null -> {
                            objectNoun = entry as? EnNoun
                            objectLabel = token.label
                        }
                        else -> return null
                    }
                }
                // Adjectives are prenominal: allowed only before the object noun, never after it.
                entry is EnAdjective -> {
                    if (!sawVerb || adjectiveLabel != null || objectLabel != null) return null
                    adjective = entry
                    adjectiveLabel = token.label
                }
                else -> return null // unknown material -> concat, never guess
            }
        }
        if (!sawVerb) return null
        // A bare modal ("I can") needs its infinitive complement.
        if (modal != null && infinitive == null) return null

        val resolvedSubject = subject ?: if (subjectNoun != null) Subject.NOUN_3S else Subject.I
        val isCopula = modal == null && verb?.base == "be" && objectLabel == null && adjective != null

        // An adjective must attach to an object noun or the copula; else it would be dropped.
        if (adjectiveLabel != null && objectLabel == null && !isCopula) return null
        // The verb governing the object (infinitive if present, else the finite verb) must
        // be transitive, else the object needs a preposition we do not build -> concat.
        val governing = infinitive ?: verb
        if (objectLabel != null && governing?.transitive != true) return null
        // Can't build an NP for an unsupported object noun that also carries an adjective.
        if (objectLabel != null && objectNoun == null && adjective != null) return null
        // "be" takes no direct object in v1 (predicate nominative out of scope).
        if (objectLabel != null && governing?.base == "be") return null

        return ParsedSentence(
            subject = resolvedSubject,
            subjectNounPhrase = subjectNoun?.let { "the ${it.singular}" },
            negated = negated,
            modal = modal,
            verb = verb,
            infinitive = infinitive,
            objectPhrase =
                objectLabel?.let {
                    ObjectPhrase(objectNoun?.singular ?: it, adjective, adjectiveLabel)
                },
            predicateAdjective = if (isCopula) adjective else null,
        )
    }

    // ------------------------------------------------------------ realization

    private fun subjectWord(s: ParsedSentence): String? = s.subjectNounPhrase ?: s.subject.word

    @Suppress("ReturnCount", "CyclomaticComplexMethod") // one branch per sentence shape
    private fun present(
        s: ParsedSentence,
        definiteObject: Boolean,
    ): SentenceCandidate? {
        val parts = mutableListOf<String>()
        subjectWord(s)?.let { parts += it }

        when {
            s.predicateAdjective != null -> {
                // copula: "she is (not) tired"
                parts += s.subject.be
                if (s.negated) parts += "not"
                parts += s.predicateAdjective.word
            }
            s.modal != null -> {
                // "I can (not) go" — modal invariable, bare infinitive
                parts += if (s.negated) modalNegated(s.modal.base) else s.modal.base
                parts += requireNotNull(s.infinitive).base
                objectNp(s, definiteObject)?.let { parts += it }
            }
            s.verb?.base == "want" && s.infinitive != null -> {
                // "I do not want to eat" / "he wants to eat"
                if (s.negated) {
                    parts += doNot(s.subject)
                    parts += "want"
                } else {
                    parts += if (s.subject.thirdSingular) s.verb.thirdSingular else s.verb.base
                }
                parts += "to"
                parts += s.infinitive.base
                objectNp(s, definiteObject)?.let { parts += it }
            }
            else -> {
                val v = s.verb ?: return null
                if (s.negated) {
                    parts += doNot(s.subject)
                    parts += v.base
                } else {
                    parts += if (s.subject.thirdSingular) v.thirdSingular else v.base
                }
                objectNp(s, definiteObject)?.let { parts += it }
            }
        }
        val article = if (definiteObject) "def" else "indef"
        return SentenceCandidate(
            parts.joinToString(" "),
            CandidateSource.TEMPLATE,
            "en:${shape(s)}|tense=present|art=$article",
        )
    }

    @Suppress("ReturnCount", "CyclomaticComplexMethod") // one branch per sentence shape
    private fun past(s: ParsedSentence): SentenceCandidate? {
        if (s.modal != null) return null // modal past out of scope
        val parts = mutableListOf<String>()
        subjectWord(s)?.let { parts += it }
        when {
            s.predicateAdjective != null -> {
                parts += s.subject.bePast
                if (s.negated) parts += "not"
                parts += s.predicateAdjective.word
            }
            s.verb?.base == "want" && s.infinitive != null -> {
                if (s.negated) parts += "did not"
                parts += if (s.negated) "want" else s.verb.past
                parts += "to"
                parts += s.infinitive.base
                objectNp(s, definiteObject = true)?.let { parts += it }
            }
            else -> {
                val v = s.verb ?: return null
                if (v.base == "be") return null // handled by predicate branch only
                if (s.negated) {
                    parts += "did not"
                    parts += v.base
                } else {
                    parts += v.past
                }
                objectNp(s, definiteObject = true)?.let { parts += it }
            }
        }
        return SentenceCandidate(parts.joinToString(" "), CandidateSource.TEMPLATE, "en:${shape(s)}|tense=past|art=def")
    }

    private fun objectNp(
        s: ParsedSentence,
        definiteObject: Boolean,
    ): String? {
        val obj = s.objectPhrase ?: return null
        val head = obj.adjective?.word ?: obj.adjectiveLabel // adjective precedes the noun
        val firstWord = head ?: obj.nounWord
        val article =
            if (definiteObject) {
                "the"
            } else {
                EnglishMorphology.indefiniteArticle(firstWord, indefiniteOverride(obj))
            }
        return listOfNotNull(article, obj.adjective?.word ?: obj.adjectiveLabel, obj.nounWord).joinToString(" ")
    }

    private fun indefiniteOverride(obj: ObjectPhrase): String? =
        obj.adjective?.indefinite ?: (entries[obj.nounWord.lowercase()] as? EnNoun)?.indefinite

    private fun doNot(subject: Subject): String = if (subject.thirdSingular) "does not" else "do not"

    private fun modalNegated(base: String): String = if (base == "can") "cannot" else "$base not"

    private fun shape(s: ParsedSentence): String =
        when {
            s.predicateAdjective != null -> "copula-adj"
            s.modal != null -> "s-modal-inf"
            s.verb?.base == "want" && s.infinitive != null -> "s-want-to-inf"
            s.objectPhrase != null -> "s-v-o"
            else -> "s-v"
        }

    private companion object {
        val MODAL_LEMMAS = setOf("can", "must")

        // Keyed by lowercased pronoun (parse lowercases lemmas); the Subject carries the
        // correctly-cased output word ("I").
        val PRONOUNS: Map<String, Subject> =
            Subject.entries.filter { it.word != null }.associateBy { requireNotNull(it.word).lowercase() }
    }
}
