package io.github.giuseppesorge.pictospeak.speech

/**
 * Explicit TTS readiness — a mute AAC device is the worst field failure, so readiness is
 * modeled, shown to caregivers in the first-run wizard, and never assumed.
 */
sealed interface TtsReadiness {
    /** Engine init in progress; speaking is impossible by construction. */
    data object Initializing : TtsReadiness

    /** An engine with a usable voice for the requested language is available. */
    data class Ready(
        val voiceName: String?,
        val isOfflineVoice: Boolean,
    ) : TtsReadiness

    /** Engine present, language supported, but voice data must be downloaded once. */
    data object MissingVoiceData : TtsReadiness

    /** Engine present but the requested language is not supported at all. */
    data object NoLanguageSupport : TtsReadiness

    /** No TTS engine on the device. */
    data object NoEngine : TtsReadiness
}
