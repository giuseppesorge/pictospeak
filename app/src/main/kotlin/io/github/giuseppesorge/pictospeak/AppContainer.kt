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
    // Language selection becomes profile-driven when profiles land; "it" is the first
    // implemented LanguagePack (docs/language-packs.md).
    val sentenceEngine: SentenceEngine by lazy { TemplateSentenceEngine(language = "it") }

    /** Null in the foss flavor and on non-capable devices — templates are the product. */
    val sentenceRefiner: SentenceRefiner? by lazy { RefinerFactory.create() }

    val ttsGateway: TtsGateway by lazy { AndroidTtsGateway(appContext) }

    val vocabularyRepository: VocabularyRepository by lazy {
        AssetVocabularyRepository(appContext, language = "it")
    }
}
