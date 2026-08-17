package net.blockhost.trestle.instance

enum class MinecraftNarratorMode(
    val optionValue: Int,
    val label: String,
) {
    OFF(0, "Off"),
    ALL(1, "Narrate all"),
    CHAT(2, "Narrate chat"),
    SYSTEM(3, "Narrate system"),
}

data class MinecraftClientSettings(
    val narratorMode: MinecraftNarratorMode = MinecraftNarratorMode.OFF,
    val masterVolumePercent: Int = 50,
    val musicVolumePercent: Int = 20,
    val renderDistanceChunks: Int = 12,
    val simulationDistanceChunks: Int = 8,
    val autoJump: Boolean = false,
    val showSubtitles: Boolean = false,
    val enableVsync: Boolean = true,
) {
    init {
        require(masterVolumePercent in 0..100) { "Master volume must be between 0 and 100." }
        require(musicVolumePercent in 0..100) { "Music volume must be between 0 and 100." }
        require(renderDistanceChunks in 2..32) { "Render distance must be between 2 and 32 chunks." }
        require(simulationDistanceChunks in 5..32) { "Simulation distance must be between 5 and 32 chunks." }
    }

    internal fun toOptionsText(minecraftVersionId: String): String {
        val support = MinecraftClientOptionsSupport.forVersion(minecraftVersionId)
        if (!support.audioAndVideo) return ""

        return buildList {
            add("enableVsync:$enableVsync")
            add("renderDistance:$renderDistanceChunks")
            add("soundCategory_master:${masterVolumePercent.toOptionVolume()}")
            add("soundCategory_music:${musicVolumePercent.toOptionVolume()}")
            if (support.subtitles) add("showSubtitles:$showSubtitles")
            if (support.autoJump) add("autoJump:$autoJump")
            if (support.narrator) add("narrator:${narratorMode.optionValue}")
            if (support.simulationDistance) add("simulationDistance:$simulationDistanceChunks")
        }.joinToString(separator = "\n", postfix = "\n")
    }
}

private data class MinecraftClientOptionsSupport(
    val audioAndVideo: Boolean,
    val subtitles: Boolean,
    val autoJump: Boolean,
    val narrator: Boolean,
    val simulationDistance: Boolean,
) {
    companion object {
        fun forVersion(versionId: String) = MinecraftClientOptionsSupport(
            audioAndVideo = versionId.isAtLeast(ReleaseVersion(1, 7, 2), SnapshotVersion(13, 36)),
            subtitles = versionId.isAtLeast(ReleaseVersion(1, 9), SnapshotVersion(15, 43)),
            autoJump = versionId.isAtLeast(ReleaseVersion(1, 10), SnapshotVersion(16, 20)),
            narrator = versionId.isAtLeast(ReleaseVersion(1, 12), SnapshotVersion(17, 6)),
            simulationDistance = versionId.isAtLeast(ReleaseVersion(1, 18), SnapshotVersion(21, 38)),
        )
    }
}

private data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0,
) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)
}

private data class SnapshotVersion(
    val year: Int,
    val week: Int,
) : Comparable<SnapshotVersion> {
    override fun compareTo(other: SnapshotVersion): Int =
        compareValuesBy(this, other, SnapshotVersion::year, SnapshotVersion::week)
}

private fun String.isAtLeast(release: ReleaseVersion, snapshot: SnapshotVersion): Boolean {
    val releaseMatch = RELEASE_VERSION.matchEntire(this)
    if (releaseMatch != null) {
        val candidate = ReleaseVersion(
            major = releaseMatch.groupValues[1].toInt(),
            minor = releaseMatch.groupValues[2].toInt(),
            patch = releaseMatch.groupValues[3].toIntOrNull() ?: 0,
        )
        return candidate >= release
    }

    val snapshotMatch = SNAPSHOT_VERSION.matchEntire(this) ?: return false
    val candidate = SnapshotVersion(
        year = snapshotMatch.groupValues[1].toInt(),
        week = snapshotMatch.groupValues[2].toInt(),
    )
    return candidate >= snapshot
}

private fun Int.toOptionVolume(): String = when (this) {
    0 -> "0.0"
    100 -> "1.0"
    else -> (this / 100.0).toString()
}

private val RELEASE_VERSION = Regex("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-_].*)?$")
private val SNAPSHOT_VERSION = Regex("^(\\d{2})w(\\d{2})[a-z].*$", RegexOption.IGNORE_CASE)
