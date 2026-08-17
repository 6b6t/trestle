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

private val TrestleColors = darkColorScheme(
    primary = Ochre,
    onPrimary = Color(0xFF211B12),
    primaryContainer = Color(0xFF55401F),
    onPrimaryContainer = Color(0xFFF4D9A4),
    background = Soot,
    onBackground = Chalk,
    surface = Surface,
    onSurface = Chalk,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = Muted,
    outline = Rule,
    error = Color(0xFFE89982),
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
