package io.github.giuseppesorge.pictospeak.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
                    .padding(12.dp)
                    .semantics { testTagsAsResourceId = true },
        ) {
            SelectionStrip(state.selection)
            ProposalBar(
                state = state,
                speaking = speaking,
                readiness = readiness,
                onCandidateTapped = viewModel::onCandidateTapped,
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
            )
        }
    }
}

@Composable
private fun SelectionStrip(selection: List<PictogramToken>) {
    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(selection) { token ->
            Box(
                modifier =
                    Modifier
                        .background(Color(FitzgeraldSlot.fromPos(token.pos).argb))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(token.label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ProposalBar(
    state: BoardUiState,
    speaking: Boolean,
    readiness: TtsReadiness,
    onCandidateTapped: (Int) -> Unit,
    onSpeakPressed: () -> Unit,
    onStopPressed: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onAboutPressed: () -> Unit,
) {
    val proposal = state.candidates.getOrNull(state.selectedCandidateIndex)
    val voiceReady = readiness is TtsReadiness.Ready
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val canCycle = state.candidates.size > 1
            val nextSuggestion = stringResource(R.string.board_next_suggestion)
            Text(
                text = proposal?.text ?: stringResource(R.string.board_proposal_placeholder),
                style = MaterialTheme.typography.headlineSmall,
                modifier =
                    Modifier
                        .weight(1f)
                        // Only expose the cycle affordance when there is another candidate —
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
            // One persistent button whose label/action toggles, so TalkBack keeps focus and
            // announces the state change instead of the node being replaced. Speak stays the
            // ONLY route to audio: it invokes onSpeakPressed (the ConfirmationGate call site).
            Button(
                onClick = { if (speaking) onStopPressed() else onSpeakPressed() },
                enabled = speaking || (proposal != null && voiceReady),
                modifier =
                    Modifier
                        .testTag("board-speak")
                        .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    stringResource(if (speaking) R.string.board_stop else R.string.board_speak),
                )
            }
            TextButton(onClick = onBackspace) { Text(stringResource(R.string.board_backspace)) }
            TextButton(onClick = onClear) { Text(stringResource(R.string.board_clear)) }
            TextButton(onClick = onAboutPressed) { Text(stringResource(R.string.about_open)) }
        }
        // Never leave a silent Speak: tell the caregiver the voice needs setting up.
        if (!voiceReady && !speaking) {
            Text(
                stringResource(R.string.board_voice_not_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
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
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("board-grid"),
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
    Column(
        modifier =
            Modifier
                .background(FOLDER_BACKGROUND)
                .border(2.dp, FOLDER_BORDER)
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
                .background(FOLDER_BACKGROUND)
                .border(2.dp, FOLDER_BORDER)
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
        Text(stringResource(R.string.board_back_home), style = MaterialTheme.typography.labelLarge)
    }
}

@Suppress("MagicNumber") // single documented folder palette (docs/ui-conventions.md)
private val FOLDER_BACKGROUND = Color(0xFFEDEDED)

@Suppress("MagicNumber")
private val FOLDER_BORDER = Color(0xFFBDBDBD)
