package net.blockhost.trestle.ui

import android.icu.text.CompactDecimalFormat
import java.util.Locale

internal actual fun formatCompactCount(count: Long): String =
    CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).format(count)
