package io.github.giuseppesorge.pictospeak.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.giuseppesorge.pictospeak.data.VocabularyRepository
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceEngine
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceRefiner
import io.github.giuseppesorge.pictospeak.speech.ConfirmationGate
import io.github.giuseppesorge.pictospeak.speech.TtsGateway
import io.github.giuseppesorge.pictospeak.speech.TtsReadiness
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class BoardUiState(
    /** Board vocabulary from the bundled catalog (empty until loaded). */
    val vocabulary: List<PictogramToken> = emptyList(),
    /** Message strip, in selection order. Capped — AAC messages are short by design. */
    val selection: List<PictogramToken> = emptyList(),
    /** Index 0 is ALWAYS the template default; an LLM candidate is only ever appended. */
    val candidates: List<SentenceCandidate> = emptyList(),
    val selectedCandidateIndex: Int = 0,
)

class BoardViewModel(
    private val sentenceEngine: SentenceEngine,
    private val sentenceRefiner: SentenceRefiner?,
    private val ttsGateway: TtsGateway,
    vocabularyRepository: VocabularyRepository,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    val speaking: StateFlow<Boolean> = ttsGateway.speaking
    val ttsReadiness: StateFlow<TtsReadiness> = ttsGateway.readiness

    private var recomputeJob: Job? = null

    init {
        viewModelScope.launch {
            val vocabulary = vocabularyRepository.load()
            _uiState.update { it.copy(vocabulary = vocabulary) }
        }
    }

    fun onPictogramTapped(token: PictogramToken) {
        if (_uiState.value.selection.size >= MAX_SELECTION) return
        _uiState.update { it.copy(selection = it.selection + token) }
        recompute()
    }

    fun onBackspace() {
        _uiState.update { it.copy(selection = it.selection.dropLast(1)) }
        recompute()
    }

    fun onClear() {
        _uiState.update { it.copy(selection = emptyList()) }
        recompute()
    }

    fun onCandidateTapped(index: Int) {
        _uiState.update { state ->
            if (index in state.candidates.indices) state.copy(selectedCandidateIndex = index) else state
        }
    }

    /**
     * INVARIANT-1: the ONLY code path that leads to speech. This handler is invoked
     * exclusively by the user's explicit tap on the speak button, and this is the single
     * [ConfirmationGate.confirm] call site in the app.
     */
    fun onSpeakPressed() {
        val state = _uiState.value
        val candidate = state.candidates.getOrNull(state.selectedCandidateIndex) ?: return
        ttsGateway.speak(ConfirmationGate.confirm(candidate))
    }

    fun onStopPressed() {
        ttsGateway.stop()
    }

    private fun recompute() {
        // Any in-flight LLM refinement is stale the moment the selection changes.
        recomputeJob?.cancel()
        recomputeJob =
            viewModelScope.launch(computeDispatcher) {
                val tokens = _uiState.value.selection
                val templateCandidates = sentenceEngine.propose(tokens)
                // Template candidates publish immediately — the LLM must never delay them.
                _uiState.update { it.copy(candidates = templateCandidates, selectedCandidateIndex = 0) }

                val baseline = templateCandidates.firstOrNull() ?: return@launch
                val refiner = sentenceRefiner ?: return@launch
                val refined = withTimeoutOrNull(REFINE_TIMEOUT_MS) { refiner.refine(tokens, baseline) }
                if (refined != null) {
                    // Append only. Never replace, reorder, or auto-select (CLAUDE.md hard rule 3).
                    _uiState.update { it.copy(candidates = it.candidates + refined) }
                }
            }
    }

    private companion object {
        const val MAX_SELECTION = 8
        const val REFINE_TIMEOUT_MS = 4_000L
    }
}
