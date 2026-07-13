package io.github.giuseppesorge.pictospeak.speech

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine
import android.speech.tts.UtteranceProgressListener
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Native Android TTS behind [TtsGateway]. Thin platform glue: all decision logic lives in
 * the JVM-tested [TtsReadinessMachine]. The active locale follows the profile language;
 * readiness is re-evaluated on init and on every language change so a mute device is always
 * shown, never assumed (docs/architecture.md, TTS section).
 */
@Suppress("TooManyFunctions") // cohesive platform adapter: the TtsGateway surface + lifecycle glue
class AndroidTtsGateway(
    private val appContext: Context,
    initialLocale: Locale = Locale.ITALIAN,
) : TtsGateway {
    private val _readiness = MutableStateFlow<TtsReadiness>(TtsReadiness.Initializing)
    override val readiness: StateFlow<TtsReadiness> = _readiness.asStateFlow()

    private val _speaking = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val utteranceCounter = AtomicLong(0)

    @Volatile private var locale: Locale = initialLocale

    @Volatile private var initialized = false

    private val tts: TextToSpeech =
        TextToSpeech(appContext.applicationContext) { status ->
            initialized = status == TextToSpeech.SUCCESS
            if (initialized) attachProgressListener()
            evaluateReadiness()
        }

    override fun setLanguage(locale: Locale) {
        this.locale = locale
        evaluateReadiness()
    }

    override fun setSpeechRate(rate: Float) {
        tts.setSpeechRate(rate)
    }

    override fun setPitch(pitch: Float) {
        tts.setPitch(pitch)
    }

    private fun evaluateReadiness() {
        val availability =
            if (initialized) {
                when (tts.setLanguage(locale)) {
                    TextToSpeech.LANG_MISSING_DATA -> TtsReadinessMachine.LanguageAvailability.MISSING_DATA
                    TextToSpeech.LANG_NOT_SUPPORTED -> TtsReadinessMachine.LanguageAvailability.NOT_SUPPORTED
                    else -> TtsReadinessMachine.LanguageAvailability.AVAILABLE
                }
            } else {
                null
            }
        val voices = if (initialized) runCatching { tts.voices }.getOrNull().orEmpty() else emptySet()
        val localeVoices = voices.filter { it.locale.language == locale.language }
        val readiness =
            TtsReadinessMachine.decide(
                engineAvailable = initialized,
                languageAvailability = availability,
                offlineVoiceName = localeVoices.firstOrNull { !it.isNetworkConnectionRequired }?.name,
                anyVoiceName = localeVoices.firstOrNull()?.name,
            )
        mainHandler.post { _readiness.value = readiness }
    }

    private fun attachProgressListener() {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post { _speaking.value = true }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post { _speaking.value = false }
                }

                @Deprecated("Platform API — superseded overload still required for API 29")
                override fun onError(utteranceId: String?) {
                    mainHandler.post { _speaking.value = false }
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    mainHandler.post { _speaking.value = false }
                }
            },
        )
    }

    override fun speak(utterance: ConfirmedUtterance) = utter(utterance.text, "utterance")

    override fun speakWordPreview(token: PictogramToken) = utter(token.label, "preview")

    private fun utter(
        text: String,
        tag: String,
    ) {
        if (_readiness.value !is TtsReadiness.Ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$tag-${utteranceCounter.incrementAndGet()}")
    }

    override fun stop() {
        tts.stop()
        mainHandler.post { _speaking.value = false }
    }

    override fun installVoiceData() {
        val intent =
            Intent(Engine.ACTION_INSTALL_TTS_DATA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { appContext.startActivity(intent) }
    }

    override fun shutdown() {
        tts.shutdown()
    }
}
