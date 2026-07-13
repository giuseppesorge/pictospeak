package io.github.giuseppesorge.pictospeak

import android.content.Context
import io.github.giuseppesorge.pictospeak.data.AssetVocabularyRepository
import io.github.giuseppesorge.pictospeak.data.Profile
import io.github.giuseppesorge.pictospeak.data.ProfileRepository
import io.github.giuseppesorge.pictospeak.data.VocabularyRepository
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceEngine
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import io.github.giuseppesorge.pictospeak.speech.AndroidTtsGateway
import io.github.giuseppesorge.pictospeak.speech.TtsGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manual dependency wiring — deliberately no DI framework (docs/adr/0003).
 * Lazy singletons; per-language packs are built on demand and cached so switching the
 * profile language is cheap. Keep this class small enough to read in one screen.
 */
class AppContainer(
    private val appContext: Context,
) {
    val profileRepository = ProfileRepository(appContext.filesDir)

    private val _profile = MutableStateFlow(profileRepository.load())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    fun updateProfile(transform: (Profile) -> Profile) {
        val updated = transform(_profile.value)
        _profile.value = updated
        profileRepository.save(updated)
    }

    /** Null in the foss flavor and on non-capable devices — templates are the product. */
    val sentenceRefiner: SentenceRefiner? by lazy { RefinerFactory.create() }

    val ttsGateway: TtsGateway by lazy {
        AndroidTtsGateway(appContext, Locale.forLanguageTag(_profile.value.language))
    }

    // The engine parses the pack's lexicon JSON once per language (a ~130KB read; the engine
    // never names a lexicon type — each realizer owns its own, so a new language is a new
    // pack, not an engine change).
    private val engines = mutableMapOf<String, SentenceEngine>()
    private val vocabularies = mutableMapOf<String, VocabularyRepository>()

    fun sentenceEngine(language: String): SentenceEngine =
        engines.getOrPut(language) {
            // A missing lexicon asset (unknown/typo'd language) must degrade to concat-only,
            // never throw during composition. forLanguage tolerates a null lexicon.
            val lexicon = runCatching { readAsset("lexicon/lexicon_$language.json") }.getOrNull()
            TemplateSentenceEngine.forLanguage(language, lexicon)
        }

    fun vocabularyRepository(language: String): VocabularyRepository =
        vocabularies.getOrPut(language) { AssetVocabularyRepository(appContext, language) }

    private fun readAsset(path: String): String = appContext.assets.open(path).use { it.readBytes().decodeToString() }
}
