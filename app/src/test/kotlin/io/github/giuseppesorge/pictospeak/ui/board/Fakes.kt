package io.github.giuseppesorge.pictospeak.ui.board

import io.github.giuseppesorge.pictospeak.data.VocabularyRepository
import io.github.giuseppesorge.pictospeak.nlg.api.Board
import io.github.giuseppesorge.pictospeak.nlg.api.BoardCell
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

    override fun setLanguage(locale: java.util.Locale) = Unit

    override fun setSpeechRate(rate: Float) = Unit

    override fun setPitch(pitch: Float) = Unit

    override fun installVoiceData() = Unit

    override fun shutdown() = Unit
}

/**
 * In-memory content: a home board with all [tokens] plus one "cibo" folder containing the
 * same tokens — enough to exercise selection, proposals, and folder navigation.
 */
class FakeVocabularyRepository(
    private val tokens: List<PictogramToken> = emptyList(),
) : VocabularyRepository {
    override suspend fun loadCatalog(): Map<String, PictogramToken> = tokens.associateBy { it.id }

    override suspend fun loadBoards(): Map<String, Board> {
        val cells = tokens.map { BoardCell.Pictogram(it.id) }
        val home =
            Board(
                id = "home",
                locale = "it",
                name = "Home",
                cells = cells + listOf(BoardCell.Link("cibo", "Cibo", tokens.firstOrNull()?.id ?: "")),
            )
        val cibo = Board(id = "cibo", locale = "it", name = "Cibo", cells = cells)
        return mapOf("home" to home, "cibo" to cibo)
    }
}
