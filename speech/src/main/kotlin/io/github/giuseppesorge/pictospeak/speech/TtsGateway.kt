package io.github.giuseppesorge.pictospeak.speech

import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import kotlinx.coroutines.flow.StateFlow

/**
 * The only path to audio output.
 *
 * INVARIANT-1: [speak] accepts ONLY a [ConfirmedUtterance]. There is no speak(String)
 * overload, and none may ever be added. Generated text reaches this interface exclusively
 * through [ConfirmationGate.confirm] in the user's confirmation tap handler.
 */
interface TtsGateway {
    val readiness: StateFlow<TtsReadiness>
    val speaking: StateFlow<Boolean>

    fun speak(utterance: ConfirmedUtterance)

    /**
     * Speaks the single label of a tapped pictogram (profile-gated word preview).
     * This is user-initiated speech of exactly the tapped word — not generated text —
     * and is therefore compatible with INVARIANT-1 by definition.
     */
    fun speakWordPreview(token: PictogramToken)

    fun stop()

    fun shutdown()
}
