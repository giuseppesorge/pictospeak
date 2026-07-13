package io.github.giuseppesorge.pictospeak.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.giuseppesorge.pictospeak.R
import io.github.giuseppesorge.pictospeak.data.Profile

/**
 * Caregiver settings: language pack, speech rate/pitch, tap-to-hear. Separated from the
 * user's communicating space; reached via the About/settings affordance. All changes are
 * applied and persisted immediately by the caller.
 */
@Composable
fun SettingsScreen(
    profile: Profile,
    onProfileChange: (Profile) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    llm: LlmSettingsState = LlmSettingsState(supported = false, capability = null, modelName = null),
    onImportModel: () -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.board_back_home)) }
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

            LanguageChooser(profile.language) { onProfileChange(profile.copy(language = it)) }

            LabeledSlider(
                label = stringResource(R.string.settings_rate),
                value = profile.ttsRate,
                range = RATE_MIN..RATE_MAX,
                onChange = { onProfileChange(profile.copy(ttsRate = it)) },
            )
            LabeledSlider(
                label = stringResource(R.string.settings_pitch),
                value = profile.ttsPitch,
                range = PITCH_MIN..PITCH_MAX,
                onChange = { onProfileChange(profile.copy(ttsPitch = it)) },
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // Merge the label and switch into one named toggle for TalkBack.
                        .toggleable(
                            value = profile.speakLabelOnTap,
                            role = Role.Switch,
                            onValueChange = { onProfileChange(profile.copy(speakLabelOnTap = it)) },
                        ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_speak_on_tap), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = profile.speakLabelOnTap, onCheckedChange = null)
            }

            if (llm.supported) {
                HorizontalDivider()
                LlmSection(
                    profile = profile,
                    onProfileChange = onProfileChange,
                    llm = llm,
                    onImportModel = onImportModel,
                )
            }

            HorizontalDivider()
            Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onExport) { Text(stringResource(R.string.settings_export)) }
                OutlinedButton(onClick = onImport) { Text(stringResource(R.string.settings_import)) }
            }
        }
    }
}

@Composable
private fun LanguageChooser(
    current: String,
    onSelect: (String) -> Unit,
) {
    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
    Column(modifier = Modifier.selectableGroup()) {
        Profile.SUPPORTED_LANGUAGES.forEach { lang ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = current == lang, role = Role.RadioButton) { onSelect(lang) }
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == lang, onClick = null)
                Text(
                    languageName(lang),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    // Drag updates only transient UI state; persist once on release. Otherwise every
    // intermediate tick would fire a synchronous main-thread save() + a TTS readiness
    // re-eval — jank/ANR on the 2GB floor device.
    var sliderValue by remember(value) { mutableStateOf(value) }
    Column {
        Text("$label  ${"%.1f".format(sliderValue)}", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onChange(sliderValue) },
            valueRange = range,
            // Give the slider a real name and announce the actual value, not Compose's
            // default "% of range" reading.
            modifier =
                Modifier.semantics {
                    contentDescription = label
                    stateDescription = "%.1f".format(sliderValue)
                },
        )
    }
}

@Composable
private fun languageName(lang: String): String =
    when (lang) {
        "it" -> stringResource(R.string.settings_language_it)
        "en" -> stringResource(R.string.settings_language_en)
        else -> lang
    }

/**
 * Optional on-device LLM controls (play flavor only). The feature stays off until every
 * gate is satisfied: an eligible device, an imported model, its license accepted, and the
 * explicit opt-in. Templates remain the product regardless (CLAUDE.md hard rule 3).
 */
@Composable
private fun LlmSection(
    profile: Profile,
    onProfileChange: (Profile) -> Unit,
    llm: LlmSettingsState,
    onImportModel: () -> Unit,
) {
    val eligible = llm.capability?.eligible == true
    val hasModel = llm.modelName != null

    Text(stringResource(R.string.settings_llm_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.settings_llm_desc), style = MaterialTheme.typography.bodyMedium)

    val ramGib = "%.1f".format(llm.capability?.totalMemGib ?: 0.0)
    Text(
        stringResource(
            if (eligible) R.string.settings_llm_device_ok else R.string.settings_llm_device_no,
            ramGib,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = if (eligible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
    )

    OutlinedButton(onClick = onImportModel, enabled = eligible) {
        Text(stringResource(R.string.settings_llm_import_model))
    }
    Text(
        llm.modelName?.let { stringResource(R.string.settings_llm_model_label, it) }
            ?: stringResource(R.string.settings_llm_no_model),
        style = MaterialTheme.typography.bodySmall,
    )

    SwitchRow(
        label = stringResource(R.string.settings_llm_license_accept),
        checked = profile.llmModelLicenseAccepted,
        enabled = hasModel,
        onCheckedChange = { onProfileChange(profile.copy(llmModelLicenseAccepted = it)) },
    )
    SwitchRow(
        label = stringResource(R.string.settings_llm_enable),
        checked = profile.llmEnabled,
        enabled = eligible && hasModel && profile.llmModelLicenseAccepted,
        onCheckedChange = { onProfileChange(profile.copy(llmEnabled = it)) },
    )
}

/** A label + Switch merged into one named toggle for TalkBack (see the speak-on-tap row). */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

private const val RATE_MIN = 0.5f
private const val RATE_MAX = 1.5f
private const val PITCH_MIN = 0.5f
private const val PITCH_MAX = 1.5f
