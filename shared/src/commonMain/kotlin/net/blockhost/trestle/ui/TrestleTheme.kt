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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.blockhost.trestle.app.ThemePreference
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.roboto_variable
import org.jetbrains.compose.resources.Font

private val Soot = Color(0xFF171715)
private val Surface = Color(0xFF24231F)
private val RaisedSurface = Color(0xFF2E2C27)
private val Ochre = Color(0xFFBE8F45)
private val Chalk = Color(0xFFE7E3D9)
private val Muted = Color(0xFFA9A49A)
private val Rule = Color(0xFF3A3833)
private val ErrorSurface = Color(0xFF3A2520)
private val ErrorText = Color(0xFFF0AA94)
private val PrimaryFixed = Color(0xFFF4D9A4)
private val PrimaryFixedDim = Color(0xFFE2BD75)
private val OnPrimaryFixed = Color(0xFF2B200D)
private val OnPrimaryFixedVariant = Color(0xFF55401F)
private val SecondaryFixed = Color(0xFFE7E1D6)
private val SecondaryFixedDim = Color(0xFFA9A49A)
private val OnSecondaryFixed = Color(0xFF211F1B)
private val OnSecondaryFixedVariant = Color(0xFF49453E)
private val TrestleDarkColors = darkColorScheme(
    primary = Ochre,
    onPrimary = Color(0xFF211B12),
    primaryContainer = Color(0xFF55401F),
    onPrimaryContainer = Color(0xFFF4D9A4),
    secondary = Muted,
    onSecondary = Soot,
    secondaryContainer = RaisedSurface,
    onSecondaryContainer = Chalk,
    tertiary = Ochre,
    onTertiary = Color(0xFF211B12),
    tertiaryContainer = Color(0xFF55401F),
    onTertiaryContainer = Color(0xFFF4D9A4),
    background = Soot,
    onBackground = Chalk,
    surface = Surface,
    onSurface = Chalk,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = Muted,
    surfaceTint = Ochre,
    surfaceBright = Color(0xFF35332D),
    surfaceDim = Soot,
    surfaceContainerLowest = Soot,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = RaisedSurface,
    surfaceContainerHighest = Color(0xFF35332D),
    outline = Rule,
    outlineVariant = Rule,
    error = Color(0xFFE89982),
    onError = Color(0xFF2B0B04),
    errorContainer = ErrorSurface,
    onErrorContainer = ErrorText,
    inverseSurface = Chalk,
    inverseOnSurface = Soot,
    inversePrimary = Color(0xFF765A2D),
    scrim = Color.Black,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = PrimaryFixed,
    tertiaryFixedDim = PrimaryFixedDim,
    onTertiaryFixed = OnPrimaryFixed,
    onTertiaryFixedVariant = OnPrimaryFixedVariant,
)

private val TrestleLightColors = lightColorScheme(
    primary = Color(0xFF765A2D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4D9A4),
    onPrimaryContainer = Color(0xFF2B200D),
    secondary = Color(0xFF655F55),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E1D6),
    onSecondaryContainer = Color(0xFF211F1B),
    tertiary = Color(0xFF765A2D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4D9A4),
    onTertiaryContainer = Color(0xFF2B200D),
    background = Color(0xFFF8F5ED),
    onBackground = Color(0xFF24221E),
    surface = Color(0xFFFFFCF5),
    onSurface = Color(0xFF24221E),
    surfaceVariant = Color(0xFFEDE8DE),
    onSurfaceVariant = Color(0xFF625E56),
    surfaceTint = Color(0xFF765A2D),
    surfaceBright = Color(0xFFFFFCF5),
    surfaceDim = Color(0xFFE4E0D7),
    surfaceContainerLowest = Color(0xFFFFFCF5),
    surfaceContainerLow = Color(0xFFF5F1E8),
    surfaceContainer = Color(0xFFF0ECE3),
    surfaceContainerHigh = Color(0xFFEAE6DD),
    surfaceContainerHighest = Color(0xFFE4E0D7),
    outline = Color(0xFF7A756C),
    outlineVariant = Color(0xFFD0CBC1),
    error = Color(0xFF9E3D2A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD1),
    onErrorContainer = Color(0xFF3B0902),
    inverseSurface = Color(0xFF302F2B),
    inverseOnSurface = Color(0xFFF5F1E8),
    inversePrimary = Color(0xFFE2BD75),
    scrim = Color.Black,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = PrimaryFixed,
    tertiaryFixedDim = PrimaryFixedDim,
    onTertiaryFixed = OnPrimaryFixed,
    onTertiaryFixedVariant = OnPrimaryFixedVariant,
)

internal fun trestleColorScheme(
    accentColor: Color? = null,
    darkTheme: Boolean = true,
    highContrast: Boolean = false,
): ColorScheme {
    val base = if (darkTheme) TrestleDarkColors else TrestleLightColors
    val contrastedBase = base.withHighContrast(highContrast)
    if (accentColor == null) return contrastedBase
    val primary = primaryAccent(accentColor, contrastedBase.surface, if (darkTheme) Chalk else Soot)
    val primaryContainer = lerp(contrastedBase.surface, primary, if (darkTheme) 0.32f else 0.18f)
    val fixedDim = primaryAccent(accentColor, Soot, Chalk)
    val fixed = lerp(Chalk, fixedDim, 0.24f)
    return contrastedBase.copy(
        primary = primary,
        onPrimary = contentColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contentColor(primaryContainer),
        tertiary = primary,
        onTertiary = contentColor(primary),
        tertiaryContainer = primaryContainer,
        onTertiaryContainer = contentColor(primaryContainer),
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
        tertiaryFixed = fixed,
        tertiaryFixedDim = fixedDim,
        onTertiaryFixed = contentColor(fixed),
        onTertiaryFixedVariant = contentColor(fixedDim),
    )
}

internal fun ColorScheme.withHighContrast(enabled: Boolean): ColorScheme =
    if (enabled) {
        copy(
            outline = onSurface,
            outlineVariant = lerp(surface, onSurface, 0.62f),
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

private fun contentColor(background: Color): Color =
    if (contrastRatio(Soot, background) >= contrastRatio(Chalk, background)) Soot else Chalk

internal fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private const val MINIMUM_ACCENT_CONTRAST = 3f
private const val MINIMUM_CONTENT_CONTRAST = 4.5f

private val TrestleShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
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

    return Typography(fontFamily = roboto)
}

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
