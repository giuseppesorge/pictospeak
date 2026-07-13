package io.github.giuseppesorge.pictospeak

import android.content.Context
import io.github.giuseppesorge.pictospeak.data.AssetVocabularyRepository
import io.github.giuseppesorge.pictospeak.data.Profile
import io.github.giuseppesorge.pictospeak.data.ProfileRepository
import io.github.giuseppesorge.pictospeak.data.VocabularyRepository
import io.github.giuseppesorge.pictospeak.llm.DeviceCapability
import io.github.giuseppesorge.pictospeak.llm.DeviceGate
import io.github.giuseppesorge.pictospeak.llm.ModelStore
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceEngine
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import io.github.giuseppesorge.pictospeak.speech.AndroidTtsGateway
import io.github.giuseppesorge.pictospeak.speech.TtsGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
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

    // ---- Optional on-device LLM (play flavor only; templates are always the product) ----

    /** True only in a build that contains :llm (play). foss hides the whole LLM surface. */
    val llmFlavor: Boolean = RefinerFactory.LLM_FLAVOR

    /** Imported model weights (never bundled — llm/NOTICE-models.md). */
    val modelStore = ModelStore(File(appContext.filesDir, "models"))

    /** Device capability for the LLM gate; evaluated once (docs/llm-experiment.md). */
    val deviceCapability: DeviceCapability by lazy { DeviceGate.forDevice(appContext) }

    private var refinerKey: String? = null
    private var cachedRefiner: SentenceRefiner? = null

    /**
     * The refiner for the current profile, or null when any gate fails (foss flavor, opt-in
     * off, license not accepted, device ineligible, or no model imported). Cached and rebuilt
     * only when the gate outcome or the model changes; the old engine is released on rebuild.
     */
    @Synchronized
    fun sentenceRefiner(profile: Profile): SentenceRefiner? {
        val model = modelStore.current()
        val gatePassed =
            llmFlavor &&
                profile.llmEnabled &&
                profile.llmModelLicenseAccepted &&
                deviceCapability.eligible &&
                model != null
        val key = "$gatePassed:${model?.sha256.orEmpty()}"
        if (key != refinerKey) {
            (cachedRefiner as? AutoCloseable)?.close()
            cachedRefiner = RefinerFactory.create(modelStore.currentModelPath(), gatePassed)
            refinerKey = key
        }
        return cachedRefiner
    }

    /** Release the LLM engine's native memory (low-memory callback). */
    @Synchronized
    fun releaseLlm() {
        (cachedRefiner as? AutoCloseable)?.close()
        cachedRefiner = null
        refinerKey = null
    }

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
