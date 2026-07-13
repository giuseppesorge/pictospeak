package io.github.giuseppesorge.pictospeak.speech

import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.SentenceCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmationGateTest {
    @Test
    fun `confirm carries the candidate text and trace verbatim`() {
        val candidate =
            SentenceCandidate(
                text = "io voglio mangiare la pizza",
                source = CandidateSource.TEMPLATE,
                trace = "s-modal-inf-o",
            )
        val utterance = ConfirmationGate.confirm(candidate)
        assertEquals("io voglio mangiare la pizza", utterance.text)
        assertEquals("s-modal-inf-o", utterance.sourceTrace)
    }

    // Note: "ConfirmedUtterance cannot be constructed outside :speech" is enforced by the
    // compiler (internal constructor) — there is deliberately no runtime test for it.
}
