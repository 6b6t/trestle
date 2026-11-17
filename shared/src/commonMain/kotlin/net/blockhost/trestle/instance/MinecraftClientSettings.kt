package net.blockhost.trestle.instance

import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_brightness_must_be_between_and
import net.blockhost.trestle.resources.msg_field_of_view_must_be_between_and
import net.blockhost.trestle.resources.msg_gui_scale_must_be_between_and
import net.blockhost.trestle.resources.msg_master_volume_must_be_between_and
import net.blockhost.trestle.resources.msg_maximum_frame_rate_must_be_between_and
import net.blockhost.trestle.resources.msg_mouse_sensitivity_must_be_between_and
import net.blockhost.trestle.resources.msg_music_volume_must_be_between_and
import net.blockhost.trestle.resources.msg_render_distance_must_be_between_and_chunks
import net.blockhost.trestle.resources.msg_simulation_distance_must_be_between_and_chunks
import net.blockhost.trestle.text.requireUser
import net.blockhost.trestle.text.text
import kotlin.math.roundToInt

enum class MinecraftNarratorMode(val optionValue: Int, val label: String) {
    OFF(0, "Off"),
    ALL(1, "Narrate all"),
    CHAT(2, "Narrate chat"),
    SYSTEM(3, "Narrate system"),
}

enum class MinecraftParticleSetting(val optionValue: Int, val label: String) {
    ALL(0, "All"),
    DECREASED(1, "Decreased"),
    MINIMAL(2, "Minimal"),
}

data class MinecraftClientSettings(
    val narratorMode: MinecraftNarratorMode = MinecraftNarratorMode.OFF,
    val masterVolumePercent: Int = 50,
    val musicVolumePercent: Int = 20,
    val fieldOfViewDegrees: Int = 70,
    val brightnessPercent: Int = 50,
    val mouseSensitivityPercent: Int = 50,
    val maximumFrameRate: Int = 120,
    val guiScale: Int = 0,
    val renderDistanceChunks: Int = 12,
    val simulationDistanceChunks: Int = 8,
    val particles: MinecraftParticleSetting = MinecraftParticleSetting.ALL,
    val autoJump: Boolean = false,
    val showSubtitles: Boolean = false,
    val enableVsync: Boolean = true,
    val fullscreen: Boolean = false,
    val viewBobbing: Boolean = true,
    val invertMouse: Boolean = false,
    val entityShadows: Boolean = true,
) {
    init {
        requireUser(masterVolumePercent in 0..100) { text(Res.string.msg_master_volume_must_be_between_and) }
        requireUser(musicVolumePercent in 0..100) { text(Res.string.msg_music_volume_must_be_between_and) }
        requireUser(fieldOfViewDegrees in 30..110) { text(Res.string.msg_field_of_view_must_be_between_and) }
        requireUser(brightnessPercent in 0..100) { text(Res.string.msg_brightness_must_be_between_and) }
        requireUser(mouseSensitivityPercent in 0..100) { text(Res.string.msg_mouse_sensitivity_must_be_between_and) }
        requireUser(maximumFrameRate in 10..260) { text(Res.string.msg_maximum_frame_rate_must_be_between_and) }
        requireUser(guiScale in 0..8) { text(Res.string.msg_gui_scale_must_be_between_and) }
        requireUser(renderDistanceChunks in 2..32) { text(Res.string.msg_render_distance_must_be_between_and_chunks) }
        requireUser(simulationDistanceChunks in 5..32) { text(Res.string.msg_simulation_distance_must_be_between_and_chunks) }
    }

    internal fun toOptionsText(minecraftVersionId: String): String {
        val entries = toOptionEntries(minecraftVersionId)
        return entries.entries.joinToString(separator = "\n", postfix = if (entries.isEmpty()) "" else "\n") { (key, value) ->
            "$key:$value"
        }
    }

    internal fun mergeIntoOptionsText(existing: String, minecraftVersionId: String): String {
        val entries = toOptionEntries(minecraftVersionId)
        if (entries.isEmpty()) return existing

        val writtenKeys = mutableSetOf<String>()
        val lines = buildList {
            existing.trimEnd('\r', '\n').takeIf(String::isNotEmpty)?.lines()?.forEach { line ->
                val key = line.optionKey()
                if (key in entries) {
                    if (writtenKeys.add(key)) add("$key:${entries.getValue(key)}")
                } else {
                    add(line)
                }
            }
            entries.forEach { (key, value) ->
                if (writtenKeys.add(key)) add("$key:$value")
            }
        }
        return lines.joinToString(separator = "\n", postfix = "\n")
    }

    private fun toOptionEntries(minecraftVersionId: String): Map<String, String> {
        val support = MinecraftClientOptionsSupport.forVersion(minecraftVersionId)
        if (!support.audioAndVideo) return emptyMap()

        return buildMap {
            put("fullscreen", fullscreen.toString())
            put("enableVsync", enableVsync.toString())
            put("maxFps", maximumFrameRate.toString())
            put("guiScale", guiScale.toString())
            put("renderDistance", renderDistanceChunks.toString())
            put("particles", particles.optionValue.toString())
            put("bobView", viewBobbing.toString())
            put("fov", fieldOfViewDegrees.toFovOption())
            put("gamma", brightnessPercent.toOptionFraction())
            put("mouseSensitivity", mouseSensitivityPercent.toOptionFraction())
            put("invertYMouse", invertMouse.toString())
            put("soundCategory_master", masterVolumePercent.toOptionFraction())
            put("soundCategory_music", musicVolumePercent.toOptionFraction())
            if (support.entityShadows) put("entityShadows", entityShadows.toString())
            if (support.subtitles) put("showSubtitles", showSubtitles.toString())
            if (support.autoJump) put("autoJump", autoJump.toString())
            if (support.narrator) put("narrator", narratorMode.optionValue.toString())
            if (support.simulationDistance) put("simulationDistance", simulationDistanceChunks.toString())
        }
    }

    companion object {
        internal fun fromOptionsText(text: String, minecraftVersionId: String): MinecraftClientSettings? {
            val support = MinecraftClientOptionsSupport.forVersion(minecraftVersionId)
            if (!support.audioAndVideo) return null

            val options = text.lineSequence().mapNotNull { line ->
                val separator = line.indexOf(':')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
            }.toMap()
            val defaults = MinecraftClientSettings()
            return defaults.copy(
                narratorMode = options["narrator"].toIntInRangeOrNull(0..3)?.let { value ->
                    MinecraftNarratorMode.entries.first { it.optionValue == value }
                } ?: defaults.narratorMode,
                masterVolumePercent = options["soundCategory_master"].toPercentOrNull()
                    ?: defaults.masterVolumePercent,
                musicVolumePercent = options["soundCategory_music"].toPercentOrNull()
                    ?: defaults.musicVolumePercent,
                fieldOfViewDegrees = options["fov"].toFovDegreesOrNull() ?: defaults.fieldOfViewDegrees,
                brightnessPercent = options["gamma"].toPercentOrNull() ?: defaults.brightnessPercent,
                mouseSensitivityPercent = options["mouseSensitivity"].toPercentOrNull()
                    ?: defaults.mouseSensitivityPercent,
                maximumFrameRate = options["maxFps"].toIntInRangeOrNull(10..260) ?: defaults.maximumFrameRate,
                guiScale = options["guiScale"].toIntInRangeOrNull(0..8) ?: defaults.guiScale,
                renderDistanceChunks = options["renderDistance"].toIntInRangeOrNull(2..32)
                    ?: defaults.renderDistanceChunks,
                simulationDistanceChunks = options["simulationDistance"].toIntInRangeOrNull(5..32)
                    ?: defaults.simulationDistanceChunks,
                particles = options["particles"].toIntInRangeOrNull(0..2)?.let { value ->
                    MinecraftParticleSetting.entries.first { it.optionValue == value }
                } ?: defaults.particles,
                autoJump = options["autoJump"]?.toBooleanStrictOrNull() ?: defaults.autoJump,
                showSubtitles = options["showSubtitles"]?.toBooleanStrictOrNull() ?: defaults.showSubtitles,
                enableVsync = options["enableVsync"]?.toBooleanStrictOrNull() ?: defaults.enableVsync,
                fullscreen = options["fullscreen"]?.toBooleanStrictOrNull() ?: defaults.fullscreen,
                viewBobbing = options["bobView"]?.toBooleanStrictOrNull() ?: defaults.viewBobbing,
                invertMouse = options["invertYMouse"]?.toBooleanStrictOrNull() ?: defaults.invertMouse,
                entityShadows = options["entityShadows"]?.toBooleanStrictOrNull() ?: defaults.entityShadows,
            )
        }
    }
}

private data class MinecraftClientOptionsSupport(
    val audioAndVideo: Boolean,
    val entityShadows: Boolean,
    val subtitles: Boolean,
    val autoJump: Boolean,
    val narrator: Boolean,
    val simulationDistance: Boolean,
) {
    companion object {
        fun forVersion(versionId: String) = MinecraftClientOptionsSupport(
            audioAndVideo = versionId.isAtLeast(ReleaseVersion(1, 7, 2), SnapshotVersion(13, 36)),
            entityShadows = versionId.isAtLeast(ReleaseVersion(1, 8), SnapshotVersion(14, 5)),
            subtitles = versionId.isAtLeast(ReleaseVersion(1, 9), SnapshotVersion(15, 43)),
            autoJump = versionId.isAtLeast(ReleaseVersion(1, 10), SnapshotVersion(16, 20)),
            narrator = versionId.isAtLeast(ReleaseVersion(1, 12), SnapshotVersion(17, 6)),
            simulationDistance = versionId.isAtLeast(ReleaseVersion(1, 18), SnapshotVersion(21, 38)),
        )
    }
}

private data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int = 0) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)
}

private data class SnapshotVersion(val year: Int, val week: Int) : Comparable<SnapshotVersion> {
    override fun compareTo(other: SnapshotVersion): Int = compareValuesBy(this, other, SnapshotVersion::year, SnapshotVersion::week)
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

private fun Int.toOptionFraction(): String = when (this) {
    0 -> "0.0"
    100 -> "1.0"
    else -> (this / 100.0).toString()
}

private fun Int.toFovOption(): String = ((this - 70) / 40.0).toString()

private fun String?.toIntInRangeOrNull(range: IntRange): Int? = this?.toIntOrNull()?.takeIf { it in range }

private fun String?.toPercentOrNull(): Int? = this?.toDoubleOrNull()?.takeIf { it in 0.0..1.0 }?.times(100)?.roundToInt()

private fun String?.toFovDegreesOrNull(): Int? = this?.toDoubleOrNull()?.times(40)?.plus(70)?.roundToInt()?.takeIf { it in 30..110 }

private fun String.optionKey(): String = substringBefore(':', missingDelimiterValue = "")

private val RELEASE_VERSION = Regex("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-_].*)?$")
private val SNAPSHOT_VERSION = Regex("^(\\d{2})w(\\d{2})[a-z].*$", RegexOption.IGNORE_CASE)
