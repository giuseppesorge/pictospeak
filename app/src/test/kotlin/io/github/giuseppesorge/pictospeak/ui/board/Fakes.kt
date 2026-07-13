package io.github.giuseppesorge.pictospeak.ui.board

import io.github.giuseppesorge.pictospeak.data.VocabularyRepository
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.speech.ConfirmedUtterance
import io.github.giuseppesorge.pictospeak.speech.TtsGateway
import io.github.giuseppesorge.pictospeak.speech.TtsReadiness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Recording fake — the INVARIANT-1 test asserts on [spoken]. */
class FakeTtsGateway : TtsGateway {
    val spoken = mutableListOf<ConfirmedUtterance>()
    val previews = mutableListOf<PictogramToken>()
    var stopCalls = 0

    override val readiness: StateFlow<TtsReadiness> =
        MutableStateFlow(TtsReadiness.Ready("fake-voice", isOfflineVoice = true))
    override val speaking: StateFlow<Boolean> = MutableStateFlow(false)

    override fun speak(utterance: ConfirmedUtterance) {
        spoken += utterance
    }

    override fun speakWordPreview(token: PictogramToken) {
        previews += token
    }

    override fun stop() {
        stopCalls++
    }

    override fun shutdown() = Unit
}

class FakeVocabularyRepository(
    private val tokens: List<PictogramToken> = emptyList(),
) : VocabularyRepository {
    override suspend fun load(): List<PictogramToken> = tokens
}
