package net.blockhost.trestle.ui

import java.text.NumberFormat
import java.util.Locale

internal actual fun formatDecimal(value: Double): String =
    NumberFormat.getNumberInstance(formattingLocale()).apply { maximumFractionDigits = 1 }.format(value)

/**
 * The launcher language with Western digits. Translated messages insert numbers with Western digits, so Arabic,
 * Persian and Urdu would otherwise mix two digit styles on one screen.
 */
internal fun formattingLocale(): Locale = Locale.Builder()
    .setLocale(Locale.getDefault())
    .setUnicodeLocaleKeyword("nu", "latn")
    .build()
