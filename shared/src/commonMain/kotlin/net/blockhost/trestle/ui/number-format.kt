package net.blockhost.trestle.ui

/** Formats [count] in short form for the launcher language, such as "12.4M" or "12,4 Mio.". */
internal expect fun formatCompactCount(count: Long): String

/** Formats [value] with at most one fraction digit and the decimal separator of the launcher language. */
internal expect fun formatDecimal(value: Double): String
