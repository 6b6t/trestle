package net.blockhost.trestle.runtime

import net.blockhost.trestle.domain.LauncherException
import net.blockhost.trestle.metadata.Architecture
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
            throw LauncherException.InvalidMetadata("The ${path.name} native library is incomplete.")
        }
        val machine = (header[18].toInt() and 0xff) or ((header[19].toInt() and 0xff) shl 8)
        if (
            !header.copyOfRange(0, 7).contentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46, 2, 1, 1)) ||
            machine != elfMachine
        ) {
            throw LauncherException.InvalidMetadata("The ${path.name} library does not match the Android $releaseName runtime.")
        }
    }

    companion object {
        fun forArchitecture(architecture: Architecture): AndroidRuntimeAbi =
            entries.firstOrNull { it.architecture == architecture }
                ?: throw LauncherException.RuntimeUnavailable("Android game launch requires ARM64 or x64. This process uses ${architecture.name}.")
    }
}
