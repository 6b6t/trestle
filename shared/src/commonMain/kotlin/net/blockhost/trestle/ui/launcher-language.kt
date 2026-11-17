package net.blockhost.trestle.ui

import androidx.compose.runtime.Composable
import net.blockhost.trestle.app.LauncherLanguage

/**
 * Shows [content] in [language]. The platform locale changes and [content] is composed again, so
 * string resources load from the matching translation.
 */
@Composable
internal expect fun LauncherLanguageEnvironment(language: LauncherLanguage, content: @Composable () -> Unit)
