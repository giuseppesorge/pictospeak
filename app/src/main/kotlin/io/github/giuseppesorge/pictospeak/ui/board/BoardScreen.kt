@file:Suppress("TooManyFunctions") // a Compose screen composed of many small @Composable functions

package io.github.giuseppesorge.pictospeak.ui.board

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
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
import io.github.giuseppesorge.pictospeak.nlg.api.CandidateSource
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.speech.TtsReadiness
import io.github.giuseppesorge.pictospeak.ui.theme.OnPictogram
import kotlinx.coroutines.delay

/**
 * The board: selection strip + proposal bar (with the confirm-to-speak button, the app's
 * ONLY speech entry point) + pictogram grid with category-folder navigation.
 * testTags are exposed as resource ids for the Macrobenchmark journeys.
 */
@Composable
fun BoardScreen(
    viewModel: BoardViewModel,
    onAboutPressed: () -> Unit,
    hapticEnabled: Boolean = false,
    gridColumns: Int = 4,
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
            MessageArea(
                state = state,
                onCandidateTapped = viewModel::onCandidateTapped,
                onRemoveSelection = viewModel::onRemoveSelection,
            )
            ActionRow(
                state = state,
                speaking = speaking,
                readiness = readiness,
                hapticEnabled = hapticEnabled,
                onSpeakPressed = viewModel::onSpeakPressed,
                onStopPressed = viewModel::onStopPressed,
                onBackspace = viewModel::onBackspace,
                onClear = viewModel::onClear,
                onAboutPressed = onAboutPressed,
            )
            // Category navigation (folders + Back) lives in a FIXED bar above the grid, so it
            // is always reachable without scrolling past the vocabulary — better for motor and
            // switch/dwell access (docs/ui-conventions.md).
            val navCells = state.cells.filterNot { it is BoardCellUi.Picto }
            if (navCells.isNotEmpty()) {
                CategoryNav(
                    cells = navCells,
                    onFolderTapped = viewModel::onFolderTapped,
                    onBackToHome = viewModel::onBackToHome,
                )
            }
            PictogramGrid(
                cells = state.cells.filterIsInstance<BoardCellUi.Picto>(),
                columns = gridColumns,
                onPictogramTapped = viewModel::onPictogramTapped,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** A fixed, horizontally-scrolling bar of category folders (and Back) — never scrolls away. */
@Composable
private fun CategoryNav(
    cells: List<BoardCellUi>,
    onFolderTapped: (String) -> Unit,
    onBackToHome: () -> Unit,
) {
    val backLabel = stringResource(R.string.board_back_home)
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(cells) { _, cell ->
            when (cell) {
                is BoardCellUi.Folder -> CategoryChip(cell.name, cell.icon) { onFolderTapped(cell.boardId) }
                BoardCellUi.Back -> CategoryChip(backLabel, icon = null, isBack = true, onClick = onBackToHome)
                is BoardCellUi.Picto -> Unit // partitioned out before this composable
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    icon: PictogramToken?,
    isBack: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = name
                }.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isBack) {
                Text("⬅️", style = MaterialTheme.typography.titleLarge)
            } else if (icon != null) {
                AsyncImage(
                    model = "file:///android_asset/arasaac/${icon.id}.png",
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    onRemoveSelection: (Int) -> Unit,
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
            if (state.selection.isNotEmpty()) SelectionStrip(state.selection, onRemoveSelection)
            // Label non-template proposals so the user/caregiver can tell where a sentence came
            // from before speaking it (SentenceCandidate.CandidateSource contract).
            proposal?.source?.let { SourceBadge(it) }
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
                        // TalkBack: the proposal is the only composing-time feedback (INVARIANT-1
                        // forbids auto-speak), so announce it as it changes.
                        .semantics { liveRegion = LiveRegionMode.Polite }
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

/** A small label marking a non-template proposal (AI suggestion or literal fallback). */
@Composable
private fun SourceBadge(source: CandidateSource) {
    val label =
        when (source) {
            CandidateSource.LLM -> stringResource(R.string.board_source_llm)
            CandidateSource.FALLBACK_CONCAT -> stringResource(R.string.board_source_concat)
            CandidateSource.TEMPLATE -> return
        }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SelectionStrip(
    selection: List<PictogramToken>,
    onRemove: (Int) -> Unit,
) {
    val removeLabel = stringResource(R.string.board_remove_selection)
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(selection) { index, token ->
            Surface(
                color = Color(FitzgeraldSlot.fromPos(token.pos).argb),
                contentColor = OnPictogram,
                shape = MaterialTheme.shapes.small,
                // In-place repair: tap a chip to remove that pictogram (docs/ui-conventions.md).
                modifier =
                    Modifier.clickable(
                        onClickLabel = "$removeLabel ${token.label}",
                        role = Role.Button,
                    ) { onRemove(index) },
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionRow(
    state: BoardUiState,
    speaking: Boolean,
    readiness: TtsReadiness,
    hapticEnabled: Boolean,
    onSpeakPressed: () -> Unit,
    onStopPressed: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onAboutPressed: () -> Unit,
) {
    val proposal = state.candidates.getOrNull(state.selectedCandidateIndex)
    val voiceReady = readiness is TtsReadiness.Ready
    val hasSelection = state.selection.isNotEmpty()
    var showClearHint by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeakStopButton(
                speaking = speaking,
                enabled = speaking || (proposal != null && voiceReady),
                hapticEnabled = hapticEnabled,
                onSpeakPressed = onSpeakPressed,
                onStopPressed = onStopPressed,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBackspace, enabled = hasSelection) {
                Text(stringResource(R.string.board_backspace))
            }
            ClearButton(
                enabled = hasSelection,
                onShowHint = { showClearHint = true },
                onClear = {
                    showClearHint = false
                    onClear()
                },
            )
            TextButton(onClick = onAboutPressed) { Text(stringResource(R.string.about_open)) }
        }
        if (showClearHint) {
            LaunchedEffect(Unit) {
                delay(CLEAR_HINT_MS)
                showClearHint = false
            }
            Text(
                stringResource(R.string.board_clear_hold_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Never leave a silent Speak: tell the caregiver the voice needs setting up.
        if (!voiceReady && !speaking) {
            Text(
                stringResource(R.string.board_voice_not_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * The single persistent Speak/Stop button — label and action toggle so TalkBack keeps focus
 * and announces the change. Speak is the app's ONLY route to audio (it invokes onSpeakPressed,
 * the ConfirmationGate call site); an optional confirmation haptic fires on confirm only.
 */
@Composable
private fun SpeakStopButton(
    speaking: Boolean,
    enabled: Boolean,
    hapticEnabled: Boolean,
    onSpeakPressed: () -> Unit,
    onStopPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Button(
        onClick = {
            if (speaking) {
                onStopPressed()
            } else {
                // A short confirmation tick on the app's most consequential action
                // (caregiver-toggleable, honours system haptic settings; no permission).
                if (hapticEnabled) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onSpeakPressed()
            }
        },
        enabled = enabled,
        modifier =
            modifier
                .heightIn(min = 56.dp)
                .testTag("board-speak")
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            stringResource(if (speaking) R.string.board_stop else R.string.board_speak),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * Clear wipes the WHOLE message, so it is long-press-only (a tap just shows a hint) — an
 * accidental one-tap must never destroy a composed sentence with no undo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClearButton(
    enabled: Boolean,
    onShowHint: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .combinedClickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.board_clear_hold_hint),
                    role = Role.Button,
                    onClick = onShowHint,
                    onLongClick = onClear,
                ).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.board_clear),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val CLEAR_HINT_MS = 2500L

@Composable
private fun PictogramGrid(
    cells: List<BoardCellUi.Picto>,
    columns: Int,
    onPictogramTapped: (PictogramToken) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        // Fixed (not Adaptive) so a pictogram keeps its position across rotation / screen size.
        // coerceAtLeast(1): Fixed(0) throws — the profile is clamped, this guards every other path.
        columns = GridCells.Fixed(columns.coerceAtLeast(1)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("board-grid"),
    ) {
        items(
            items = cells,
            key = { "p-${it.token.id}" },
            contentType = { "pictogram" },
        ) { cell ->
            PictoCell(cell.token, onPictogramTapped)
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
