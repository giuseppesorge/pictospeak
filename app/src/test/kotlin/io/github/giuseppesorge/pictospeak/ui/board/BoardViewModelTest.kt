package io.github.giuseppesorge.pictospeak.ui.board

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

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        BoardViewModel(
            sentenceEngine = TemplateSentenceEngine(language = "it"),
            sentenceRefiner = null,
            computeDispatcher = dispatcher,
        )

    @Test
    fun `tapping pictograms appends to selection and proposes a candidate`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.uiState.test {
                assertEquals(emptyList<PictogramToken>(), awaitItem().selection)

                vm.onPictogramTapped(io)
                assertEquals(listOf(io), awaitItem().selection)
                assertEquals("io", awaitItem().candidates.single().text)

                vm.onPictogramTapped(mangiare)
                assertEquals(listOf(io, mangiare), awaitItem().selection)
                assertEquals("io mangiare", awaitItem().candidates.single().text)
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
}
