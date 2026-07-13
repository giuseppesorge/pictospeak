package io.github.giuseppesorge.pictospeak.nlg.lang.it

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.lang.LanguageRealizer

/**
 * Italian realizer — implements EXACTLY the frozen v1 scope (docs/grammar-v1.md):
 * presente indicativo, modal+infinitive, passato prossimo (per-verb auxiliary from the
 * lexicon, participle agreement under essere), article selection, noun-adjective
 * agreement, pre-verbal negation, copula+adjective. Anything outside the shapes below
 * returns no candidates and the engine falls back to concatenation.
 *
 * Sentence shapes (S = subject pronoun or leading noun; parentheses = optional):
 *   (S) (non) MODAL INFINITIVE (OBJECT (ADJ))     "io voglio mangiare la pizza"
 *   (S) (non) VERB (OBJECT (ADJ))                 "la mamma mangia la pizza"
 *   (S) (non) essere ADJ                          "lei è stanca"
 *
 * Defaults, resolved by proposal (the user cycles alternatives, docs/architecture.md):
 * subject omitted -> first person singular, pro-drop ("voglio la pizza"); object article
 * definite (indefinite offered as alternative); tense presente (passato prossimo offered
 * as alternative).
 */
class ItalianRealizer(
    lexicon: ItalianLexicon,
) : LanguageRealizer {
    private val entries = lexicon.entries.associateBy { it.lemma.lowercase() }
    private val avere = entries["avere"] as? VerbEntry
    private val essere = entries["essere"] as? VerbEntry

    private data class SubjectInfo(
        val person: Person,
        val feminine: Boolean,
        val plural: Boolean,
    )

    override fun realize(tokens: List<PictogramToken>): List<SentenceCandidate> {
        val sentence = parse(tokens) ?: return emptyList()
        return buildList {
            realizePresent(sentence, definiteObject = true)?.let(::add)
            if (sentence.objectNoun?.entry != null) {
                realizePresent(sentence, definiteObject = false)?.let(::add)
            }
            realizePassato(sentence)?.let(::add)
        }
    }

    // ---------------------------------------------------------------- parsing

    private data class ObjectPhrase(
        val entry: NounEntry?,
        val label: String,
        val adjective: AdjectiveEntry?,
        val adjectiveLabel: String?,
    )

    private data class ParsedSentence(
        val subjectPronoun: String?,
        val subjectNoun: NounEntry?,
        val negated: Boolean,
        val modal: VerbEntry?,
        val verb: VerbEntry?,
        val infinitiveLemma: String?,
        val objectNoun: ObjectPhrase?,
        val predicateAdjective: AdjectiveEntry?,
    ) {
        val subject: SubjectInfo
            get() =
                when {
                    subjectPronoun != null -> PRONOUNS.getValue(subjectPronoun)
                    subjectNoun != null ->
                        SubjectInfo(
                            Person.THIRD_SINGULAR,
                            feminine = subjectNoun.gender == Gender.FEMININE,
                            plural = false,
                        )
                    else -> SubjectInfo(Person.FIRST_SINGULAR, feminine = false, plural = false)
                }
    }

    // A hand-rolled parser is one decision tree: early returns and nesting are the point.
    @Suppress("ReturnCount", "CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth", "ComplexCondition")
    private fun parse(tokens: List<PictogramToken>): ParsedSentence? {
        var subjectPronoun: String? = null
        var subjectNoun: NounEntry? = null
        var negated = false
        var modal: VerbEntry? = null
        var verb: VerbEntry? = null
        var infinitiveLemma: String? = null
        var objectEntry: NounEntry? = null
        var objectLabel: String? = null
        var adjective: AdjectiveEntry? = null
        var adjectiveLabel: String? = null
        var sawVerb = false

        for (token in tokens) {
            val lemma = token.lemma.lowercase()
            val entry = entries[lemma]
            when {
                lemma == "non" -> {
                    if (sawVerb || negated) return null
                    negated = true
                }
                lemma in PRONOUNS -> {
                    // A subject after "non" is pre-subject negation ("non io"): out of scope.
                    if (subjectPronoun != null || subjectNoun != null || sawVerb || negated) return null
                    subjectPronoun = lemma
                }
                entry is VerbEntry -> {
                    when {
                        !sawVerb -> {
                            if (entry.reflexive) return null // clitics are out of scope: never guess
                            if (lemma in MODAL_LEMMAS) modal = entry else verb = entry
                            sawVerb = true
                        }
                        modal != null && infinitiveLemma == null -> {
                            if (entry.reflexive) return null
                            verb = entry
                            infinitiveLemma = entry.lemma
                        }
                        else -> return null // two finite verbs: outside the frozen scope
                    }
                }
                entry is NounEntry || (entry == null && token.pos == Pos.NOUN) -> {
                    when {
                        !sawVerb && subjectNoun == null && subjectPronoun == null && entry != null && !negated ->
                            subjectNoun = entry
                        // The object noun must PRECEDE its adjective (frozen shape OBJECT ADJ);
                        // an adjective already seen means out-of-order input -> concat.
                        sawVerb && objectLabel == null && adjectiveLabel == null -> {
                            objectEntry = entry as? NounEntry
                            objectLabel = token.label
                        }
                        else -> return null
                    }
                }
                // Only genuine (postnominal, agreeing) adjectives fill the adjective slot.
                // Quantifiers/possessives/demonstratives/adverbs are unsupported (entry == null)
                // and fall through to concat rather than being misplaced or wrongly inflected.
                entry is AdjectiveEntry -> {
                    if (!sawVerb || adjectiveLabel != null) return null
                    adjective = entry
                    adjectiveLabel = token.label
                }
                else -> return null // unknown material: concat fallback, never guess
            }
        }
        if (!sawVerb) return null
        if (modal != null && infinitiveLemma == null && verb == null) {
            // A lone modal acts as the main verb ("io voglio la pizza").
            verb = modal
            modal = null
        }

        // The verb that governs the object: the infinitive if present, else the finite verb.
        // An object after an intransitive verb ("andare casa") would need a preposition
        // ("a casa"), which v1 does not build -> concat, never "vado la casa".
        if (objectLabel != null && verb != null && !verb.transitive) return null
        // An adjective on an unsupported object noun cannot be agreed (no gender/number) ->
        // concat rather than a guessed inflection.
        if (objectLabel != null && objectEntry == null && adjective != null) return null

        val isCopula = modal == null && verb?.lemma?.lowercase() == "essere" && objectLabel == null && adjective != null
        // An adjective must attach to the object noun or to the copula — anything else
        // would silently drop a word the user selected. Concat fallback keeps every word.
        if (adjectiveLabel != null && objectLabel == null && !isCopula) return null
        return ParsedSentence(
            subjectPronoun = subjectPronoun,
            subjectNoun = subjectNoun,
            negated = negated,
            modal = modal,
            verb = verb,
            infinitiveLemma = infinitiveLemma,
            objectNoun =
                objectLabel?.let {
                    ObjectPhrase(objectEntry, it, adjective.takeIf { _ -> !isCopula }, adjectiveLabel)
                },
            predicateAdjective = if (isCopula) adjective else null,
        )
    }

    // ------------------------------------------------------------ realization

    @Suppress("ReturnCount") // early-return guards: missing morphology means no candidate
    private fun realizePresent(
        s: ParsedSentence,
        definiteObject: Boolean,
    ): SentenceCandidate? {
        val finite = (s.modal ?: s.verb) ?: return null
        val conjugated = finite.presentIndicative[s.subject.person] ?: return null
        val words = assemble(s, conjugated, definiteObject) ?: return null
        val article = if (definiteObject) "def" else "indef"
        return SentenceCandidate(
            text = words,
            source = CandidateSource.TEMPLATE,
            trace = "it:${shape(s)}|tense=presente|art=$article",
        )
    }

    @Suppress("ReturnCount") // early-return guards: missing morphology means no candidate
    private fun realizePassato(s: ParsedSentence): SentenceCandidate? {
        if (s.predicateAdjective != null) return null // copula passato: out of v1 scope
        val finite = (s.modal ?: s.verb) ?: return null
        val auxEntry = (if (finite.auxiliary == Auxiliary.ESSERE) essere else avere) ?: return null
        val auxForm = auxEntry.presentIndicative[s.subject.person] ?: return null
        val participle =
            if (finite.auxiliary == Auxiliary.ESSERE) {
                ItalianMorphology.agreeParticiple(finite.pastParticiple, s.subject.feminine, s.subject.plural)
            } else {
                finite.pastParticiple
            }
        val words = assemble(s, "$auxForm $participle", definiteObject = true) ?: return null
        return SentenceCandidate(
            text = words,
            source = CandidateSource.TEMPLATE,
            trace = "it:${shape(s)}|tense=passato-prossimo|art=def",
        )
    }

    @Suppress("ReturnCount")
    private fun assemble(
        s: ParsedSentence,
        finiteForm: String,
        definiteObject: Boolean,
    ): String? {
        val parts = mutableListOf<String>()
        when {
            s.subjectPronoun != null -> parts += s.subjectPronoun
            s.subjectNoun != null ->
                parts +=
                    ItalianMorphology.withDefiniteArticle(
                        s.subjectNoun.singular,
                        s.subjectNoun.gender,
                        plural = false,
                    )
        }
        if (s.negated) parts += "non"
        parts += finiteForm
        s.infinitiveLemma?.let { parts += it.lowercase() }

        s.predicateAdjective?.let { adj ->
            parts +=
                ItalianMorphology.agreeAdjective(adj, feminine = s.subject.feminine, plural = s.subject.plural)
        }

        s.objectNoun?.let { obj ->
            val noun = obj.entry
            val nounPhrase =
                when {
                    noun == null -> obj.label // citation form: article undecidable, never guess
                    definiteObject -> ItalianMorphology.withDefiniteArticle(noun.singular, noun.gender, plural = false)
                    else -> ItalianMorphology.withIndefiniteArticle(noun.singular, noun.gender)
                }
            val adjPhrase =
                when {
                    obj.adjective != null && noun != null ->
                        ItalianMorphology.agreeAdjective(
                            obj.adjective,
                            feminine = noun.gender == Gender.FEMININE,
                            plural = false,
                        )
                    obj.adjectiveLabel != null -> obj.adjectiveLabel
                    else -> null
                }
            parts += listOfNotNull(nounPhrase, adjPhrase).joinToString(" ")
        }
        return parts.joinToString(" ")
    }

    private fun shape(s: ParsedSentence): String =
        when {
            s.predicateAdjective != null -> "copula-adj"
            s.modal != null -> "s-modal-inf-o"
            s.objectNoun != null -> "s-v-o"
            else -> "s-v"
        }

    private companion object {
        val MODAL_LEMMAS = setOf("volere", "potere", "dovere")

        val PRONOUNS =
            mapOf(
                "io" to SubjectInfo(Person.FIRST_SINGULAR, feminine = false, plural = false),
                "tu" to SubjectInfo(Person.SECOND_SINGULAR, feminine = false, plural = false),
                "lui" to SubjectInfo(Person.THIRD_SINGULAR, feminine = false, plural = false),
                "lei" to SubjectInfo(Person.THIRD_SINGULAR, feminine = true, plural = false),
                "noi" to SubjectInfo(Person.FIRST_PLURAL, feminine = false, plural = true),
                "voi" to SubjectInfo(Person.SECOND_PLURAL, feminine = false, plural = true),
                "loro" to SubjectInfo(Person.THIRD_PLURAL, feminine = false, plural = true),
            )
    }
}
