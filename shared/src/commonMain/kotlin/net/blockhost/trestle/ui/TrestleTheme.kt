package net.blockhost.trestle.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.roboto_variable
import org.jetbrains.compose.resources.Font

// Dark palette: cool ink surfaces, a warm brass accent, and mint for ready states.
private val Ink = Color(0xFF0D0F14)
private val InkLow = Color(0xFF13161D)
private val InkContainer = Color(0xFF181C24)
private val InkHigh = Color(0xFF20252F)
private val InkHighest = Color(0xFF2A303C)
private val Brass = Color(0xFFF0A93E)
private val Chalk = Color(0xFFE9ECF2)
private val Muted = Color(0xFF9BA3B4)
private val Rule = Color(0xFF737B8D)
private val SoftRule = Color(0xFF2B313D)
private val Mint = Color(0xFF5BD6A8)

// Light palette: cool paper surfaces with the same brass and mint roles.
private val Paper = Color(0xFFF5F6F9)
private val PaperText = Color(0xFF181B22)
private val DeepBrass = Color(0xFF8A5800)

private val PrimaryFixed = Color(0xFFFFDDAA)
private val PrimaryFixedDim = Color(0xFFF0B963)
private val OnPrimaryFixed = Color(0xFF2A1800)
private val OnPrimaryFixedVariant = Color(0xFF5E3D00)
private val SecondaryFixed = Color(0xFFF3E1C6)
private val SecondaryFixedDim = Color(0xFFD6C3A6)
private val OnSecondaryFixed = Color(0xFF241A0B)
private val OnSecondaryFixedVariant = Color(0xFF524430)
private val TertiaryFixed = Color(0xFFB4F4DA)
private val TertiaryFixedDim = Color(0xFF5BD6A8)
private val OnTertiaryFixed = Color(0xFF002117)
private val OnTertiaryFixedVariant = Color(0xFF005139)

private val TrestleDarkColors = darkColorScheme(
    primary = Brass,
    onPrimary = Color(0xFF2A1800),
    primaryContainer = Color(0xFF4A3310),
    onPrimaryContainer = PrimaryFixed,
    secondary = Color(0xFFD6C3A6),
    onSecondary = Color(0xFF241A0B),
    secondaryContainer = Color(0xFF2E2719),
    onSecondaryContainer = Color(0xFFF3E1C6),
    tertiary = Mint,
    onTertiary = OnTertiaryFixed,
    tertiaryContainer = Color(0xFF123B2E),
    onTertiaryContainer = TertiaryFixed,
    background = Ink,
    onBackground = Chalk,
    surface = InkLow,
    onSurface = Chalk,
    surfaceVariant = InkHigh,
    onSurfaceVariant = Muted,
    surfaceTint = Brass,
    surfaceBright = InkHighest,
    surfaceDim = Ink,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkLow,
    surfaceContainer = InkContainer,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHighest,
    outline = Rule,
    outlineVariant = SoftRule,
    error = Color(0xFFFF8A7A),
    onError = Color(0xFF3A0905),
    errorContainer = Color(0xFF3D1A17),
    onErrorContainer = Color(0xFFFFB4A9),
    inverseSurface = Chalk,
    inverseOnSurface = Ink,
    inversePrimary = DeepBrass,
    scrim = Color.Black,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

private val TrestleLightColors = lightColorScheme(
    primary = DeepBrass,
    onPrimary = Color.White,
    primaryContainer = PrimaryFixed,
    onPrimaryContainer = OnPrimaryFixed,
    secondary = Color(0xFF6B5A40),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E7CF),
    onSecondaryContainer = OnSecondaryFixed,
    tertiary = Color(0xFF006C4E),
    onTertiary = Color.White,
    tertiaryContainer = TertiaryFixed,
    onTertiaryContainer = OnTertiaryFixed,
    background = Paper,
    onBackground = PaperText,
    surface = Color(0xFFFBFBFD),
    onSurface = PaperText,
    surfaceVariant = Color(0xFFE6E9F0),
    onSurfaceVariant = Color(0xFF555D6E),
    surfaceTint = DeepBrass,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDCDFE6),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F2F6),
    surfaceContainer = Color(0xFFEAEDF2),
    surfaceContainerHigh = Color(0xFFE3E6EC),
    surfaceContainerHighest = Color(0xFFDCDFE6),
    outline = Color(0xFF747C8C),
    outlineVariant = Color(0xFFCDD2DC),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2B2F38),
    inverseOnSurface = Color(0xFFF0F2F6),
    inversePrimary = Brass,
    scrim = Color.Black,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

internal fun trestleColorScheme(accentColor: Color? = null, darkTheme: Boolean = true, highContrast: Boolean = false): ColorScheme {
    val base = if (darkTheme) TrestleDarkColors else TrestleLightColors
    val contrastedBase = base.withHighContrast(highContrast)
    if (accentColor == null) return contrastedBase
    // Primary is also used for small text, so it must stay readable on the raised surfaces it sits on.
    val primary = primaryAccent(accentColor, contrastedBase.surfaceContainerHigh, if (darkTheme) Chalk else Ink)
    val primaryContainer = lerp(contrastedBase.surface, primary, if (darkTheme) 0.32f else 0.18f)
    val fixedDim = primaryAccent(accentColor, Ink, Chalk)
    val fixed = lerp(Chalk, fixedDim, 0.24f)
    return contrastedBase.copy(
        primary = primary,
        onPrimary = contentColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contentColor(primaryContainer),
        surfaceTint = primary,
        inversePrimary = accessibleAccent(
            accentColor,
            contrastedBase.inverseSurface,
            contrastedBase.inverseOnSurface,
        ),
        primaryFixed = fixed,
        primaryFixedDim = fixedDim,
        onPrimaryFixed = contentColor(fixed),
        onPrimaryFixedVariant = contentColor(fixedDim),
    )
}

internal fun ColorScheme.withHighContrast(enabled: Boolean): ColorScheme = if (enabled) {
    copy(
        outline = onSurface,
        outlineVariant = lerp(surface, onSurface, 0.62f),
        onSurfaceVariant = onSurface,
    )
} else {
    this
}

private fun primaryAccent(accentColor: Color, surface: Color, target: Color): Color {
    var candidate = accentColor.copy(alpha = 1f)
    repeat(12) {
        val surfaceContrast = contrastRatio(candidate, surface)
        val contentContrast = contrastRatio(contentColor(candidate), candidate)
        if (surfaceContrast >= MINIMUM_ACCENT_CONTRAST && contentContrast >= MINIMUM_CONTENT_CONTRAST) {
            return candidate
        }
        candidate = lerp(candidate, target, 0.16f)
    }
    return candidate
}

private fun accessibleAccent(accentColor: Color, background: Color, target: Color): Color {
    var candidate = accentColor.copy(alpha = 1f)
    repeat(12) {
        if (contrastRatio(candidate, background) >= MINIMUM_ACCENT_CONTRAST) return candidate
        candidate = lerp(candidate, target, 0.16f)
    }
    return candidate
}

private fun contentColor(background: Color): Color = if (contrastRatio(Ink, background) >= contrastRatio(Chalk, background)) Ink else Chalk

internal fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private const val MINIMUM_ACCENT_CONTRAST = 4.5f
private const val MINIMUM_CONTENT_CONTRAST = 4.5f

private val TrestleShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private object ReducedMotionScheme : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}

@Composable
private fun trestleTypography(): Typography {
    val roboto = FontFamily(
        Font(Res.font.roboto_variable, FontWeight.Normal),
        Font(Res.font.roboto_variable, FontWeight.Medium),
        Font(Res.font.roboto_variable, FontWeight.Bold),
    )

    val base = Typography(fontFamily = roboto)
    return base.copy(
        displayLarge = base.displayLarge.byContent(),
        displayMedium = base.displayMedium.byContent(),
        displaySmall = base.displaySmall.byContent(FontWeight.Bold),
        headlineLarge = base.headlineLarge.byContent(FontWeight.Bold),
        headlineMedium = base.headlineMedium.byContent(FontWeight.Bold),
        headlineSmall = base.headlineSmall.byContent(FontWeight.Bold),
        titleLarge = base.titleLarge.byContent(FontWeight.Bold),
        titleMedium = base.titleMedium.byContent(FontWeight.Bold),
        titleSmall = base.titleSmall.byContent(FontWeight.Bold),
        bodyLarge = base.bodyLarge.byContent(),
        bodyMedium = base.bodyMedium.byContent(),
        bodySmall = base.bodySmall.byContent(),
        labelLarge = base.labelLarge.byContent(FontWeight.Bold),
        labelMedium = base.labelMedium.byContent(),
        labelSmall = base.labelSmall.byContent(),
    )
}

/**
 * Lays out each text in the direction of its first strong letter. Without this, Compose uses the layout direction,
 * so an English mod description in an Arabic window ends with its period on the wrong side. Right-to-left
 * translations that start with a Latin word or a placeholder begin with U+200F to keep their own direction.
 */
private fun TextStyle.byContent(fontWeight: FontWeight? = this.fontWeight) =
    copy(fontWeight = fontWeight, textDirection = TextDirection.Content)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun TrestleTheme(
    accentColor: Color? = null,
    colorSchemeOverride: ColorScheme? = null,
    preference: ThemePreference = ThemePreference.SYSTEM,
    systemDarkTheme: Boolean? = null,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    windowAdaptiveInfo: WindowAdaptiveInfo? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (preference) {
        ThemePreference.SYSTEM -> systemDarkTheme ?: isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
    val colorScheme = remember(colorSchemeOverride, accentColor, darkTheme, highContrast) {
        colorSchemeOverride?.withHighContrast(highContrast)
            ?: trestleColorScheme(accentColor, darkTheme, highContrast)
    }
    CompositionLocalProvider(
        LocalReduceMotion provides reducedMotion,
        LocalTrestleWindowAdaptiveInfo provides windowAdaptiveInfo,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            motionScheme = if (reducedMotion) ReducedMotionScheme else MotionScheme.standard(),
            typography = trestleTypography(),
            shapes = TrestleShapes,
            content = content,
        )
    }
}

internal val LocalReduceMotion = staticCompositionLocalOf { false }
