package net.blockhost.trestle.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

/**
 * A one-line label for buttons that share a row. Long translations shrink to fit instead of wrapping
 * onto a second line.
 */
@Composable
internal fun ButtonLabel(text: String) {
    Text(
        text,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = LocalTextStyle.current.fontSize),
    )
}
