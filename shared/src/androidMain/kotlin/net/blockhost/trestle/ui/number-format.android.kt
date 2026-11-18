package net.blockhost.trestle.ui

import android.icu.text.CompactDecimalFormat

internal actual fun formatCompactCount(count: Long): String =
    CompactDecimalFormat.getInstance(formattingLocale(), CompactDecimalFormat.CompactStyle.SHORT).format(count)
