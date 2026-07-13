package io.github.giuseppesorge.pictospeak.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giuseppesorge.pictospeak.R
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos

/**
 * Skeleton board (M0): selection strip + proposal bar + pictogram grid with Fitzgerald
 * coloring, wired end-to-end to the sentence engine. Real ARASAAC assets arrive at M2;
 * the confirm-to-speak bar arrives at M4 (INVARIANT-1 lives in :speech, not here).
 */
@Composable
fun BoardScreen(viewModel: BoardViewModel) {
    val state by viewModel.uiState.collectAsState()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(12.dp)) {
            SelectionStrip(state.selection)
            ProposalBar(
                state = state,
                onCandidateTapped = viewModel::onCandidateTapped,
                onBackspace = viewModel::onBackspace,
                onClear = viewModel::onClear,
            )
            PictogramGrid(onPictogramTapped = viewModel::onPictogramTapped)
        }
    }
}

@Composable
private fun SelectionStrip(selection: List<PictogramToken>) {
    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
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
    onCandidateTapped: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val proposal = state.candidates.getOrNull(state.selectedCandidateIndex)
        Text(
            text = proposal?.text ?: stringResource(R.string.board_proposal_placeholder),
            style = MaterialTheme.typography.headlineSmall,
            modifier =
                Modifier
                    .weight(1f)
                    .clickable {
                        if (state.candidates.size > 1) {
                            onCandidateTapped((state.selectedCandidateIndex + 1) % state.candidates.size)
                        }
                    },
        )
        TextButton(onClick = onBackspace) { Text(stringResource(R.string.board_backspace)) }
        TextButton(onClick = onClear) { Text(stringResource(R.string.board_clear)) }
    }
}

@Composable
private fun PictogramGrid(onPictogramTapped: (PictogramToken) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = DemoVocabulary.tokens, key = { it.id }, contentType = { "pictogram" }) { token ->
            Box(
                modifier =
                    Modifier
                        .height(96.dp)
                        .background(Color(FitzgeraldSlot.fromPos(token.pos).argb))
                        .clickable { onPictogramTapped(token) },
                contentAlignment = Alignment.Center,
            ) {
                Text(token.label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Placeholder vocabulary until the ARASAAC pipeline lands (M2). */
internal object DemoVocabulary {
    val tokens: List<PictogramToken> =
        listOf(
            PictogramToken("demo-io", "io", Pos.MISC, "io"),
            PictogramToken("demo-tu", "tu", Pos.MISC, "tu"),
            PictogramToken("demo-volere", "volere", Pos.VERB, "volere"),
            PictogramToken("demo-mangiare", "mangiare", Pos.VERB, "mangiare"),
            PictogramToken("demo-bere", "bere", Pos.VERB, "bere"),
            PictogramToken("demo-pizza", "pizza", Pos.NOUN, "pizza"),
            PictogramToken("demo-acqua", "acqua", Pos.NOUN, "acqua"),
            PictogramToken("demo-mamma", "mamma", Pos.NOUN, "mamma"),
            PictogramToken("demo-grande", "grande", Pos.DESCRIPTOR, "grande"),
            PictogramToken("demo-ciao", "ciao", Pos.SOCIAL, "ciao"),
            PictogramToken("demo-grazie", "grazie", Pos.SOCIAL, "grazie"),
            PictogramToken("demo-aiuto", "aiuto", Pos.SOCIAL, "aiuto"),
        )
}
