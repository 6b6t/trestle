package net.blockhost.trestle.ui

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970

// Same styles as desktop and Android: a medium date and a short time without seconds.
@OptIn(ExperimentalForeignApi::class)
internal actual fun formatLocalDateTime(epochMillis: Long): String {
    val formatter = NSDateFormatter().apply {
        locale = formattingLocale()
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1_000.0))
}
