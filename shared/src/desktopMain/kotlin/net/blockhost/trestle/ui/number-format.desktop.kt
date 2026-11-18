package net.blockhost.trestle.ui

import java.text.NumberFormat
import java.util.Locale

internal actual fun formatCompactCount(count: Long): String =
    NumberFormat.getCompactNumberInstance(Locale.getDefault(), NumberFormat.Style.SHORT)
        .apply { maximumFractionDigits = 1 }
        .format(count)
