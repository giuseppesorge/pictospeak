package io.github.giuseppesorge.pictospeak.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.giuseppesorge.pictospeak.R
import io.github.giuseppesorge.pictospeak.speech.TtsReadiness

/**
 * First-run TTS readiness wizard. A mute AAC device is the worst field failure, so
 * readiness is SHOWN, not assumed: the caregiver sees the voice state and can install the
 * voice data or test speech before finishing setup (docs/architecture.md, TTS section).
 */
@Composable
fun TtsSetupScreen(
    readiness: TtsReadiness,
    onInstallVoice: () -> Unit,
    onTestVoice: () -> Unit,
    onOpenSettings: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(statusText(readiness), style = MaterialTheme.typography.bodyLarge)

            when (readiness) {
                is TtsReadiness.Ready ->
                    Button(onClick = onTestVoice) { Text(stringResource(R.string.setup_test_voice)) }
                TtsReadiness.MissingVoiceData ->
                    Button(onClick = onInstallVoice) { Text(stringResource(R.string.setup_install_voice)) }
                TtsReadiness.NoLanguageSupport, TtsReadiness.NoEngine ->
                    OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.setup_open_settings)) }
                TtsReadiness.Initializing -> Unit
            }

            OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings_title)) }
            Button(
                onClick = onDone,
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(stringResource(R.string.setup_continue)) }
        }
    }
}

@Composable
private fun statusText(readiness: TtsReadiness): String =
    when (readiness) {
        TtsReadiness.Initializing -> stringResource(R.string.setup_status_initializing)
        is TtsReadiness.Ready ->
            if (readiness.isOfflineVoice) {
                stringResource(R.string.setup_status_ready_offline)
            } else {
                stringResource(R.string.setup_status_ready_online)
            }
        TtsReadiness.MissingVoiceData -> stringResource(R.string.setup_status_missing_voice)
        TtsReadiness.NoLanguageSupport -> stringResource(R.string.setup_status_no_language)
        TtsReadiness.NoEngine -> stringResource(R.string.setup_status_no_engine)
    }
