package io.github.giuseppesorge.pictospeak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.giuseppesorge.pictospeak.nlg.api.PictogramToken
import io.github.giuseppesorge.pictospeak.nlg.api.Pos
import io.github.giuseppesorge.pictospeak.ui.about.AboutScreen
import io.github.giuseppesorge.pictospeak.ui.board.BoardScreen
import io.github.giuseppesorge.pictospeak.ui.board.BoardViewModel
import io.github.giuseppesorge.pictospeak.ui.settings.LlmSettingsState
import io.github.giuseppesorge.pictospeak.ui.settings.SettingsScreen
import io.github.giuseppesorge.pictospeak.ui.setup.TtsSetupScreen
import io.github.giuseppesorge.pictospeak.ui.theme.PictoSpeakTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        // Android 15+ forces edge-to-edge; opt in explicitly and let each screen inset its
        // own content (WindowInsets.safeDrawing) so nothing hides under the status/nav bars.
        enableEdgeToEdge()
        val container = (application as AacApplication).container
        setContent { PictoSpeakTheme { App(container) } }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Give back the LLM engine's native memory under pressure; it reloads on next use.
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            (application as AacApplication).container.releaseLlm()
        }
    }
}

@Composable
private fun App(container: AppContainer) {
    val profile by container.profile.collectAsState()
    var screen by remember { mutableStateOf<Screen>(if (profile.setupComplete) Screen.Board else Screen.Setup) }

    // Language change re-evaluates readiness (which voice, offline?). Rate/pitch are cheap
    // setters kept separate, so moving a slider never triggers a voices re-scan.
    LaunchedEffect(profile.language) {
        container.ttsGateway.setLanguage(Locale.forLanguageTag(profile.language))
    }
    LaunchedEffect(profile.ttsRate, profile.ttsPitch) {
        container.ttsGateway.setSpeechRate(profile.ttsRate)
        container.ttsGateway.setPitch(profile.ttsPitch)
    }

    when (screen) {
        Screen.Board -> {
            // Keyed by language, speak-on-tap AND llm-enabled so a settings change takes
            // effect on return (the VM captures these; same-key reuse would otherwise keep
            // the stale values until app restart).
            val boardViewModel: BoardViewModel =
                viewModel(key = "board-${profile.language}-${profile.speakLabelOnTap}-${profile.llmEnabled}") {
                    BoardViewModel(
                        sentenceEngine = container.sentenceEngine(profile.language),
                        sentenceRefiner = container.sentenceRefiner(profile),
                        ttsGateway = container.ttsGateway,
                        vocabularyRepository = container.vocabularyRepository(profile.language),
                        speakLabelOnTap = profile.speakLabelOnTap,
                    )
                }
            BoardScreen(
                boardViewModel,
                onAboutPressed = { screen = Screen.About },
                hapticEnabled = profile.hapticFeedback,
                gridColumns = profile.gridColumns,
            )
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
    val scope = rememberCoroutineScope()
    // SAF streams talk to another (possibly cloud-backed) provider process — that IPC can
    // block for seconds, so keep it off the main thread.
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        out.write(container.profileRepository.serialize(profile).toByteArray())
                    }
                }
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    val text =
                        context.contentResolver
                            .openInputStream(it)
                            ?.use { ins -> ins.readBytes().decodeToString() }
                    text?.let { body ->
                        container.profileRepository.deserialize(body)?.let { imported ->
                            container.updateProfile { imported }
                        }
                    }
                }
            }
        }

    // LLM model import (play flavor only). Any picked file is copied privately; the caregiver
    // then accepts its license and enables the feature. Never bundled (llm/NOTICE-models.md).
    var modelName by remember { mutableStateOf(container.modelStore.current()?.fileName) }
    val modelImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { ins ->
                        val info = container.modelStore.importFrom(ins, it.lastPathSegment ?: "model.litertlm")
                        modelName = info.fileName
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
        llm =
            LlmSettingsState(
                supported = container.llmFlavor,
                capability = if (container.llmFlavor) container.deviceCapability else null,
                modelName = modelName,
                modelFits = modelName == null || container.importedModelFitsDevice(),
            ),
        onImportModel = { modelImportLauncher.launch(arrayOf("*/*")) },
    )
}

/** A throwaway token used only to sound out the voice on the setup screen. */
private fun voiceTestToken(language: String): PictogramToken {
    val word = if (language == "en") "hello" else "ciao"
    return PictogramToken(id = "voice-test", lemma = word, pos = Pos.SOCIAL, label = word)
}
