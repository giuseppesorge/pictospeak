package io.github.giuseppesorge.pictospeak.speech

/**
 * Pure decision logic behind [TtsReadiness] — kept free of Android types so it is
 * JVM-table-testable. [AndroidTtsGateway] feeds it facts from the platform APIs.
 */
object TtsReadinessMachine {
    /** Mirror of the TextToSpeech.setLanguage result values we act on. */
    enum class LanguageAvailability {
        AVAILABLE,
        MISSING_DATA,
        NOT_SUPPORTED,
    }

    fun decide(
        engineAvailable: Boolean,
        languageAvailability: LanguageAvailability?,
        offlineVoiceName: String?,
        anyVoiceName: String?,
    ): TtsReadiness =
        when {
            !engineAvailable -> TtsReadiness.NoEngine
            languageAvailability == LanguageAvailability.NOT_SUPPORTED -> TtsReadiness.NoLanguageSupport
            languageAvailability == LanguageAvailability.MISSING_DATA -> TtsReadiness.MissingVoiceData
            offlineVoiceName != null -> TtsReadiness.Ready(offlineVoiceName, isOfflineVoice = true)
            anyVoiceName != null -> TtsReadiness.Ready(anyVoiceName, isOfflineVoice = false)
            languageAvailability == LanguageAvailability.AVAILABLE ->
                TtsReadiness.Ready(voiceName = null, isOfflineVoice = false)
            else -> TtsReadiness.MissingVoiceData
        }
}
