package io.github.giuseppesorge.pictospeak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.ui.about.AboutScreen
import io.github.giuseppesorge.pictospeak.ui.board.BoardScreen
import io.github.giuseppesorge.pictospeak.ui.board.BoardViewModel
import io.github.giuseppesorge.pictospeak.ui.settings.SettingsScreen
import io.github.giuseppesorge.pictospeak.ui.setup.TtsSetupScreen
import java.util.Locale

/**
 * Single activity; navigation is a sealed class + `when` — deliberately no navigation
 * library (docs/adr/0007). The active LanguagePack, TTS locale and speech rate all follow
 * the persisted profile.
 */
sealed interface Screen {
    data object Board : Screen

    data object About : Screen

    data object Settings : Screen

    data object Setup : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AacApplication).container
        setContent { MaterialTheme { App(container) } }
    }
}

@Composable
private fun App(container: AppContainer) {
    val profile by container.profile.collectAsState()
    var screen by remember { mutableStateOf<Screen>(if (profile.setupComplete) Screen.Board else Screen.Setup) }

    // The voice follows the profile: locale, rate and pitch are applied whenever they change.
    LaunchedEffect(profile.language, profile.ttsRate, profile.ttsPitch) {
        container.ttsGateway.setLanguage(Locale.forLanguageTag(profile.language))
        container.ttsGateway.setSpeechRate(profile.ttsRate)
        container.ttsGateway.setPitch(profile.ttsPitch)
    }

    when (screen) {
        Screen.Board -> {
            // Keyed by language so switching packs rebuilds the board with the right engine.
            val boardViewModel: BoardViewModel =
                viewModel(key = "board-${profile.language}") {
                    BoardViewModel(
                        sentenceEngine = container.sentenceEngine(profile.language),
                        sentenceRefiner = container.sentenceRefiner,
                        ttsGateway = container.ttsGateway,
                        vocabularyRepository = container.vocabularyRepository(profile.language),
                        speakLabelOnTap = profile.speakLabelOnTap,
                    )
                }
            BoardScreen(boardViewModel, onAboutPressed = { screen = Screen.About })
        }
        Screen.About ->
            AboutScreen(
                onBack = { screen = Screen.Board },
                onOpenSettings = { screen = Screen.Settings },
            )
        Screen.Settings -> SettingsRoute(container, profile) { screen = Screen.Board }
        Screen.Setup -> {
            val readiness by container.ttsGateway.readiness.collectAsState()
            TtsSetupScreen(
                readiness = readiness,
                onInstallVoice = { container.ttsGateway.installVoiceData() },
                onTestVoice = { container.ttsGateway.speakWordPreview(voiceTestToken(profile.language)) },
                onOpenSettings = { screen = Screen.Settings },
                onDone = {
                    container.updateProfile { it.copy(setupComplete = true) }
                    screen = Screen.Board
                },
            )
        }
    }
}

@Composable
private fun SettingsRoute(
    container: AppContainer,
    profile: io.github.giuseppesorge.pictospeak.data.Profile,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(container.profileRepository.serialize(profile).toByteArray())
                }
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                val text = context.contentResolver.openInputStream(it)?.use { ins -> ins.readBytes().decodeToString() }
                text?.let { body ->
                    container.profileRepository.deserialize(body)?.let { imported ->
                        container.updateProfile { imported }
                    }
                }
            }
        }
    SettingsScreen(
        profile = profile,
        onProfileChange = { updated -> container.updateProfile { updated } },
        onExport = { exportLauncher.launch("pictospeak-settings.json") },
        onImport = { importLauncher.launch(arrayOf("application/json")) },
        onBack = onBack,
    )
}

/** A throwaway token used only to sound out the voice on the setup screen. */
private fun voiceTestToken(language: String): PictogramToken {
    val word = if (language == "en") "hello" else "ciao"
    return PictogramToken(id = "voice-test", lemma = word, pos = Pos.SOCIAL, label = word)
}
