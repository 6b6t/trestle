package net.blockhost.trestle.ui

import java.text.DateFormat
import java.util.Date

internal actual fun formatLocalDateTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
