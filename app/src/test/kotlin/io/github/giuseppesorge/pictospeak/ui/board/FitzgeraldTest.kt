package io.github.giuseppesorge.pictospeak.ui.board

import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import org.junit.Assert.assertEquals
import org.junit.Test

class FitzgeraldTest {
    @Test
    fun `every part of speech maps to a Fitzgerald slot`() {
        val expected =
            mapOf(
                Pos.PROPER_NAME to FitzgeraldSlot.PEOPLE,
                Pos.NOUN to FitzgeraldSlot.NOUNS,
                Pos.VERB to FitzgeraldSlot.VERBS,
                Pos.DESCRIPTOR to FitzgeraldSlot.DESCRIPTORS,
                Pos.SOCIAL to FitzgeraldSlot.SOCIAL,
                Pos.MISC to FitzgeraldSlot.MISC,
            )
        Pos.entries.forEach { pos ->
            assertEquals("Unmapped POS $pos", expected.getValue(pos), FitzgeraldSlot.fromPos(pos))
        }
    }
}
