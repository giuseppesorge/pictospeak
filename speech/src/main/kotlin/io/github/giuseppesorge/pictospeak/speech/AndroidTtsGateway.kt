package io.github.giuseppesorge.pictospeak.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Native Android TTS behind [TtsGateway]. Thin platform glue: all decision logic lives in
 * the JVM-tested [TtsReadinessMachine].
 *
 * M0 skeleton: engine init, readiness evaluation, speak/stop. The first-run voice-install
 * wizard, offline-voice preference and engine iteration land at M4 (see docs/architecture.md).
 */
class AndroidTtsGateway(
    context: Context,
    private val locale: Locale = Locale.ITALIAN,
) : TtsGateway {
    private val _readiness = MutableStateFlow<TtsReadiness>(TtsReadiness.Initializing)
    override val readiness: StateFlow<TtsReadiness> = _readiness.asStateFlow()

    private val _speaking = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val utteranceCounter = AtomicLong(0)

    private val tts: TextToSpeech =
        TextToSpeech(context.applicationContext) { status ->
            evaluateReadiness(engineAvailable = status == TextToSpeech.SUCCESS)
        }

    private fun evaluateReadiness(engineAvailable: Boolean) {
        val availability =
            if (engineAvailable) {
                when (tts.setLanguage(locale)) {
                    TextToSpeech.LANG_MISSING_DATA -> TtsReadinessMachine.LanguageAvailability.MISSING_DATA
                    TextToSpeech.LANG_NOT_SUPPORTED -> TtsReadinessMachine.LanguageAvailability.NOT_SUPPORTED
                    else -> TtsReadinessMachine.LanguageAvailability.AVAILABLE
                }
            } else {
                null
            }
        val voices = if (engineAvailable) runCatching { tts.voices }.getOrNull().orEmpty() else emptySet()
        val localeVoices = voices.filter { it.locale.language == locale.language }
        val readiness =
            TtsReadinessMachine.decide(
                engineAvailable = engineAvailable,
                languageAvailability = availability,
                offlineVoiceName = localeVoices.firstOrNull { !it.isNetworkConnectionRequired }?.name,
                anyVoiceName = localeVoices.firstOrNull()?.name,
            )
        mainHandler.post { _readiness.value = readiness }

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

    override fun speak(utterance: ConfirmedUtterance) {
        if (_readiness.value !is TtsReadiness.Ready) return
        tts.speak(
            utterance.text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "utterance-${utteranceCounter.incrementAndGet()}",
        )
    }

    override fun speakWordPreview(token: PictogramToken) {
        if (_readiness.value !is TtsReadiness.Ready) return
        tts.speak(
            token.label,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "preview-${utteranceCounter.incrementAndGet()}",
        )
    }

    override fun stop() {
        tts.stop()
        mainHandler.post { _speaking.value = false }
    }

    override fun shutdown() {
        tts.shutdown()
    }
}
