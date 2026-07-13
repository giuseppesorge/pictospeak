package io.github.giuseppesorge.pictospeak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The single app theme (M3). Follows the system light/dark setting; the palette is a fixed
 * brand scheme (see [LightColors] / [DarkColors]) rather than dynamic color, so the aid looks
 * the same on every device — important for users and caregivers who learn the interface.
 */
@Composable
fun PictoSpeakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
