package io.github.giuseppesorge.pictospeak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.giuseppesorge.pictospeak.ui.about.AboutScreen
import io.github.giuseppesorge.pictospeak.ui.board.BoardScreen
import io.github.giuseppesorge.pictospeak.ui.board.BoardViewModel

/**
 * Single activity; navigation is a sealed class + `when` — deliberately no navigation
 * library (docs/adr/0007). New screens (Settings, Setup) add a Screen subtype.
 */
sealed interface Screen {
    data object Board : Screen

    data object About : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AacApplication).container
        setContent {
            MaterialTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Board) }
                val boardViewModel: BoardViewModel =
                    viewModel {
                        BoardViewModel(
                            sentenceEngine = container.sentenceEngine,
                            sentenceRefiner = container.sentenceRefiner,
                            ttsGateway = container.ttsGateway,
                            vocabularyRepository = container.vocabularyRepository,
                        )
                    }
                when (screen) {
                    Screen.Board -> BoardScreen(boardViewModel, onAboutPressed = { screen = Screen.About })
                    Screen.About -> AboutScreen(onBack = { screen = Screen.Board })
                }
            }
        }
    }
}
