package io.github.giuseppesorge.pictospeak.ui.about

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giuseppesorge.pictospeak.R

/**
 * Attribution and license surface — a definition-of-done requirement (CLAUDE.md rule 4).
 * The ARASAAC attribution below is the official wording and must never be reworded.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.board_back_home)) }
            Text("PictoSpeak", style = MaterialTheme.typography.headlineMedium)
            // Caregiver gate: long-press to reach Settings, so a user cannot open it by
            // accident (a tap only shows a hint).
            var showHint by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showHint = true },
                modifier =
                    Modifier
                        .heightIn(min = 48.dp)
                        .combinedClickable(onClick = { showHint = true }, onLongClick = onOpenSettings),
            ) { Text(stringResource(R.string.settings_open_hold)) }
            if (showHint) {
                Text(stringResource(R.string.settings_hold_hint), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            HorizontalDivider()

            Text(
                stringResource(R.string.about_symbols_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Text(ARASAAC_ATTRIBUTION_EN, style = MaterialTheme.typography.bodyMedium)
            Text(
                ARASAAC_ATTRIBUTION_IT,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                stringResource(R.string.about_license_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Text(stringResource(R.string.about_license_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Official ARASAAC wording — legal text, deliberately NOT translatable resources.
private const val ARASAAC_ATTRIBUTION_EN =
    "The pictographic symbols used are the property of the Government of Aragon and have " +
        "been created by Sergio Palao for ARASAAC (https://arasaac.org), which distributes " +
        "them under Creative Commons License BY-NC-SA."
private const val ARASAAC_ATTRIBUTION_IT =
    "I simboli pittografici utilizzati sono di proprietà del Governo di Aragona e sono " +
        "stati creati da Sergio Palao per ARASAAC (https://arasaac.org), che li distribuisce " +
        "sotto Licenza Creative Commons BY-NC-SA."
