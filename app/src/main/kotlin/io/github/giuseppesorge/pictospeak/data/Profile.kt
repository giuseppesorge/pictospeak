package io.github.giuseppesorge.pictospeak.data

import kotlinx.serialization.Serializable

/**
 * Caregiver-editable settings for one user. Deliberately small and versioned; persisted as
 * JSON in filesDir (ProfileRepository). New fields must be nullable or defaulted so old
 * files still load.
 */
@Serializable
data class Profile(
    val schemaVersion: Int = 1,
    /** Active LanguagePack (docs/language-packs.md): "it" or "en". */
    val language: String = "it",
    /** Speech rate; <1 is slower. Default slightly slow for intelligibility. */
    val ttsRate: Float = 0.9f,
    val ttsPitch: Float = 1.0f,
    /** Speak a pictogram's own label when tapped (user-initiated; compatible with INVARIANT-1). */
    val speakLabelOnTap: Boolean = false,
    /** A short confirmation vibration on selection/confirm. Off by default (docs/backlog.md). */
    val hapticFeedback: Boolean = false,
    /** Grid cell minimum size in dp — larger = fewer, bigger cells. */
    val gridMinCellDp: Int = DEFAULT_GRID_MIN_CELL_DP,
    /** Optional on-device LLM refiner (play flavor + capable device only). Default off. */
    val llmEnabled: Boolean = false,
    /** Recorded acceptance of the imported model's license terms (llm/NOTICE-models.md). */
    val llmModelLicenseAccepted: Boolean = false,
    /** Set once the caregiver has completed the first-run setup. */
    val setupComplete: Boolean = false,
) {
    companion object {
        const val DEFAULT_GRID_MIN_CELL_DP = 112
        const val DEFAULT_LANGUAGE = "it"
        val SUPPORTED_LANGUAGES = listOf("it", "en")
    }
}
