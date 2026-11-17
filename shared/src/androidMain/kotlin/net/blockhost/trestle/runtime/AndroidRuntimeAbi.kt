package net.blockhost.trestle.runtime

import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.metadata.Architecture
import net.blockhost.trestle.resources.Res
import net.blockhost.trestle.resources.msg_android_game_launch_requires_arm64_or_x64
import net.blockhost.trestle.resources.msg_the_s_library_does_not_match_the
import net.blockhost.trestle.resources.msg_the_s_native_library_is_incomplete
import net.blockhost.trestle.text.text
import okio.Path
import java.io.DataInputStream
import java.io.EOFException
import java.io.FileInputStream

internal enum class AndroidRuntimeAbi(
    val architecture: Architecture,
    val directoryName: String,
    val releaseName: String,
    private val elfMachine: Int,
) {
    ARM64(Architecture.ARM64, "arm64-v8a", "arm64", 183),
    X64(Architecture.X86_64, "x86_64", "x64", 62),
    ;

    fun verifyLibrary(path: Path) {
        val header = ByteArray(20)
        try {
            DataInputStream(FileInputStream(path.toString())).use { it.readFully(header) }
        } catch (_: EOFException) {
            throw LauncherException.InvalidMetadata(text(Res.string.msg_the_s_native_library_is_incomplete, path.name))
        }
        val machine = (header[18].toInt() and 0xff) or ((header[19].toInt() and 0xff) shl 8)
        if (
            !header.copyOfRange(0, 7).contentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46, 2, 1, 1)) ||
            machine != elfMachine
        ) {
            throw LauncherException.InvalidMetadata(text(Res.string.msg_the_s_library_does_not_match_the, path.name, releaseName))
        }
    }

    companion object {
        fun forArchitecture(architecture: Architecture): AndroidRuntimeAbi = entries.firstOrNull { it.architecture == architecture }
            ?: throw LauncherException.RuntimeUnavailable(
                text(Res.string.msg_android_game_launch_requires_arm64_or_x64, architecture.name),
            )
    }
}
