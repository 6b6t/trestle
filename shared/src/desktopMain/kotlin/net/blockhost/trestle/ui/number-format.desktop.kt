package net.blockhost.trestle.ui

import java.text.NumberFormat

internal actual fun formatCompactCount(count: Long): String =
    NumberFormat.getCompactNumberInstance(formattingLocale(), NumberFormat.Style.SHORT)
        .apply { maximumFractionDigits = 1 }
        .format(count)
