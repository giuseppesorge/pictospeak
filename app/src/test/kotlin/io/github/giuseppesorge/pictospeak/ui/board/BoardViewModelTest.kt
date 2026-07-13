package io.github.giuseppesorge.pictospeak.ui.board

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.nlg.engine.TemplateSentenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val io = PictogramToken("t1", "io", Pos.MISC, "io")
    private val mangiare = PictogramToken("t2", "mangiare", Pos.VERB, "mangiare")
    private lateinit var tts: FakeTtsGateway

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tts = FakeTtsGateway()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        BoardViewModel(
            sentenceEngine = TemplateSentenceEngine.forLanguage("it", null),
            sentenceRefiner = null,
            ttsGateway = tts,
            vocabularyRepository = FakeVocabularyRepository(listOf(io, mangiare)),
            computeDispatcher = dispatcher,
        )

    @Test
    fun `home board loads with pictogram and folder cells`() =
        runTest(dispatcher) {
            val vm = viewModel()
            dispatcher.scheduler.advanceUntilIdle()
            val state = vm.uiState.value
            assertEquals("home", state.boardId)
            assertEquals(
                listOf(io, mangiare),
                state.cells.filterIsInstance<BoardCellUi.Picto>().map { it.token },
            )
            assertEquals(
                listOf("cibo"),
                state.cells.filterIsInstance<BoardCellUi.Folder>().map { it.boardId },
            )
        }

    @Test
    fun `folder navigation adds a back cell and back returns home`() =
        runTest(dispatcher) {
            val vm = viewModel()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onFolderTapped("cibo")
            dispatcher.scheduler.advanceUntilIdle()
            val inFolder = vm.uiState.value
            assertEquals("cibo", inFolder.boardId)
            assertEquals(BoardCellUi.Back, inFolder.cells.first())
            vm.onBackToHome()
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals("home", vm.uiState.value.boardId)
        }

    @Test
    fun `navigating boards preserves the selection strip`() =
        runTest(dispatcher) {
            val vm = viewModel()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onPictogramTapped(io)
            vm.onFolderTapped("cibo")
            vm.onPictogramTapped(mangiare)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(listOf(io, mangiare), vm.uiState.value.selection)
        }

    @Test
    fun `tapping pictograms appends to selection and proposes a candidate`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.uiState.test {
                skipItems(1) // initial empty state

                vm.onPictogramTapped(io)
                assertEquals(listOf(io), expectMostRecentItemAfterIdle().selection)

                vm.onPictogramTapped(mangiare)
                val state = expectMostRecentItemAfterIdle()
                assertEquals(listOf(io, mangiare), state.selection)
                assertEquals("io mangiare", state.candidates.single().text)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clear empties selection and candidates`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onPictogramTapped(io)
            vm.onClear()
            dispatcher.scheduler.advanceUntilIdle()
            val state = vm.uiState.value
            assertTrue(state.selection.isEmpty())
            assertTrue(state.candidates.isEmpty())
        }

    @Test
    fun `selection is capped`() =
        runTest(dispatcher) {
            val vm = viewModel()
            repeat(20) { vm.onPictogramTapped(io) }
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(8, vm.uiState.value.selection.size)
        }

    // INVARIANT-1 (docs/adr/0010): no event sequence except an explicit onSpeakPressed
    // may reach TtsGateway.speak.
    @Test
    fun `composing, cycling, clearing never speaks`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onPictogramTapped(io)
            vm.onPictogramTapped(mangiare)
            vm.onCandidateTapped(0)
            vm.onBackspace()
            vm.onPictogramTapped(mangiare)
            vm.onClear()
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue("INVARIANT-1 violated: speech without confirmation", tts.spoken.isEmpty())
        }

    @Test
    fun `speak pressed confirms exactly the selected candidate`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onPictogramTapped(io)
            vm.onPictogramTapped(mangiare)
            dispatcher.scheduler.advanceUntilIdle()
            vm.onSpeakPressed()
            assertEquals(listOf("io mangiare"), tts.spoken.map { it.text })
        }

    @Test
    fun `speak with no candidates is a no-op`() =
        runTest(dispatcher) {
            val vm = viewModel()
            dispatcher.scheduler.advanceUntilIdle()
            vm.onSpeakPressed()
            assertTrue(tts.spoken.isEmpty())
        }

    private fun TurbineTestContext<BoardUiState>.expectMostRecentItemAfterIdle(): BoardUiState {
        dispatcher.scheduler.advanceUntilIdle()
        return expectMostRecentItem()
    }
}
