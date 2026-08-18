package net.blockhost.trestle.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.barlow_condensed_semibold
import net.blockhost.trestle.resources.barlow_medium
import net.blockhost.trestle.resources.barlow_regular
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

private val TrestleColors = darkColorScheme(
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
    background = Soot,
    onBackground = Chalk,
    surface = Surface,
    onSurface = Chalk,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = Muted,
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
)

internal fun trestleColorScheme(accentColor: Color? = null): ColorScheme {
    if (accentColor == null) return TrestleColors
    val primary = primaryAccent(accentColor)
    val primaryContainer = lerp(Surface, primary, 0.32f)
    return TrestleColors.copy(
        primary = primary,
        onPrimary = contentColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = contentColor(primaryContainer),
        tertiary = primary,
        onTertiary = contentColor(primary),
        inversePrimary = accessibleAccent(accentColor, Chalk, Soot),
    )
}

private fun primaryAccent(accentColor: Color): Color {
    var candidate = accentColor.copy(alpha = 1f)
    repeat(12) {
        val surfaceContrast = contrastRatio(candidate, Surface)
        val contentContrast = contrastRatio(contentColor(candidate), candidate)
        if (surfaceContrast >= MINIMUM_ACCENT_CONTRAST && contentContrast >= MINIMUM_CONTENT_CONTRAST) {
            return candidate
        }
        candidate = lerp(candidate, Chalk, 0.16f)
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

@Composable
private fun trestleTypography(): Typography {
    val display = FontFamily(
        Font(Res.font.barlow_condensed_semibold, FontWeight.SemiBold),
    )
    val body = FontFamily(
        Font(Res.font.barlow_regular, FontWeight.Normal),
        Font(Res.font.barlow_medium, FontWeight.Medium),
    )

    return Typography(
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 40.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        ),
    )
}

@Composable
internal fun TrestleTheme(accentColor: Color? = null, content: @Composable () -> Unit) {
    val colorScheme = remember(accentColor) { trestleColorScheme(accentColor) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = trestleTypography(),
        shapes = TrestleShapes,
        content = content,
    )
}
