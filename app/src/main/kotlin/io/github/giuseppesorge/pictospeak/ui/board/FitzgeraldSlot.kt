package io.github.giuseppesorge.pictospeak.ui.board

import io.github.giuseppesorge.pictospeak.nlg.api.Pos

/**
 * Modified Fitzgerald key color coding — the AAC convention users and therapists expect.
 * One documented palette, applied consistently (docs/ui-conventions.md). Colors are ARGB
 * longs so this stays a pure JVM-testable mapping with no Compose dependency.
 */
enum class FitzgeraldSlot(
    val argb: Long,
) {
    PEOPLE(0xFFFFF176), // yellow
    NOUNS(0xFFFFB74D), // orange
    VERBS(0xFF81C784), // green
    DESCRIPTORS(0xFF64B5F6), // blue
    SOCIAL(0xFFF06292), // pink
    MISC(0xFFFFFFFF), // white
    ;

    companion object {
        fun fromPos(pos: Pos): FitzgeraldSlot =
            when (pos) {
                Pos.PROPER_NAME -> PEOPLE
                Pos.NOUN -> NOUNS
                Pos.VERB -> VERBS
                Pos.DESCRIPTOR -> DESCRIPTORS
                Pos.SOCIAL -> SOCIAL
                Pos.MISC -> MISC
            }
    }
}
