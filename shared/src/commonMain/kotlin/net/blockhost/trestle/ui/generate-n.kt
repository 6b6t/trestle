package net.blockhost.trestle.ui

fun generateN(count: Int): List<Int> = List(count.coerceAtLeast(0)) { index -> index + 1 }
