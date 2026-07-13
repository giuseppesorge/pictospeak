package io.github.giuseppesorge.pictospeak.ui.settings

import io.github.giuseppesorge.pictospeak.llm.DeviceCapability

/** Everything the settings screen needs to render the optional-LLM section (play flavor). */
data class LlmSettingsState(
    val supported: Boolean,
    val capability: DeviceCapability?,
    val modelName: String?,
)
