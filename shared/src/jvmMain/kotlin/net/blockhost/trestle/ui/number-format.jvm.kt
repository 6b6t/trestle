package net.blockhost.trestle.ui

import java.text.NumberFormat
import java.util.Locale

internal actual fun formatDecimal(value: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 }.format(value)
