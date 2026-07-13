package io.github.giuseppesorge.pictospeak.nlg.lang.it

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Alternatives and safety properties that the golden corpus (defaults only) cannot see. */
class ItalianRealizerTest {
    private val engine = TemplateSentenceEngine(language = "it", lexicon = TestLexicon.lexicon)

    private fun t(
        lemma: String,
        pos: Pos,
    ) = PictogramToken(id = lemma, lemma = lemma, pos = pos, label = lemma)

    @Test
    fun `indefinite article is offered as a cyclable alternative`() {
        val texts =
            engine
                .propose(listOf(t("io", Pos.MISC), t("volere", Pos.VERB), t("pizza", Pos.NOUN)))
                .map { it.text }
        assertEquals("io voglio la pizza", texts.first())
        assertTrue("missing indefinite alternative in $texts", "io voglio una pizza" in texts)
    }

    @Test
    fun `passato prossimo is offered as an alternative with avere`() {
        val texts =
            engine
                .propose(listOf(t("io", Pos.MISC), t("mangiare", Pos.VERB), t("pizza", Pos.NOUN)))
                .map { it.text }
        assertTrue("missing passato alternative in $texts", "io ho mangiato la pizza" in texts)
    }

    @Test
    fun `passato prossimo uses essere with participle agreement`() {
        val lei = engine.propose(listOf(t("lei", Pos.MISC), t("andare", Pos.VERB))).map { it.text }
        assertTrue("missing 'lei è andata' in $lei", "lei è andata" in lei)
        val noi = engine.propose(listOf(t("noi", Pos.MISC), t("cadere", Pos.VERB))).map { it.text }
        assertTrue("missing 'noi siamo caduti' in $noi", "noi siamo caduti" in noi)
    }

    // The SimpleNLG-IT lesson (docs/grammar-v1.md): the auxiliary is LEXICON DATA.
    // succedere must produce "è successo" — "ha successo" was the published failure.
    @Test
    fun `the e-successo lesson - auxiliary comes from the lexicon`() {
        val texts = engine.propose(listOf(t("lui", Pos.MISC), t("succedere", Pos.VERB))).map { it.text }
        assertTrue("missing 'lui è successo' in $texts", "lui è successo" in texts)
        assertTrue("'ha successo' must never be generated: $texts", texts.none { "ha successo" in it })
    }

    @Test
    fun `modal passato uses avere`() {
        val texts =
            engine
                .propose(
                    listOf(t("io", Pos.MISC), t("volere", Pos.VERB), t("mangiare", Pos.VERB), t("pizza", Pos.NOUN)),
                ).map { it.text }
        assertTrue("missing modal passato in $texts", "io ho voluto mangiare la pizza" in texts)
    }

    @Test
    fun `no candidate ever drops a word the user selected`() {
        // "grande" cannot attach anywhere: the realizer must refuse, not omit it.
        val candidates = engine.propose(listOf(t("io", Pos.MISC), t("mangiare", Pos.VERB), t("grande", Pos.DESCRIPTOR)))
        assertEquals(1, candidates.size)
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
        assertEquals("io mangiare grande", candidates.single().text)
    }

    @Test
    fun `reflexive verbs degrade to concat - clitics are out of scope`() {
        val candidates = engine.propose(listOf(t("io", Pos.MISC), t("sedersi", Pos.VERB)))
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
    }

    @Test
    fun `unknown vocabulary degrades to concat`() {
        val candidates = engine.propose(listOf(t("xyzzy", Pos.MISC), t("mangiare", Pos.VERB)))
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
    }

    @Test
    fun `empty lexicon means concat-only engine`() {
        val bare = TemplateSentenceEngine(language = "it")
        val candidates = bare.propose(listOf(t("io", Pos.MISC), t("mangiare", Pos.VERB)))
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
    }

    @Test
    fun `unknown language is concat-only by design`() {
        val engine = TemplateSentenceEngine(language = "xx", lexicon = TestLexicon.lexicon)
        val candidates = engine.propose(listOf(t("io", Pos.MISC), t("mangiare", Pos.VERB)))
        assertEquals(CandidateSource.FALLBACK_CONCAT, candidates.single().source)
    }
}
