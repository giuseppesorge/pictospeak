package io.github.giuseppesorge.pictospeak.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.giuseppesorge.pictospeak.R
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.speech.TtsReadiness
import io.github.giuseppesorge.pictospeak.ui.theme.OnPictogram

/**
 * The board: selection strip + proposal bar (with the confirm-to-speak button, the app's
 * ONLY speech entry point) + pictogram grid with category-folder navigation.
 * testTags are exposed as resource ids for the Macrobenchmark journeys.
 */
@Composable
fun BoardScreen(
    viewModel: BoardViewModel,
    onAboutPressed: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val speaking by viewModel.speaking.collectAsState()
    val readiness by viewModel.ttsReadiness.collectAsState()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    // Keep content clear of the status/nav bars (the app draws edge-to-edge).
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { testTagsAsResourceId = true },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MessageArea(state = state, onCandidateTapped = viewModel::onCandidateTapped)
            ActionRow(
                state = state,
                speaking = speaking,
                readiness = readiness,
                onSpeakPressed = viewModel::onSpeakPressed,
                onStopPressed = viewModel::onStopPressed,
                onBackspace = viewModel::onBackspace,
                onClear = viewModel::onClear,
                onAboutPressed = onAboutPressed,
            )
            PictogramGrid(
                cells = state.cells,
                onPictogramTapped = viewModel::onPictogramTapped,
                onFolderTapped = viewModel::onFolderTapped,
                onBackToHome = viewModel::onBackToHome,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The message window: the selected pictograms as chips plus the composed sentence, on their
 * own rows in a tonal container so the sentence has room to read (crucial on a narrow phone).
 */
@Composable
private fun MessageArea(
    state: BoardUiState,
    onCandidateTapped: (Int) -> Unit,
) {
    val proposal = state.candidates.getOrNull(state.selectedCandidateIndex)
    val canCycle = state.candidates.size > 1
    val nextSuggestion = stringResource(R.string.board_next_suggestion)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.selection.isNotEmpty()) SelectionStrip(state.selection)
            Text(
                text = proposal?.text ?: stringResource(R.string.board_proposal_placeholder),
                style = MaterialTheme.typography.headlineSmall,
                color =
                    if (proposal == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        // Expose the cycle affordance only when there is another candidate —
                        // otherwise TalkBack would announce a dead double-tap.
                        .then(
                            if (canCycle) {
                                Modifier.clickable(onClickLabel = nextSuggestion, role = Role.Button) {
                                    onCandidateTapped((state.selectedCandidateIndex + 1) % state.candidates.size)
                                }
                            } else {
                                Modifier
                            },
                        ),
            )
            if (canCycle) {
                Text(
                    "${state.selectedCandidateIndex + 1}/${state.candidates.size} · $nextSuggestion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SelectionStrip(selection: List<PictogramToken>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(selection) { token ->
            Surface(
                color = Color(FitzgeraldSlot.fromPos(token.pos).argb),
                contentColor = OnPictogram,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    token.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * Actions on their own row so the sentence above keeps full width. Speak is the prominent,
 * full-weight primary target (also the ONLY route to audio — it invokes onSpeakPressed, the
 * ConfirmationGate call site); editing/nav actions are secondary text buttons.
 */
@Composable
private fun ActionRow(
    state: BoardUiState,
    speaking: Boolean,
    readiness: TtsReadiness,
    onSpeakPressed: () -> Unit,
    onStopPressed: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onAboutPressed: () -> Unit,
) {
    val proposal = state.candidates.getOrNull(state.selectedCandidateIndex)
    val voiceReady = readiness is TtsReadiness.Ready
    val hasSelection = state.selection.isNotEmpty()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // One persistent button whose label/action toggles, so TalkBack keeps focus and
            // announces the state change instead of the node being replaced.
            Button(
                onClick = { if (speaking) onStopPressed() else onSpeakPressed() },
                enabled = speaking || (proposal != null && voiceReady),
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .testTag("board-speak")
                        .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    stringResource(if (speaking) R.string.board_stop else R.string.board_speak),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = onBackspace, enabled = hasSelection) {
                Text(stringResource(R.string.board_backspace))
            }
            TextButton(onClick = onClear, enabled = hasSelection) {
                Text(stringResource(R.string.board_clear))
            }
            TextButton(onClick = onAboutPressed) { Text(stringResource(R.string.about_open)) }
        }
        // Never leave a silent Speak: tell the caregiver the voice needs setting up.
        if (!voiceReady && !speaking) {
            Text(
                stringResource(R.string.board_voice_not_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PictogramGrid(
    cells: List<BoardCellUi>,
    onPictogramTapped: (PictogramToken) -> Unit,
    onFolderTapped: (String) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("board-grid"),
    ) {
        items(
            items = cells,
            key = { cell ->
                when (cell) {
                    is BoardCellUi.Picto -> "p-${cell.token.id}"
                    is BoardCellUi.Folder -> "f-${cell.boardId}"
                    BoardCellUi.Back -> "back"
                }
            },
            contentType = { cell ->
                when (cell) {
                    is BoardCellUi.Picto -> "pictogram"
                    is BoardCellUi.Folder -> "folder"
                    BoardCellUi.Back -> "back"
                }
            },
        ) { cell ->
            when (cell) {
                is BoardCellUi.Picto -> PictoCell(cell.token, onPictogramTapped)
                is BoardCellUi.Folder -> FolderCell(cell, onFolderTapped)
                BoardCellUi.Back -> BackCell(onBackToHome)
            }
        }
    }
}

@Composable
private fun PictoCell(
    token: PictogramToken,
    onPictogramTapped: (PictogramToken) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .background(Color(FitzgeraldSlot.fromPos(token.pos).argb))
                .heightIn(min = 48.dp)
                // TalkBack reads the whole cell as one labelled button.
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = token.label
                }.clickable { onPictogramTapped(token) }
                .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = "file:///android_asset/arasaac/${token.id}.png",
            contentDescription = null,
            modifier = Modifier.size(88.dp),
        )
        Text(
            token.label,
            style = MaterialTheme.typography.labelLarge,
            // Fixed dark text: the Fitzgerald cell color is bright in both themes.
            color = OnPictogram,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FolderCell(
    folder: BoardCellUi.Folder,
    onFolderTapped: (String) -> Unit,
) {
    // Folders are chrome, not content — they follow the theme (adapt to dark mode).
    Column(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant)
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = folder.name
                }.clickable { onFolderTapped(folder.boardId) }
                .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (folder.icon != null) {
            AsyncImage(
                model = "file:///android_asset/arasaac/${folder.icon.id}.png",
                contentDescription = null,
                modifier = Modifier.size(88.dp),
            )
        } else {
            Box(modifier = Modifier.size(88.dp))
        }
        Text(
            "📁 ${folder.name}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BackCell(onBackToHome: () -> Unit) {
    val backLabel = stringResource(R.string.board_back_home)
    Column(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant)
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = backLabel
                }.clickable { onBackToHome() }
                .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Text("⬅️", style = MaterialTheme.typography.displaySmall)
        }
        Text(
            stringResource(R.string.board_back_home),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
