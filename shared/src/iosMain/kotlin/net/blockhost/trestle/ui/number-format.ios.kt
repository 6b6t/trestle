package net.blockhost.trestle.ui

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale
import platform.Foundation.preferredLanguages

// Foundation has no compact number style for Objective-C callers, so iOS shows the full grouped number.
internal actual fun formatCompactCount(count: Long): String =
    decimalFormatter(0).stringFromNumber(NSNumber(long = count)) ?: count.toString()

internal actual fun formatDecimal(value: Double): String =
    decimalFormatter(1).stringFromNumber(NSNumber(double = value)) ?: value.toString()

private fun decimalFormatter(fractionDigits: Int) = NSNumberFormatter().apply {
    numberStyle = NSNumberFormatterDecimalStyle
    maximumFractionDigits = fractionDigits.toULong()
    locale = formattingLocale()
}

/**
 * The launcher language with Western digits. Trestle writes the launcher language to AppleLanguages, so the first
 * preferred language follows it right away, while NSLocale.currentLocale keeps the language from app start. Western
 * digits match the numbers that translated messages insert.
 */
internal fun formattingLocale(): NSLocale = (NSLocale.preferredLanguages.firstOrNull() as? String)
    ?.let { NSLocale(localeIdentifier = "$it@numbers=latn") }
    ?: NSLocale.currentLocale
