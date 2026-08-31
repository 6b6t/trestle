package net.blockhost.trestle.app

/** SemVer precedence, without build metadata. Invalid versions never trigger an update. */
internal data class ReleaseVersion(val core: List<ULong>, val prerelease: List<String>) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int {
        core.zip(other.core).forEach { (left, right) ->
            left.compareTo(right).takeIf { it != 0 }?.let { return it }
        }
        if (prerelease.isEmpty()) return if (other.prerelease.isEmpty()) 0 else 1
        if (other.prerelease.isEmpty()) return -1
        prerelease.zip(other.prerelease).forEach { (left, right) ->
            val leftNumber = left.toULongOrNull()
            val rightNumber = right.toULongOrNull()
            val result = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1
                rightNumber != null -> 1
                else -> left.compareTo(right)
            }
            if (result != 0) return result
        }
        return prerelease.size.compareTo(other.prerelease.size)
    }

    companion object {
        private val pattern = Regex("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$")

        fun parse(value: String): ReleaseVersion? {
            val match = pattern.matchEntire(value.removePrefix("v")) ?: return null
            val core = (1..3).map { match.groupValues[it].toULongOrNull() ?: return null }
            val pre = match.groupValues[4].takeIf(String::isNotEmpty)?.split('.') ?: emptyList()
            if (pre.any { part -> part.all(Char::isDigit) && (part.length > 1 && part.startsWith('0') || part.toULongOrNull() == null) }) return null
            return ReleaseVersion(core, pre)
        }
    }
}
