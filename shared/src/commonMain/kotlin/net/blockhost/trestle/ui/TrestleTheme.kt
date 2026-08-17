package net.blockhost.trestle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.barlow_condensed_semibold
import net.blockhost.trestle.resources.barlow_medium
import net.blockhost.trestle.resources.barlow_regular
import org.jetbrains.compose.resources.Font

internal val Soot = Color(0xFF171715)
internal val Surface = Color(0xFF24231F)
internal val RaisedSurface = Color(0xFF2E2C27)
internal val Ochre = Color(0xFFBE8F45)
internal val Chalk = Color(0xFFE7E3D9)
internal val Muted = Color(0xFFA9A49A)
internal val Rule = Color(0xFF3A3833)
internal val ErrorSurface = Color(0xFF3A2520)
internal val ErrorText = Color(0xFFF0AA94)

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
internal fun TrestleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrestleColors,
        typography = trestleTypography(),
        content = content,
    )
}
