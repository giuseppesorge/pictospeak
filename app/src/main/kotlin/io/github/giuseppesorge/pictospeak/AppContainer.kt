package io.github.giuseppesorge.pictospeak

import android.content.Context
import io.github.giuseppesorge.pictospeak.data.AssetVocabularyRepository
import io.github.giuseppesorge.pictospeak.data.VocabularyRepository
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceEngine
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import io.github.giuseppesorge.pictospeak.speech.AndroidTtsGateway
import io.github.giuseppesorge.pictospeak.speech.TtsGateway

/**
 * Manual dependency wiring — deliberately no DI framework (docs/adr/0003).
 * Lazy singletons only; keep this class small enough to read in one screen.
 */
class AppContainer(
    private val appContext: Context,
) {
    // Active language — becomes profile-driven when profiles land. Italian and English are
    // the supported LanguagePacks (docs/language-packs.md).
    private val language = "it"

    // The engine parses the pack's lexicon JSON once, lazily (a ~130KB read; negligible
    // against the cold-start budget). The engine never names a lexicon type — each realizer
    // owns its own, so adding a language is a new pack, not an engine change.
    val sentenceEngine: SentenceEngine by lazy {
        TemplateSentenceEngine.forLanguage(language, readAsset("lexicon/lexicon_$language.json"))
    }

    private fun readAsset(path: String): String = appContext.assets.open(path).use { it.readBytes().decodeToString() }

    /** Null in the foss flavor and on non-capable devices — templates are the product. */
    val sentenceRefiner: SentenceRefiner? by lazy { RefinerFactory.create() }

    val ttsGateway: TtsGateway by lazy { AndroidTtsGateway(appContext) }

    val vocabularyRepository: VocabularyRepository by lazy {
        AssetVocabularyRepository(appContext, language = language)
    }
}
