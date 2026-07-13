package io.github.giuseppesorge.pictospeak.speech

import io.github.giuseppesorge.pictospeak.speech.TtsReadinessMachine.LanguageAvailability
import org.junit.Assert.assertEquals
import org.junit.Test

class TtsReadinessMachineTest {
    @Test
    fun `no engine wins over everything`() {
        assertEquals(
            TtsReadiness.NoEngine,
            TtsReadinessMachine.decide(false, LanguageAvailability.AVAILABLE, "voice", "voice"),
        )
    }

    @Test
    fun `unsupported language is terminal`() {
        assertEquals(
            TtsReadiness.NoLanguageSupport,
            TtsReadinessMachine.decide(true, LanguageAvailability.NOT_SUPPORTED, null, null),
        )
    }

    @Test
    fun `missing voice data asks for the one-time download`() {
        assertEquals(
            TtsReadiness.MissingVoiceData,
            TtsReadinessMachine.decide(true, LanguageAvailability.MISSING_DATA, null, null),
        )
    }

    @Test
    fun `offline voice is preferred and flagged offline`() {
        assertEquals(
            TtsReadiness.Ready("offline-voice", isOfflineVoice = true),
            TtsReadinessMachine.decide(true, LanguageAvailability.AVAILABLE, "offline-voice", "network-voice"),
        )
    }

    @Test
    fun `network-only voice is usable but flagged not offline`() {
        assertEquals(
            TtsReadiness.Ready("network-voice", isOfflineVoice = false),
            TtsReadinessMachine.decide(true, LanguageAvailability.AVAILABLE, null, "network-voice"),
        )
    }

    @Test
    fun `available language with no enumerable voices is still usable`() {
        assertEquals(
            TtsReadiness.Ready(null, isOfflineVoice = false),
            TtsReadinessMachine.decide(true, LanguageAvailability.AVAILABLE, null, null),
        )
    }
}
